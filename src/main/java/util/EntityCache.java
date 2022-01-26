package util;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * This class is used by the Session class to cache entities that have
 * been queried in the database.
 *
 * @param <T> the entity type being store in this cache
 */
public class EntityCache<T> implements Cache<T> {

    private final Map<Integer, T> map;

    public EntityCache() {
        this.map = new HashMap<>();
    }

    /**
     * This method returns an optional of an entity of type T. If the
     * entity is not in the cache the isPresent method of the optional
     * will return false. The underlying data structure that manages
     * the data is a HashMap.
     *
     * @param id database id of the entity
     * @return   the java object representation of the entity
     */
    @Override
    public Optional<T> get(int id) {
        if(this.map.containsKey(id)) {
            return Optional.of(this.map.get(id));
        } else {
            return Optional.empty();
        }
    }

    /**
     * This method caches the object for later retrieval. The underlying
     * data structure that manages the data is a HashMap. If the object
     * being cached is already in the cache we assume the one being added
     * is the newest version and override the one in the HashMap.
     *
     * @param id the id of the entity
     * @param t  the entity to store in the cache
     */
    @Override
    public void store(int id, T t) {
        this.map.put(id, t);
    }

    /**
     * This methods is used to check if an entity is in the cache.
     * Uses the entity's database id to do so. The underlying data
     * structure is a HashMap so this method checks whether the
     * key is in the map.
     *
     * @param id the id of the entity
     * @return   whether the entity has been cached
     */
    @Override
    public boolean contains(int id) {
        return this.map.containsKey(id);
    }
}
