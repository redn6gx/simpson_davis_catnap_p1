package persistence;

import exceptions.CatnapException;
import models.MockModel;
import models.MockModelWithRelationship;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import util.Cache;
import util.MappingStrategy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
public class SessionTest {

    @InjectMocks
    private Session session;

    @Mock
    private Connection connection;

    @Mock
    private MappingStrategy mappingStrategy;

    @Mock
    private Cache cache;

    @Mock
    private PreparedStatement statement;

    @Mock
    private ResultSet resultSet;

    @BeforeEach
    public void checkDependencies() {
        assertNotNull(connection);
        assertNotNull(mappingStrategy);
        assertNotNull(cache);
    }

    @Test
    public void testGetSuccess() throws SQLException, CatnapException {
        MockModel model = new MockModel(1, "mock");

        when(mappingStrategy.get(model.getClass(), model.getId())).thenReturn("");
        when(connection.prepareStatement("")).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getObject("id")).thenReturn(model.getId());
        when(resultSet.getObject("name")).thenReturn(model.getName());

        Optional<Object> op;
        op = session.get(model.getClass(), model.getId());

        verify(cache, times(1)).contains(model.getClass(), model.getId());
        verify(cache, times(1)).store(Mockito.any(model.getClass()));

        assertTrue(op.isPresent());
        MockModel gotModel = (MockModel) op.get();

        assertEquals(gotModel.getId(), model.getId());
        assertEquals(gotModel.getName(), model.getName());
    }

    @Test
    public void testGetFromCacheSuccess() throws CatnapException {
        MockModel model = new MockModel(1, "mock");

        when(cache.contains(model.getClass(), model.getId())).thenReturn(true);
        when(cache.get(model.getClass(), model.getId())).thenReturn(Optional.of(model));

        Optional<Object> gotModelOp = session.get(model.getClass(), model.getId());
        MockModel gotModel = (MockModel) gotModelOp.orElse(null);

        assertEquals(gotModel.getId(), model.getId());
        assertEquals(gotModel.getName(), model.getName());
    }

    @Test
    public void testGetSQLException() throws SQLException {
        MockModel model = new MockModel(1, "mock");

        when(mappingStrategy.get(model.getClass(), model.getId())).thenReturn("");
        when(connection.prepareStatement("")).thenReturn(statement);
        when(statement.executeQuery()).thenThrow(SQLException.class);

        assertThrows(CatnapException.class, () -> session.get(model.getClass(), model.getId()));
    }
}
