package persistence;

import exceptions.CatnapException;
import models.MockModel;
import models.MockModelWithOneToOne;
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
import java.util.List;
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

    @Mock
    private ResultSet resultSet2;

    @BeforeEach
    public void checkDependencies() {
        assertNotNull(connection);
        assertNotNull(mappingStrategy);
        assertNotNull(cache);
    }

    @Test
    public void testGet() throws SQLException, CatnapException {
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
    public void testGetFromCache() throws CatnapException {
        MockModel model = new MockModel(1, "mock");

        when(cache.contains(model.getClass(), model.getId())).thenReturn(true);
        when(cache.get(model.getClass(), model.getId())).thenReturn(Optional.of(model));

        Optional<Object> gotModelOp = session.get(model.getClass(), model.getId());
        MockModel gotModel = (MockModel) gotModelOp.orElse(new MockModel(2, "mock2"));

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

    @Test
    public void testGetAll() throws SQLException, CatnapException {
        MockModel model1 = new MockModel(1, "mock1");
        MockModel model2 = new MockModel(2, "mock2");
        MockModel model3 = new MockModel(3, "mock3");

        when(this.mappingStrategy.getAll(model1.getClass())).thenReturn("");
        when(connection.prepareStatement("")).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, true, true, false);
        when(resultSet.getObject("id")).thenReturn(model1.getId(), model2.getId(), model3.getId());
        when(resultSet.getObject("name")).thenReturn(model1.getName(), model2.getName(), model3.getName());

        List<Object> entities = session.getAll(model1.getClass());
        verify(cache, times(3)).store(Mockito.any(model1.getClass()));

        assertEquals(entities.size(), 3);

        assertEquals(((MockModel) entities.get(0)).getId(), model1.getId());
        assertEquals(((MockModel) entities.get(1)).getId(), model2.getId());
        assertEquals(((MockModel) entities.get(2)).getId(), model3.getId());

        assertEquals(((MockModel) entities.get(0)).getName(), model1.getName());
        assertEquals(((MockModel) entities.get(1)).getName(), model2.getName());
        assertEquals(((MockModel) entities.get(2)).getName(), model3.getName());
    }

    @Test
    public void testGetNone() throws SQLException, CatnapException {
        MockModel model = new MockModel(1, "mock");

        when(this.mappingStrategy.getAll(model.getClass())).thenReturn("");
        when(connection.prepareStatement("")).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        List<Object> entities = session.getAll(model.getClass());

        assertTrue(entities.isEmpty());
    }

    @Test
    public void testGetAllSQLException() throws SQLException {
        MockModel model = new MockModel(1, "mock");

        when(mappingStrategy.getAll(model.getClass())).thenReturn("");
        when(connection.prepareStatement("")).thenReturn(statement);
        when(statement.executeQuery()).thenThrow(SQLException.class);

        assertThrows(CatnapException.class, () -> session.getAll(model.getClass()));
    }
}
