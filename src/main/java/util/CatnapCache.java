package util;

import exceptions.CatnapException;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * An implementation of the Cache interface that uses CatnapResult as the elements.
 * It uses a two level HashMap for splitting up entities by their type and id (so by their table and id).
 */
public class CatnapCache implements Cache<CatnapResult> {

    private final Map<Class<?>, Map<Integer, CatnapResult>> entityMap;

    public CatnapCache() {
        this.entityMap = new HashMap<>();
    }

    /**
     * This method checks whether an entity has been cached.
     *
     * @param clazz             the type of entity
     * @param id                the id of the entity
     * @return                  whether the entity is in the cache
     */
    @Override
    public boolean contains(Class<?> clazz, int id) {
        return this.entityMap.containsKey(clazz) && this.entityMap.get(clazz).containsKey(id);
    }

    /**
     * This method is used to get a specific entity from the cache.
     *
     * @param clazz             the type of entity to get
     * @param id                the primary key of the entity
     * @return                  an optional which is empty when the entity is not found in the cache
     */
    @Override
    public Optional<CatnapResult> get(Class<?> clazz, int id) {
        if(this.entityMap.containsKey(clazz)) {
            Map<Integer, CatnapResult> entityMapLevel2 = this.entityMap.get(clazz);
            if(entityMapLevel2.containsKey(id)) {
                return Optional.of(entityMapLevel2.get(id));
            }
        }

        return Optional.empty();
    }

    /**
     * This method is used to store an entity in the cache.
     *
     * @param entity            the entity to store
     * @throws CatnapException  thrown when the entity is missing an id
     */
    @Override
    public void store(CatnapResult entity) throws CatnapException {
        Class<?> clazz = entity.getEntityType();
        int id = entity.getId().orElseThrow(() -> new CatnapException("The id field of entity type: " + clazz.getName() +  "was empty!"));

        if(!this.entityMap.containsKey(clazz)) {
            this.entityMap.put(clazz, new HashMap<>());
        }

        this.entityMap.get(clazz).put(id, entity);
    }

    /**
     * This method is used to store a collection of entities. It iterates through the collection
     * and calls store on each element.
     *
     * @param entities            a collection of entities to store
     * @throws CatnapException    thrown when the entity is missing an id
     */
    @Override
    public void store(Collection<CatnapResult> entities) throws CatnapException {
        for (CatnapResult entity: entities) {
            store(entity);
        }
    }

    /**
     * This method is used to remove an entity from the cache.
     *
     * @param entity               the entity to remove
     * @throws CatnapException     thrown when the entity is missing an id
     */
    @Override
    public void remove(CatnapResult entity) throws CatnapException {
        if(this.entityMap.containsKey(entity.getEntityType())) {
            int id = entity.getId().orElseThrow(() -> {
                try {
                    return new CatnapException("The id field of entity type: " + entity.getEntityType().getName() +  "was empty!");
                } catch (CatnapException e) {
                    return e;
                }
            });
            this.entityMap.get(entity.getEntityType()).remove(id);
        }
    }
}
