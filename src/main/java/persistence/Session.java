package persistence;

import exceptions.CatnapException;
import exceptions.RollbackException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.*;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * This class represents the persistence and is used to persist entities. It maintains a cache of
 * entities.
 *
 * Entities are cached in the database operating methods (defined in EntityManager). This class' methods
 * work in the following way:
 * 1. Method sets up database query and passes it to a helper function. Or it gets the cached version
 * and goes right to step 4.
 * 2. The helper method returns a CatnapResult or List<CatnapResult> object depending on the method
 * 3. Method caches result.
 * 4. entity is returned
 */
public class Session implements EntityManager {

    private final Connection connection;
    private final MappingStrategy mappingStrategy;
    private final CatnapCache cache;
    private final SessionFactory factory;

    private final static Logger logger = LogManager.getLogger(Session.class);

    public Session(Connection connection, MappingStrategy mappingStrategy, CatnapCache cache, SessionFactory factory) {
        this.connection = connection;
        this.mappingStrategy = mappingStrategy;
        this.cache = cache;
        this.factory = factory;
    }

    /**
     * This method gets an instance of an entity based on the entity's id. If the entity has been cached
     * it will return the cached version. We take the ResultSet from the query and use reflection to loop
     * through the fields of the entity, using the column name-field name mapping to get the data out of the
     * ResultSet and put them into the entity object.
     *
     * @param clazz              the type of entity to be returned
     * @param id                 the database identity of the entity to be returned
     * @return                   an Optional containing the entity if it was found
     * @throws CatnapException   thrown when a field can't be accessed, when the entity can't be instantiated,
     * or when an error occurs in accessing the database or database objects
     */
    @Override
    public Optional<Object> get(Class<?> clazz, int id) throws CatnapException {

        Optional<CatnapResult> entityOp;

        // get the entity
        if(cache.contains(clazz, id)) {
            entityOp = cache.get(clazz, id);
        } else {
            String sql;
            try {
                sql = this.mappingStrategy.get(clazz, id);
            } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
                String s = "There was an error trying to get a model of type: " + clazz.getName() + ", got: " + e.getMessage();
                throw new CatnapException(s);
            }

            PreparedStatement query;
            ResultSet rs;

            try {
                query = this.connection.prepareStatement(sql);
                rs = query.executeQuery();

            } catch (SQLException e) {
                String s = "There was an error performing a select on the database for entity type: " + clazz.getName() + ", error message:" + e.getMessage();
                logger.error(s);
                throw new CatnapException(s);
            }

            try {
                if(!rs.next()) {
                    return Optional.empty();
                }
            } catch (SQLException e) {
                String s = "There was an error when trying to move the cursor in the ResultSet for entity" + clazz.getName() + ". Got error: " + e.getMessage();
                logger.error(s);
                throw new CatnapException(s);
            }

            entityOp = buildEntity(clazz, rs);
            if(entityOp.isPresent()) {
                cache.store(entityOp.get());
            }
        }

