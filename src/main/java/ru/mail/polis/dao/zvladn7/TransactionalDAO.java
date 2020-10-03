package ru.mail.polis.dao.zvladn7;

import ru.mail.polis.dao.DAO;

public interface TransactionalDAO extends DAO {

    void commit();

    void rollback();

}
