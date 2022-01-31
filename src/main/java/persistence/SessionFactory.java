package persistence;

import exceptions.ConnectionFailedException;
import util.ConnectionPool;
import util.MappingStrategy;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

public class SessionFactory implements EntityManagerFactory {

    private final ConnectionPool connectionPool;
    private final MappingStrategy mappingStrategy;
    private final Map<String, Session> sessionMap;

    private static SessionFactory instance = null;

    public static SessionFactory getInstance() {
        return instance;
    }

    public static SessionFactory build(ConnectionPool connectionPool, MappingStrategy mappingStrategy) {
        if(instance == null) {
            instance = new SessionFactory(connectionPool, mappingStrategy);
        }

        return instance;
    }

    private SessionFactory(ConnectionPool connectionPool, MappingStrategy mappingStrategy) {
        this.connectionPool = connectionPool;
        this.mappingStrategy = mappingStrategy;
        this.sessionMap = new HashMap<>();
    }

    @Override
    public EntityManager createEntityManager() throws ConnectionFailedException {
        return null;
    }

    @Override
    public EntityManager createEntityManager(String id) throws ConnectionFailedException {
        return null;
    }

    @Override
    public EntityManager getSessionContext(String id) {
        return null;
    }

    public void releaseConnection(Connection connection) {
        this.connectionPool.releaseConnection(connection);
    }
}
