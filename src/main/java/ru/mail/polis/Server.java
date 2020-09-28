
package ru.mail.polis;

import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.DAOFactory;
import ru.mail.polis.service.Service;
import ru.mail.polis.service.ServiceFactory;

import java.io.File;
import java.io.IOException;

/**
 * Starts storage and waits for shutdown.
 *
 * @author Vadim Tsesko
 */
public final class Server {
    private static final int PORT = 8080;

    private Server() {
        // Not instantiable
    }

    public static void main(String[] args) throws IOException {
        // Temporary storage in the file system
        final File data;
        if (args.length > 0) {
            data = new File(args[0]);
        } else {
            data = Files.createTempDirectory();
        }
        System.out.println("Storing data at " + data);

        // Start the storage
        final DAO dao = DAOFactory.create(data);
        final Service storage =
                ServiceFactory.create(
                        PORT,
                        dao);
        storage.start();
        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> {
                    storage.stop();
                    try {
                        dao.close();
                    } catch (IOException e) {
                        throw new RuntimeException("Can't close dao", e);
                    }
                }));
    }
}