package util;

import mocks.EntityMock;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class EntityCacheTest {

    private static EntityMock entityMock;

    @BeforeAll
    public static void init() {
        entityMock = new EntityMock(1, "mock");
    }

    @Test
    public void testStoreAndGet() {
        EntityCache<EntityMock> cache = new EntityCache<>();

        assertDoesNotThrow(() -> cache.store(entityMock.getId(), entityMock));
        Optional<EntityMock> o = cache.get(1);

        assertTrue(o.isPresent());

        assertEquals(o.get().getId(), 1);
        assertEquals(o.get().getName(), "mock");
    }

    @Test
    public void testDoesNotContain() {
        EntityCache<EntityMock> cache = new EntityCache<>();

        assertDoesNotThrow(() -> cache.store(entityMock.getId(), entityMock));

        EntityMock mock2 = new EntityMock(2, "mock2");

        assertFalse(cache.contains(mock2.getId()));
    }

    @Test
    public void testDoesContain()  {
        EntityCache<EntityMock> cache = new EntityCache<>();

        assertDoesNotThrow(() -> cache.store(entityMock.getId(), entityMock));

        EntityMock mock2 = new EntityMock(2, "mock2");
        cache.store(mock2.getId(), mock2);

        assertTrue(cache.contains(mock2.getId()));
    }
}
