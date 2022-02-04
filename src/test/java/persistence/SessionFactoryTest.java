package persistence;

import exceptions.ConnectionFailedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import util.ConnectionPool;
import util.MappingStrategy;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class SessionFactoryTest {

    @InjectMocks
    private SessionFactory factory;

    @Mock
    private ConnectionPool mockConnectionPool;

    @Mock
    private MappingStrategy mockMappingStrategy;

    @Test
    public void testCreateEntityManager() throws ConnectionFailedException {

        EntityManager session = factory.createEntityManager();
        assertTrue(session instanceof Session);
    }

    @Test
    public void testCreateEntityManagerWithSessionId() throws ConnectionFailedException {

        String id = "id";
        EntityManager session = factory.createEntityManager(id);

        assertTrue(session instanceof Session);
    }

    @Test
    public void testGetSessionContext() throws ConnectionFailedException {

        String id = "id";
        EntityManager session = factory.createEntityManager(id);
        EntityManager session2 = factory.getSessionContext(id);

        assertEquals(session, session2);
    }

    @Test
    public void testGetSessionContextBeforeExists() throws ConnectionFailedException {

        String id = "id";
        EntityManager session = factory.getSessionContext(id);
        EntityManager session2 = factory.getSessionContext(id);

        assertEquals(session, session2);
    }
}
