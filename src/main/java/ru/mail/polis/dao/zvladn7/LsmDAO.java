package ru.mail.polis.dao.zvladn7;

import ru.mail.polis.dao.DAO;

public interface LsmDAO extends DAO {

    /**
     * Begin new transaction.
     */
    TransactionalDAO beginTransaction();

}
