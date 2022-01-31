package persistence;

import exceptions.CatnapException;
import models.MockModel;
import models.MockModelWithOneToMany;
import models.MockModelWithOneToOne;
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
    private PreparedStatement statement2;

    @Mock
    private ResultSet resultSet;

    @Mock
    private ResultSet resultSet2;

    @Mock
    private ResultSetMetaData metaData;

    @Mock
    private ResultSetMetaData metaData2;

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
        when(resultSet.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnCount()).thenReturn(2);
        when(metaData.getColumnName(0)).thenReturn(model.getClass().getDeclaredFields()[0].getName());
        when(metaData.getColumnName(1)).thenReturn(model.getClass().getDeclaredFields()[1].getName());
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
        when(resultSet.getMetaData()).thenReturn(metaData, metaData, metaData);
        when(metaData.getColumnCount()).thenReturn(2, 2, 2);
        when(metaData.getColumnName(0)).thenReturn(
                model1.getClass().getDeclaredFields()[0].getName(),
                model2.getClass().getDeclaredFields()[0].getName(),
                model3.getClass().getDeclaredFields()[0].getName()
        );
        when(metaData.getColumnName(1)).thenReturn(
                model1.getClass().getDeclaredFields()[1].getName(),
                model2.getClass().getDeclaredFields()[1].getName(),
                model3.getClass().getDeclaredFields()[1].getName()
        );
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

    @Test
    public void testGetWithOneToOne() throws SQLException, CatnapException {
        // We have a relationship where model1 "owns" model2 but not model 3. This means that
        // the MockModel table will have the foreign key
        MockModelWithOneToOne model1 = new MockModelWithOneToOne(1, "model1");
        MockModel model2 = new MockModel(1, "model2");
        MockModel model3 = new MockModel(2, "model3");

        // we want to get model1 and have the relationship resolve
        // first we define model1 dependencies
        when(this.mappingStrategy.get(model1.getClass(), model1.getId())).thenReturn("model1");
        when(this.connection.prepareStatement("model1")).thenReturn(this.statement);
        when(this.statement.executeQuery()).thenReturn(this.resultSet);
        when(this.resultSet.next()).thenReturn(true, false);
        when(this.resultSet.getMetaData()).thenReturn(this.metaData);
        when(metaData.getColumnCount()).thenReturn(2);
        when(metaData.getColumnName(0)).thenReturn(model1.getClass().getDeclaredFields()[0].getName());
        when(metaData.getColumnName(1)).thenReturn(model1.getClass().getDeclaredFields()[1].getName());
        when(resultSet.getObject("id")).thenReturn(model1.getId());
        when(resultSet.getObject("name")).thenReturn(model1.getName());

        // now we define model2 and model3 dependencies
        when(this.mappingStrategy.getAll(model2.getClass())).thenReturn("model2");
        when(this.connection.prepareStatement("model2")).thenReturn(this.statement2);
        when(this.statement2.executeQuery()).thenReturn(this.resultSet2);
        when(this.resultSet2.next()).thenReturn(true, true, false);
        when(this.resultSet2.getMetaData()).thenReturn(metaData2);
        when(metaData2.getColumnCount()).thenReturn(3, 3);
        when(metaData2.getColumnName(0)).thenReturn(
                model2.getClass().getDeclaredFields()[0].getName(),
                model3.getClass().getDeclaredFields()[0].getName()
        );
        when(metaData2.getColumnName(1)).thenReturn(
                model2.getClass().getDeclaredFields()[1].getName(),
                model3.getClass().getDeclaredFields()[1].getName()
        );
        when(metaData2.getColumnName(2)).thenReturn("mock_id", "mock_id");
        when(resultSet2.getObject("id")).thenReturn(model2.getId(), model3.getId());
        when(resultSet2.getObject("name")).thenReturn(model2.getName(), model3.getName());
        when(resultSet2.getInt("mock_id")).thenReturn(4, 1);

        Optional<Object> entity = this.session.get(model1.getClass(), model1.getId());

        assertTrue(entity.isPresent());
        MockModelWithOneToOne gotModel = (MockModelWithOneToOne) entity.get();

        assertEquals(gotModel.getId(), model1.getId());
        assertEquals(gotModel.getName(), model1.getName());
        assertEquals(gotModel.getRelation().getId(), model3.getId());
        assertEquals(gotModel.getRelation().getName(), model3.getName());
    }

    @Test
    public void testGetOneToMany() throws SQLException, CatnapException {
        // We have a relationship where model1 "owns" model2 but not model 3. This means that
        // the MockModel table will have the foreign key
        MockModelWithOneToMany model1 = new MockModelWithOneToMany(1, "model1");
        MockModel model2 = new MockModel(1, "model2");
        MockModel model3 = new MockModel(2, "model3");
        MockModel model4 = new MockModel(3, "model4");

        // we want to get model1 and have the relationship resolve
        // first we define model1 dependencies
        when(this.mappingStrategy.get(model1.getClass(), model1.getId())).thenReturn("model1");
        when(this.connection.prepareStatement("model1")).thenReturn(this.statement);
        when(this.statement.executeQuery()).thenReturn(this.resultSet);
        when(this.resultSet.next()).thenReturn(true, false);
        when(this.resultSet.getMetaData()).thenReturn(this.metaData);
        when(metaData.getColumnCount()).thenReturn(2);
        when(metaData.getColumnName(0)).thenReturn(model1.getClass().getDeclaredFields()[0].getName());
        when(metaData.getColumnName(1)).thenReturn(model1.getClass().getDeclaredFields()[1].getName());
        when(resultSet.getObject("id")).thenReturn(model1.getId());
        when(resultSet.getObject("name")).thenReturn(model1.getName());

        // now we define model2 and model3 dependencies
        when(this.mappingStrategy.getAll(model2.getClass())).thenReturn("model2");
        when(this.connection.prepareStatement("model2")).thenReturn(this.statement2);
        when(this.statement2.executeQuery()).thenReturn(this.resultSet2);
        when(this.resultSet2.next()).thenReturn(true, true, true, false);
        when(this.resultSet2.getMetaData()).thenReturn(metaData2);
        when(metaData2.getColumnCount()).thenReturn(4, 4, 4);
        when(metaData2.getColumnName(0)).thenReturn(
                model2.getClass().getDeclaredFields()[0].getName(),
                model3.getClass().getDeclaredFields()[0].getName(),
                model4.getClass().getDeclaredFields()[0].getName()
        );
        when(metaData2.getColumnName(1)).thenReturn(
                model2.getClass().getDeclaredFields()[1].getName(),
                model3.getClass().getDeclaredFields()[1].getName(),
                model4.getClass().getDeclaredFields()[1].getName()
        );
        when(metaData2.getColumnName(2)).thenReturn("mock_id", "mock_id", "mock_id");
        when(resultSet2.getObject("id")).thenReturn(model2.getId(), model3.getId(), model4.getId());
        when(resultSet2.getObject("name")).thenReturn(model2.getName(), model3.getName(), model4.getName());
        when(resultSet2.getInt("mock_id")).thenReturn(4, 1, 1);

        Optional<Object> entity = this.session.get(model1.getClass(), model1.getId());

        assertTrue(entity.isPresent());

        MockModelWithOneToMany gotModel = (MockModelWithOneToMany) entity.get();

        assertEquals(gotModel.getFollowers().size(), 2);

        assertEquals(gotModel.getFollowers().get(0).getId(), model3.getId());
        assertEquals(gotModel.getFollowers().get(0).getName(), model3.getName());
        assertEquals(gotModel.getFollowers().get(1).getId(), model4.getId());
        assertEquals(gotModel.getFollowers().get(1).getName(), model4.getName());
    }
}
