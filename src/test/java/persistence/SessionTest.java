package persistence;

import exceptions.CatnapException;
import models.MockModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import util.Cache;
import util.CatnapResult;
import util.MappingStrategy;

import java.sql.*;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

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
    public void testGet() throws SQLException, CatnapException {
        MockModel model = new MockModel(1, "mock");

        when(mappingStrategy.get(model.getClass(), model.getId())).thenReturn("");
        when(connection.prepareStatement("")).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getObject("id")).thenReturn(model.getId());
        when(resultSet.getObject("name")).thenReturn(model.getName());

        Optional<Object> op = session.get(model.getClass(), model.getId());

        verify(cache, times(1)).contains(model.getClass(), model.getId());
        verify(cache, times(1)).store(Mockito.any(CatnapResult.class));

        assertTrue(op.isPresent());
        MockModel gotModel = (MockModel) op.get();

        assertEquals(gotModel.getId(), model.getId());
        assertEquals(gotModel.getName(), model.getName());
    }

    @Test
    public void testGetFromCache() throws CatnapException {
        MockModel model = new MockModel(1, "mock");

        when(cache.contains(model.getClass(), model.getId())).thenReturn(true);
        when(cache.get(model.getClass(), model.getId())).thenReturn(Optional.of(new CatnapResult(model)));

        Optional<Object> gotModelOp = session.get(model.getClass(), model.getId());
        MockModel gotModel = (MockModel) gotModelOp.orElse(new MockModel(2, "mock2"));

        assertEquals(gotModel.getId(), model.getId());
        assertEquals(gotModel.getName(), model.getName());
    }

    @Test
    public void testGetNone() throws CatnapException, SQLException {
        MockModel model = new MockModel(1, "mock");

        when(mappingStrategy.get(model.getClass(), model.getId())).thenReturn("");
        when(connection.prepareStatement("")).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        Optional<Object> op = session.get(model.getClass(), model.getId());
        verify(cache, times(0)).store(Mockito.any(CatnapResult.class));

        assertFalse(op.isPresent());
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
    public void testGetResultSetSQLException() throws SQLException {
        MockModel model = new MockModel(1, "mock");

        when(mappingStrategy.get(model.getClass(), model.getId())).thenReturn("");
        when(connection.prepareStatement("")).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenThrow(SQLException.class);

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
        verify(cache, times(1)).store(Mockito.any(List.class));

        assertEquals(entities.size(), 3);

        assertEquals(((MockModel) entities.get(0)).getId(), model1.getId());
        assertEquals(((MockModel) entities.get(1)).getId(), model2.getId());
        assertEquals(((MockModel) entities.get(2)).getId(), model3.getId());

        assertEquals(((MockModel) entities.get(0)).getName(), model1.getName());
        assertEquals(((MockModel) entities.get(1)).getName(), model2.getName());
        assertEquals(((MockModel) entities.get(2)).getName(), model3.getName());
    }

    @Test
    public void testGetAllNone() throws SQLException, CatnapException {
        MockModel model = new MockModel(1, "mock");

        when(this.mappingStrategy.getAll(model.getClass())).thenReturn("");
        when(connection.prepareStatement("")).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        List<Object> entities = session.getAll(model.getClass());

        assertTrue(entities.isEmpty());
    }

    @Test
    public void testGetAllNoneSQLException() throws SQLException {
        MockModel model = new MockModel(1, "mock");

        when(this.mappingStrategy.getAll(model.getClass())).thenReturn("");
        when(connection.prepareStatement("")).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenThrow(SQLException.class);

        assertThrows(CatnapException.class, () -> {
            List<Object> entities = session.getAll(model.getClass());
        });
    }

    @Test
    public void testGetAllSQLException() throws SQLException {
        MockModel model = new MockModel(1, "mock");

        when(mappingStrategy.getAll(model.getClass())).thenReturn("");
        when(connection.prepareStatement("")).thenReturn(statement);
        when(statement.executeQuery()).thenThrow(SQLException.class);

        assertThrows(CatnapException.class, () -> session.getAll(model.getClass()));
    }

    @Test
    public void testDelete() throws SQLException, CatnapException {
        MockModel model = new MockModel(1, "mock");

        when(this.mappingStrategy.delete(model.getClass(), model.getId())).thenReturn("");
        when(this.connection.prepareStatement("")).thenReturn(statement);
        when(this.statement.executeUpdate()).thenReturn(1);

        this.session.delete(model);

        verify(this.cache, times(1)).remove(Mockito.any(CatnapResult.class));
    }

    @Test
    public void testDeleteSQLException() throws SQLException {
        MockModel model = new MockModel(1, "mock");

        when(this.mappingStrategy.delete(model.getClass(), model.getId())).thenReturn("");
        when(this.connection.prepareStatement("")).thenReturn(statement);
        when(this.statement.executeUpdate()).thenThrow(new SQLException());

        assertThrows(CatnapException.class, () -> this.session.delete(model));
    }

    @Test
    public void testPersist() throws SQLException, CatnapException {
        MockModel model = new MockModel(1, "mock");

        when(mappingStrategy.insert(model)).thenReturn("");
        when(connection.prepareStatement("")).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);

        assertDoesNotThrow(() -> {
            this.session.persist(model);
        });

        verify(cache, times(1)).store(Mockito.any(CatnapResult.class));
    }

    @Test
    public void testPersistSQLException() throws SQLException, CatnapException {
        MockModel model = new MockModel(1, "mock");

        when(mappingStrategy.insert(model)).thenReturn("");
        when(connection.prepareStatement("")).thenReturn(statement);
        when(statement.executeUpdate()).thenThrow(SQLException.class);

        assertThrows(CatnapException.class, () -> {
            this.session.persist(model);
        });

        verify(cache, times(0)).store(Mockito.any(CatnapResult.class));
    }

    @Test
    public void testUpdate() throws SQLException, CatnapException {
        MockModel model = new MockModel(1, "mock");

        when(mappingStrategy.update(model)).thenReturn("");
        when(connection.prepareStatement("")).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);

        assertDoesNotThrow(() -> {
            this.session.update(model);
        });

        verify(cache, times(1)).store(Mockito.any(CatnapResult.class));
    }

    @Test
    public void testUpdateSQLEXception() throws SQLException, CatnapException {
        MockModel model = new MockModel(1, "mock");

        when(mappingStrategy.update(model)).thenReturn("");
        when(connection.prepareStatement("")).thenReturn(statement);
        when(statement.executeUpdate()).thenThrow(SQLException.class);

        assertThrows(CatnapException.class, () -> {
            this.session.update(model);
        });

        verify(cache, times(0)).store(Mockito.any(CatnapResult.class));
    }
}
