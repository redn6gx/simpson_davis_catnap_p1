package persistence;

import exceptions.ConnectionFailedException;
import util.ConnectionPool;
import util.CatnapCache;
import util.MappingStrategy;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is used to obtain Session objects during a persistence context. Typical usage involves
 * calling createEntityManager with a session id so that subsequent requests can make use of the same
 * Session class, and therefore, the cache will have entities in it. Recommend using a wrapper class
 * to manage the factory.
 */
public class SessionFactory implements EntityManagerFactory {

    private final ConnectionPool connectionPool;
    private final MappingStrategy mappingStrategy;
    private final Map<String, EntityManager> entityManagerMap;

    public SessionFactory(ConnectionPool connectionPool, MappingStrategy mappingStrategy) {
        this.connectionPool = connectionPool;
        this.mappingStrategy = mappingStrategy;
        this.entityManagerMap = new HashMap<>();
    }

    /**
     * This method is used to get a Session object that is not tied to a web session. Not recommended
     * as it will not utilize the cache.
     *
     * @return                             an EntityManager implementing Session object
     * @throws ConnectionFailedException   thrown when the ConnectionPool fails to give the session a Connection
     */
    @Override
    public EntityManager createEntityManager() throws ConnectionFailedException {
        return new Session(this.connectionPool.getConnection(), this.mappingStrategy, new CatnapCache(), this);
    }

    /**
     * This method is used to get a Session object that is tied to a web session. This is the preferred
     * way of getting a Session.
     *
     * @param id                           the id of the user
     * @return                             an EntityManager implementing Session object
     * @throws ConnectionFailedException   thrown when the ConnectionPool fails to give the session a Connection
     */
    @Override
    public EntityManager createEntityManager(String id) throws ConnectionFailedException {
        EntityManager em = createEntityManager();
        this.entityManagerMap.put(id, em);

        return em;
    }

    /**
     * This method is used to get a Session object back during a new request from a user who has already
     * started  a web session.
     *
     * @param id                            the id of the user session
     * @return                              an EntityManager implementing Session object
     * @throws ConnectionFailedException    thrown when the ConnectionPool fails to give the session a Connection
     */
    @Override
    public EntityManager getSessionContext(String id) throws ConnectionFailedException {
        if(this.entityManagerMap.containsKey(id)) {
            return this.entityManagerMap.get(id);
        } else {
            return createEntityManager(id);
        }
    }

    /**
     * This method is used to return a Connection object to the ConnectionPool when an EntityManager
     * is finished with it.
     *
     * @param connection                     the Connection to return to the pool
     */
    public void releaseConnection(Connection connection) {
        this.connectionPool.releaseConnection(connection);
    }
}
