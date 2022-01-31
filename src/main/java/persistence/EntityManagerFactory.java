package persistence;

import exceptions.ConnectionFailedException;

/**
 * Classes implementing this interface produce EntityManager objects for performing
 * CRUD operations on the database.
 */
public interface EntityManagerFactory {

    /**
     * This method returns an EntityManager object whose lifetime is expected to be for a single request.
     * @return                             an entitymanager object for a single request persistence context
     * @throws ConnectionFailedException   thrown when obtaining a connection for the EntityManager fails
     */
    public EntityManager createEntityManager() throws ConnectionFailedException;

    /**
     * This method returns an EntityManager object whose lifetime is expected
     * to be for a single session of a user.
     * @param id                           the id of the user
     * @return                             an EntityManger object for a user session persistence context
     * @throws ConnectionFailedException   thrown when obtaining a connection for the EntityManager fails
     */
    public EntityManager createEntityManager(String id) throws ConnectionFailedException;

    /**
     * This method retrieves the EntityManager for a user session.
     * @param id     the id of the user session
     */
    public EntityManager getSessionContext(String id);
}
