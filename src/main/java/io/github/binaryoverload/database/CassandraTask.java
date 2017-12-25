package io.github.binaryoverload.database;

import com.datastax.driver.core.Session;

@FunctionalInterface
public interface CassandraTask {

    void execute(Session session);
}
