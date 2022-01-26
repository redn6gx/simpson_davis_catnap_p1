package util;

import java.util.Optional;

public interface Cache<T> {

    /**
     * This method returns an optional of an entity of type T. If the
     * entity is not in the cache the isPresent method of the optional
     * will return false.
     *
     * @param id database id of the entity
     * @return   the java object representation of the entity
     */
    public Optional<T> get(int id);

    /**
     * This method caches the object for later retrieval.
     *
     * @param id the id of the entity
     * @param t  the entity to store in the cache
     */
    public void store(int id, T t);

    /**
     * This methods is used to check if an entity is in the cache.
     * Uses the entity's database id to do so.
     *
     * @param id the id of the entity
     * @return   whether the entity has been cached
     */
    public boolean contains(int id);
}
