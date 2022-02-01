package persistence;

import exceptions.CatnapException;
import exceptions.ConnectionFailedException;
import util.ConnectionPool;
import util.L1Cache;
import util.MappingStrategy;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

public class SessionFactory implements EntityManagerFactory {

    private final ConnectionPool connectionPool;
    private final MappingStrategy mappingStrategy;
    private final Map<String, EntityManager> entityManagerMap;

    private static SessionFactory instance = null;

    public static SessionFactory getInstance() throws CatnapException {
        if(instance == null) {
            throw new CatnapException("You need to call build first to use the SessionFactory!");
        }

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
        this.entityManagerMap = new HashMap<>();
    }

    @Override
    public EntityManager createEntityManager() throws ConnectionFailedException {
        return new Session(this.connectionPool.getConnection(), this.mappingStrategy, new L1Cache());
    }

    @Override
    public EntityManager createEntityManager(String id) throws ConnectionFailedException {
        EntityManager em = createEntityManager();
        this.entityManagerMap.put(id, em);

        return em;
    }

    @Override
    public EntityManager getSessionContext(String id) throws ConnectionFailedException {
        if(this.entityManagerMap.containsKey(id)) {
            return this.entityManagerMap.get(id);
        } else {
            return createEntityManager(id);
        }
    }

    public void releaseConnection(Connection connection) {
        this.connectionPool.releaseConnection(connection);
    }
}
