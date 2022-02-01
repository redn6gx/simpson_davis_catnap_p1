package persistence;

import exceptions.CatnapException;
import exceptions.RollbackException;

import java.util.List;
import java.util.Optional;

/**
 * This interface represents the valid operations that can be performed on a database in Catnap.
 */
public interface EntityManager {

    /**
     * This method returns an Optional of an entity that was obtained from either the
     * l1 cache or the database. If isPresent is false then the entity does not exist
     * in the database.
     *
     * @param clazz              the type of entity to be returned
     * @param id                 the database identity of the entity to be returned
     * @return                   optional of the java object version of the returning entity
     * @throws CatnapException   thrown when a database operation fails, initialization of an entity fails,
     * or when trying to illegally access a member of an entity
     */
    public Optional<Object> get(Class<?> clazz, int id) throws CatnapException;

    /**
     * This method returns a list of all entities of the type specificed by clazz. The list will be empty
     * if no entities are found.
     *
     * @param clazz              the type of entity to get all records of
     * @return                   a list of entities found in the database
     * @throws CatnapException   thrown when a database operation fails, initialization of an entity fails,
     * or when trying to illegally access a member of an entity
     */
    public List<Object> getAll(Class<?> clazz) throws CatnapException;

    /**
     * This method deletes an entity from the database.
     *
     * @param entity             the entity to delete
     * @throws CatnapException   thrown when a database operation fails
     */
    public void delete(Object entity) throws CatnapException;

    /**
     * This method saves the entity to the database. The entity must not have existed
     * in the database before this call. To update entities use update.
     *
     * @param entity   the entity to add to the database
     * @throws CatnapException   thrown when a database operation fails
     */
    public void persist(Object entity) throws CatnapException;

    /**
     * This method updates the entity in the database. It also updates the entities
     * in the cache if they are there.
     *
     * @param entity   the entity to update with its new data
     * @throws CatnapException   thrown when a database operation fails
     */
    public void update(Object entity) throws CatnapException;

    /**
     * This method is used to begin a transaction. To add commands to a transaction call
     * session methods as normal after calling this method.
     * @throws CatnapException   thrown when the transaction creation fails
     */
    public void beginTransaction() throws CatnapException;

    /**
     * This method is used to tell the EntityManager you are finished adding commands to a transaction
     * and to run it on the database.
     *
     * @throws RollbackException   thrown when the transaction fails
     */
    public void commit() throws RollbackException;

    /**
     * This method should be called after a RollbackException occurs. It performs a rollback
     * on the transaction to ensure data integrity in the database.
     *
     * @throws CatnapException     thrown when the transaction fails
     */
    public void rollback() throws CatnapException;

    /**
     * This ends the session, releasing any connections to the database that this session had.
     *
     * @throws CatnapException     thrown when the SessionFactory hasn't been built yet
     */
    public void close() throws CatnapException;
}
