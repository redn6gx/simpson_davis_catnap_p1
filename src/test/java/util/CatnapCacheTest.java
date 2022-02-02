package util;

import exceptions.CatnapException;
import models.MockModel;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class CatnapCacheTest {

    @Test
    public void testStoreAndGetEntity() throws CatnapException {
        MockModel model = new MockModel(1, "mock");
        CatnapCache cache = new CatnapCache();

        cache.store(new CatnapResult(model));
        Optional<CatnapResult> e = cache.get(model.getClass(), model.getId());

        assertTrue(e.isPresent());

        assertEquals(e.get().getEntity(), model);
    }

    @Test
    public void testStoreMultipleAndGetEntity() throws CatnapException {
        MockModel model = new MockModel(1, "mock");
        MockModel model2 = new MockModel(2, "mock2");
        CatnapCache cache = new CatnapCache();

        List<CatnapResult> models = new ArrayList<>();
        models.add(new CatnapResult(model));
        models.add(new CatnapResult(model2));

        cache.store(models);

        Optional<CatnapResult> op1 = cache.get(model.getClass(), model.getId());
        Optional<CatnapResult> op2 = cache.get(model2.getClass(), model2.getId());
        MockModel got1 = (MockModel) op1.orElseThrow(CatnapException::new).getEntity();
        MockModel got2 = (MockModel) op2.orElseThrow(CatnapException::new).getEntity();

        assertEquals(got1.getId(), model.getId());
        assertEquals(got1.getName(), model.getName());
        assertEquals(got2.getId(), model2.getId());
        assertEquals(got2.getName(), model2.getName());
    }

    @Test
    public void testStoreException() {
        CatnapResult er = new CatnapResult(null);
        CatnapCache cache = new CatnapCache();

        assertThrows(CatnapException.class, () -> cache.store(er));
    }

    @Test
    public void testGetWhenNone() {
        CatnapCache cache = new CatnapCache();
        assertFalse(cache.get(MockModel.class, 1).isPresent());
    }

    @Test
    public void testDoesNotContain() {
        CatnapCache cache = new CatnapCache();

        assertFalse(cache.contains(MockModel.class, 1));
    }

    @Test
    public void testContains() throws CatnapException {
        MockModel model = new MockModel(1, "mock");
        CatnapCache cache = new CatnapCache();
        cache.store(new CatnapResult(model));

        assertTrue(cache.contains(model.getClass(), model.getId()));
    }

    @Test
    public void testRemove() throws CatnapException {
        MockModel model = new MockModel(1, "mock");
        CatnapCache cache = new CatnapCache();
        CatnapResult er = new CatnapResult(model);
        cache.store(er);
        cache.remove(er);

        assertFalse(cache.get(model.getClass(), model.getId()).isPresent());
    }

    @Test
    public void testRemoveException() {
        CatnapResult er = new CatnapResult(null);
        CatnapCache cache = new CatnapCache();

        assertThrows(CatnapException.class, () -> cache.store(er));
    }
}
