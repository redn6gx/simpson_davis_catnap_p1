package util;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public class CatnapCache implements Cache<CatnapResult> {

    private Map<Class<?>, Map<Integer, CatnapResult>> entityMap;

    @Override
    public boolean contains(Class<?> clazz, int id) {
        return false;
    }

    @Override
    public Optional<CatnapResult> get(Class<?> clazz, int id) {
        return Optional.empty();
    }

    @Override
    public void store(CatnapResult catnapResult) {

    }

    @Override
    public void store(Collection<CatnapResult> entities) {

    }

    @Override
    public void remove(CatnapResult entity) {

    }
}
