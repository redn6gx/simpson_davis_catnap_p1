package util;

import exceptions.CatnapException;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CatnapCache implements Cache<CatnapResult> {

    private final Map<Class<?>, Map<Integer, CatnapResult>> entityMap;

    public CatnapCache() {
        this.entityMap = new HashMap<>();
    }

    @Override
    public boolean contains(Class<?> clazz, int id) {
        return this.entityMap.containsKey(clazz) && this.entityMap.get(clazz).containsKey(id);
    }

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

    @Override
    public void store(CatnapResult entity) throws CatnapException {
        Class<?> clazz = entity.getEntityType();
        int id = entity.getId().orElseThrow(() -> new CatnapException("The id field of entity type: " + clazz.getName() +  "was empty!"));

        if(!this.entityMap.containsKey(clazz)) {
            this.entityMap.put(clazz, new HashMap<>());
        }

        this.entityMap.get(clazz).put(id, entity);
    }

    @Override
    public void store(Collection<CatnapResult> entities) throws CatnapException {
        for (CatnapResult entity: entities) {
            store(entity);
        }
    }

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
