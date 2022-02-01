package persistence;

import exceptions.CatnapException;
import exceptions.ConnectionFailedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import util.ConnectionPool;
import util.MappingStrategy;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SessionFactoryTest {

    public class MockMappingStrategy implements MappingStrategy {
        @Override
        public String get(Class clazz, int id) {
            return null;
        }

        @Override
        public String delete(Class clazz, int id) {
            return null;
        }

        @Override
        public String insert(Object entity) {
            return null;
        }

        @Override
        public String update(Object entity) {
            return null;
        }

        @Override
        public String getAll(Class clazz) {
            return null;
        }
    }

    public class MockConnectionPool implements ConnectionPool {
        @Override
        public void connect() throws ConnectionFailedException {

        }

        @Override
        public Connection getConnection() throws ConnectionFailedException {
            return null;
        }

        @Override
        public void releaseConnection(Connection connection) {

        }

        @Override
        public void shutdown() {

        }
    }

    private ConnectionPool mockConnectionPool;
    private MappingStrategy mockMappingStrategy;

    @BeforeEach
    public void testInit() {
        mockConnectionPool = new MockConnectionPool();
        mockMappingStrategy = new MockMappingStrategy();
        SessionFactory.build(mockConnectionPool, mockMappingStrategy);
    }

    @Test
    public void testCreateEntityManager() throws ConnectionFailedException, CatnapException {
        SessionFactory factory = SessionFactory.getInstance();

        EntityManager session = factory.createEntityManager();

        assertTrue(session instanceof Session);
    }

    @Test
    public void testCreateEntityManagerWithSessionId() throws CatnapException, ConnectionFailedException {
        String id = "id";
        SessionFactory factory = SessionFactory.getInstance();
        EntityManager session = factory.createEntityManager(id);

        assertTrue(session instanceof Session);
    }

    @Test
    public void testGetSessionContext() throws CatnapException, ConnectionFailedException {
        String id = "id";
        SessionFactory factory = SessionFactory.getInstance();
        EntityManager session = factory.createEntityManager(id);
        EntityManager session2 = factory.getSessionContext(id);

        assertEquals(session, session2);
    }

    @Test
    public void testGetSessionContextBeforeExists() throws CatnapException, ConnectionFailedException {
        String id = "id";
        SessionFactory factory = SessionFactory.getInstance();
        EntityManager session = factory.getSessionContext(id);
        EntityManager session2 = factory.getSessionContext(id);

        assertEquals(session, session2);
    }
}