        return entityOp.map(CatnapResult::getEntity);
    }

    /**
     * This method retrieves all the entities of type Class from the database. It assumes that not
     * all the entities are loaded into the cache, and loads them afterwards.
     *
     * @param clazz              the type of entity to get all records of
     * @return                   a list of the entities
     * @throws CatnapException   thrown when a field can't be accessed, when the entity can't be instantiated,
     * or when an error occurs in accessing the database or database objects
     */
    @Override
    public List<Object> getAll(Class<?> clazz) throws CatnapException {
        String sql = null;
        try {
            sql = this.mappingStrategy.getAll(clazz);
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            String s = "There was an error trying to get all of a model of type: " + clazz.getName() + ", got: " + e.getMessage();
            throw new CatnapException(s);
        }
        PreparedStatement query;
        ResultSet rs;

        try {
            query = this.connection.prepareStatement(sql);
            rs = query.executeQuery();

        } catch (SQLException e) {
            String s = "There was an error performing a select on the database for entity type: " + clazz.getName() + ", error message:" + e.getMessage();
            logger.error(s);
            throw new CatnapException(s);
        }

        List<CatnapResult> entities = new ArrayList<>();
        while(true) {
            try {
                if(!rs.next()) break;
            } catch (SQLException e) {
                String s = "There was an error when trying to move the cursor in the ResultSet for entity" + clazz.getName() + ". Got error: " + e.getMessage();
                logger.error(s);
                throw new CatnapException(s);
            }

            buildEntity(clazz, rs).ifPresent(entities::add);
        }

        cache.store(entities);

        return entities.stream()
                .map(CatnapResult::getEntity)
                .collect(Collectors.toList());
    }

    /**
     * This method removes a record of an entity from the database. It is also removed from the cache.
     * @param entity             the entity to delete
     * @throws CatnapException   thrown when a field can't be accessed, when the entity can't be instantiated,
     * or when an error occurs in accessing the database or database objects
     */
    @Override
    public void delete(Object entity) throws CatnapException {
        CatnapResult wrappedEntity = new CatnapResult(entity);
        Optional<Integer> entityId = wrappedEntity.getId();

        String sql = null;
        try {
            sql = this.mappingStrategy.delete(
                    wrappedEntity.getEntityType(),
                    entityId.orElseThrow(() -> new CatnapException("Entity type: " + entity.getClass() + " had no id field!"))
            );
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            String s = "There was an error trying to delete a model of type: " + wrappedEntity.getEntityType().getName() + ", got: " + e.getMessage();
            throw new CatnapException(s);
        }
        PreparedStatement query;

        try {
            query = this.connection.prepareStatement(sql);
            query.executeUpdate();
            cache.remove(wrappedEntity);

        } catch (SQLException e) {
            String s = "There was an error performing a select on the database for entity type: " + wrappedEntity.getEntityType().getName() + ", error message:" + e.getMessage();
            logger.error(s);
            throw new CatnapException(s);
        }
    }

    /**
     * This method inserts a record of an entity into the database. It is also adds it to the cache.
     * @param entity             the entity to persist
     * @throws CatnapException   thrown when a field can't be accessed, when the entity can't be instantiated,
     * or when an error occurs in accessing the database or database objects
     */
    @Override
    public void persist(Object entity) throws CatnapException {
        CatnapResult wrappedEntity = new CatnapResult(entity);
        String sql = null;
        try {
            sql = this.mappingStrategy.insert(entity);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            String s = "There was an error trying to persist a model of type: " + wrappedEntity.getEntityType().getName() + ", got: " + e.getMessage();
            throw new CatnapException(s);
        }

        PreparedStatement query;

        // store entity itself
        try {
            query = this.connection.prepareStatement(sql);
            query.executeUpdate();
            cache.store(wrappedEntity);

        } catch (SQLException e) {
            String s = "There was an error performing a select on the database for entity type: " + wrappedEntity.getEntityType().getName() + ", error message:" + e.getMessage();
            logger.error(s);
            throw new CatnapException(s);
        }
    }

    /**
     * This method updates a record of an entity in the database. It is also updated in the cache.
     * @param entity             the entity to update with the updated values
     * @throws CatnapException   thrown when a field can't be accessed, when the entity can't be instantiated,
     * or when an error occurs in accessing the database or database objects
     */
    @Override
    public void update(Object entity) throws CatnapException {
        CatnapResult wrappedEntity = new CatnapResult(entity);
        String sql = null;
        try {
            sql = this.mappingStrategy.update(entity);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            String s = "There was an error trying to update a model of type: " + wrappedEntity.getEntityType().getName() + ", got: " + e.getMessage();
            throw new CatnapException(s);
        }

        PreparedStatement query;

        // store entity itself
        try {
            query = this.connection.prepareStatement(sql);
            query.executeUpdate();
            cache.store(wrappedEntity);

        } catch (SQLException e) {
            String s = "There was an error performing a select on the database for entity type: " + wrappedEntity.getEntityType().getName() + ", error message:" + e.getMessage();
            logger.error(s);
            throw new CatnapException(s);
        }
    }

    /**
     * Begins a transaction
     *
     * @throws CatnapException    thrown when something goes wrong in starting a transaction
     */
    @Override
    public void beginTransaction() throws CatnapException {
        try {
            this.connection.setAutoCommit(false);
        } catch (SQLException e) {
            logger.error("There was an error when trying to start a transaction. Got: " + e.getMessage());
            throw new CatnapException();
        }
    }

    /**
     * Commits a transaction
     *
     * @throws RollbackException    thrown when something goes wrong when committing the transaction
     */
    @Override
    public void commit() throws RollbackException {
        try {
            this.connection.commit();
        } catch (SQLException e) {
            logger.error("There was an error when trying to commit a transaction. Got: " + e.getMessage());
            throw new RollbackException();
        }
    }

    /**
     * Rolls back the transaction to return the database to its pervious state
     *
     * @throws CatnapException    thrown when something goes wrong rolling back the database.
     */
    @Override
    public void rollback() throws CatnapException {
        try {
            this.connection.rollback();
        } catch (SQLException e) {
            logger.error("There was an error when trying to rollback a transaction. Got: " + e.getMessage());
            throw new CatnapException();
        }
    }

    /**
     * This method releases the connection this EntityManager was using
     */
    @Override
    public void close() {
        factory.releaseConnection(this.connection);
    }

    /**
     * This method builds a CatnapResult object and returns an Optional with it in it, or an empty one if the
     * build failed.
     * @param clazz              the type of entity that is being built
     * @param rs                 a JDBC ResultSet to extract the field data from
     * @return                   an Optional CatnapResult encapsulating the entity
     * @throws CatnapException   thrown when a field can't be accessed, when the entity can't be instantiated,
     * or when an error occurs in accessing the database or database objects
     */
    private Optional<CatnapResult> buildEntity(Class<?> clazz, ResultSet rs) throws CatnapException {
        Object entity;
        try {
            entity = clazz.newInstance();
        } catch (InstantiationException e) {
            String s = "Tried initializing an entity of type " + clazz.getName() + " and failed. It may be missing a no arg constructor!";
            logger.error(s);
            throw new CatnapException(s + ", error message: " + e.getMessage());
        } catch (IllegalAccessException e) {
            String s = "Unable to access constructor of type: " + clazz.getName() + " when trying to instantiate it.";
            logger.error(s);
            throw new CatnapException(s + ", error message: " + e.getMessage());
        }

        CatnapResult entityResult = new CatnapResult(entity);

        // get non associative field data into entity
        List<Field> fields = entityResult.getFields();
        for (Field field: fields) {
            field.setAccessible(true);
            try {
                field.set(entity, rs.getObject(field.getName()));
            } catch (IllegalAccessException e) {
                String s = "Was unable to access field: " + field.getName() + "of entity: " + entity.getClass().getName() +
                        " when trying to set it. Got: " + e.getMessage();
                logger.error(s);
                throw new CatnapException(s);
            } catch (SQLException e) {
                String s = "There was an error trying to get a non-associative field from the database. Got: " + e.getMessage();
                logger.error(s);
                throw new CatnapException(s);
            }
        }

        return Optional.of(entityResult);
    }
}
