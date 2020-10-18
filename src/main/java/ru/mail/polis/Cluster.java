/*
 * Copyright 2020 (c) Odnoklassniki
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.mail.polis;

import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.DAOFactory;
import ru.mail.polis.service.Service;
import ru.mail.polis.service.ServiceFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Starts 3-node storage cluster and waits for shutdown.
 *
 * @author Vadim Tsesko
 */
public final class Cluster {
    private static final int[] PORTS = {8080, 8081, 8082};

    private Cluster() {
        // Not instantiable
    }

    public static void main(String[] args) throws IOException {
        // Fill the topology
        final Set<String> topology = new HashSet<>(3);
        for (final int port : PORTS) {
            topology.add("http://localhost:" + port);
        }

        // Start nodes
        for (int i = 0; i < PORTS.length; i++) {
            final int port = PORTS[i];
            final File data = Files.createTempDirectory();
            final DAO dao = DAOFactory.create(data);

            System.out.println("Starting node " + i + " on port " + port + " and data at " + data);

            // Start the storage
            final Service storage =
                    ServiceFactory.create(
                            port,
                            dao,
                            topology);
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
}
