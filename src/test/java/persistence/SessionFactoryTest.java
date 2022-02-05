package persistence;

import exceptions.CatnapException;
import exceptions.ConnectionFailedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import util.ConnectionPool;
import util.MappingStrategy;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SessionFactoryTest {

    @InjectMocks
    private SessionFactory factory;

    @Mock
    private ConnectionPool mockConnectionPool;

    @Mock
    private MappingStrategy mockMappingStrategy;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement statement;

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

    @Test
    public void testBuild() throws SQLException, ConnectionFailedException, CatnapException {

        when(mockConnectionPool.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("")).thenReturn(statement);

        factory.build("");

        verify(statement, times(1)).executeUpdate();
    }

    @Test
    public void testBuildSQLException() throws SQLException, ConnectionFailedException {

        when(mockConnectionPool.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("")).thenReturn(statement);
        when(statement.executeUpdate()).thenThrow(SQLException.class);

        assertThrows(CatnapException.class, () -> factory.build(""));
    }
}
