package util;

import java.util.Collection;
import java.util.Optional;

public interface Cache<T> {

    /**
     * This method is used to check if an entity is in the cache.
     *
     * @param clazz             the type of entity
     * @param id                the id of the entity
     * @return                  whether the entity has been cached
     */
    public boolean contains(Class<?> clazz, int id);

    /**
     * This method is used to get an entity from the cache.
     *
     * @param clazz           the type of entity to get
     * @param id              the primary key of the entity
     * @return                an optional containing the entity if it's found
     */
    public Optional<T> get(Class<?> clazz, int id);

    /**
     * This method caches the object for later retrieval.
     *
     * @param t  the entity to store in the cache
     */
    public void store(T t);

    /**
     * This method stores a collection of entities into the cache.
     *
     * @param entities        a collection of entities to store
     */
    public void store(Collection<T> entities);

    /**
     * This method removes an entity from the cache.
     *
     * @param entity          the entity to remove
     */
    public void remove(CatnapResult entity);

}
