package persistence;

import annotations.OneToMany;
import annotations.OneToOne;
import exceptions.CatnapException;
import exceptions.RollbackException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import util.Cache;
import util.CatnapResult;
import util.MappingStrategy;
import util.SimpleConnectionPool;

import javax.swing.text.html.Option;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
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
 * 4. Dependencies are resolved through a helper method.
 * 5. entity is finally returned
 */
public class Session implements EntityManager {

    private final Connection connection;
    private final MappingStrategy mappingStrategy;
    private final Cache cache;

    private final static Logger logger = LogManager.getLogger(SimpleConnectionPool.class);

    public Session(Connection connection, MappingStrategy mappingStrategy, Cache cache) {
        this.connection = connection;
        this.mappingStrategy = mappingStrategy;
        this.cache = cache;
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
            entityOp = getFromDatabase(clazz, id);
        }

        // now that we have entityOp we need to resolve dependencies
        if(entityOp.isPresent()) {
            CatnapResult entityResult = entityOp.get();

            if(entityResult.hasOneToOneField()) {
                resolveOneToOne(entityResult);
            }

            if(entityResult.hasOneToManyField()) {
                resolveOneToMany(entityResult);
            }

            return Optional.of(entityResult.getEntity());
        } else {
            return Optional.empty();
        }
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
        List<CatnapResult> entities = getAllFromDatabase(clazz);

        for(CatnapResult entity: entities) {
            if(entity.hasOneToOneField()) {
                resolveOneToOne(entity);
            }

            if(entity.hasOneToManyField()) {
                resolveOneToMany(entity);
            }
        }

        return entities.stream()
                .map(CatnapResult::getEntity)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(Object entity) throws CatnapException {
        CatnapResult wrappedEntity = new CatnapResult(entity);
        Optional<Integer> entityId = wrappedEntity.getId();

        String sql = this.mappingStrategy.delete(
                wrappedEntity.getEntityType(),
                entityId.orElseThrow(() -> new CatnapException("Entity type: " + wrappedEntity.getEntityType() + " had no id field!"))
        );
        PreparedStatement query;

        try {
            query = this.connection.prepareStatement(sql);
            query.executeUpdate();
            cache.remove(wrappedEntity); // cascading the delete is done in the cache

        } catch (SQLException e) {
            String s = "There was an error performing a select on the database for entity type: " + wrappedEntity.getEntityType().getName() + ", error message:" + e.getMessage();
            logger.error(s);
            throw new CatnapException(s);
        }
    }

    @Override
    public void persist(Object entity) throws CatnapException {
        CatnapResult wrappedEntity = new CatnapResult(entity);
        String sql = this.mappingStrategy.insert(entity);

        PreparedStatement query;

        // store entity itself
        try {
            query = this.connection.prepareStatement(sql);
            query.executeUpdate();
            cache.store(wrappedEntity); // store should also store the associated entities

        } catch (SQLException e) {
            String s = "There was an error performing a select on the database for entity type: " + wrappedEntity.getEntityType().getName() + ", error message:" + e.getMessage();
            logger.error(s);
            throw new CatnapException(s);
        }

        if(wrappedEntity.hasOneToOneField()) {
            List<Field> oneToOneFields = wrappedEntity.getOneToOneFields();

            for(Field f: oneToOneFields) {
                f.setAccessible(true);
                try {
                    Object e = f.get(entity);
                    persist(e, wrappedEntity.getId().orElseThrow(() -> new CatnapException("Tried to get id of " + wrappedEntity.getEntityType())));
                } catch (IllegalAccessException ex) {
                    String s = "Unable to access field of type: " + f.getType() + " when trying to instantiate it.";
                    logger.error(s);
                    throw new CatnapException(s + ", error message: " + ex.getMessage());
                }
            }
        }

        if(wrappedEntity.hasOneToManyField()) {
            List<Field> oneToManyFields = wrappedEntity.getOneToManyFields();

            for(Field f: oneToManyFields) {
                f.setAccessible(true);
                try {
                    // this should work because it has to be a collection
                    Collection<Object> relations = (Collection<Object>) f.get(entity);
                    for(Object e: relations) {
                        persist(e);
                    }
                } catch (IllegalAccessException e) {
                    String s = "Unable to access field of type: " + f.getType() + " when trying to instantiate it.";
                    logger.error(s);
                    throw new CatnapException(s + ", error message: " + e.getMessage());
                }
            }
        }
    }

    public void persist(Object entity, int id) throws NotImplementedException {
        throw new NotImplementedException();
    }

    @Override
    public void update(Object entity) throws CatnapException {
        String sql = this.mappingStrategy.update(entity);
    }

    @Override
    public void beginTransaction() throws CatnapException {

    }

    @Override
    public void commit() throws RollbackException {

    }

    @Override
    public void rollback() throws CatnapException {

    }

    @Override
    public void close() {

    }

    /**
     * This method retrieves the entity from the database and calls buildEntity
     * to initialize not associative fields. It stores the entity it found in the cache.
     * @param clazz            the type of entity to retrieve, corresponds to a table
     * @param id               the id of the entity to retrieve
     * @return                 an optional of a CatnapResult holding the entity
     * @throws CatnapException thrown when a field can't be accessed, when the entity can't be instantiated,
     * or when an error occurs in accessing the database or database objects
     */
    private Optional<CatnapResult> getFromDatabase(Class<?> clazz, int id) throws CatnapException {
        String sql = this.mappingStrategy.get(clazz, id);
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

        Optional<CatnapResult> entity = buildEntity(clazz, rs);
        entity.ifPresent(cache::store);
        return entity;
    }

    /**
     * This method is used to get all entities of a type clazz from the database. It stores them
     * all in the cache.
     * @param clazz               the type to get all entities of, corresponds to a table
     * @return                    a list of CatnapResults encapsulating the entities.
     * @throws CatnapException    thrown when a field can't be accessed, when the entity can't be instantiated,
     * or when an error occurs in accessing the database or database objects
     */
    private List<CatnapResult> getAllFromDatabase(Class<?> clazz) throws CatnapException {
        String sql = this.mappingStrategy.getAll(clazz);
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
        return entities;
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
        List<Field> nonAssociativeFields = entityResult.getNonAssociativeFields();
        for (Field field: nonAssociativeFields) {
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

        // get foreign key data
        List<String> columnNames = new ArrayList<>();
        List<String> nonAssociativeColumns = nonAssociativeFields.stream()
                .map(Field::getName)
                .collect(Collectors.toList());
        try {
            ResultSetMetaData rsMeta = rs.getMetaData();
            int columns = rsMeta.getColumnCount();
            for (int i = 0; i < columns; i++) {
                String columnName = rsMeta.getColumnName(i);
                if(!nonAssociativeColumns.contains(columnName)) {
                    columnNames.add(columnName);
                }
            }
        } catch (SQLException e) {
            String s = "There was an error trying to get metadata on the resulting query. Got: " + e.getMessage();
            logger.error(s);
            throw new CatnapException(s);
        }

        for (String column: columnNames) {
            try {
                entityResult.addForeignKey(column, rs.getInt(column));
            } catch (SQLException e) {
                String s = "There was an error trying to get the foreign key field from the database. Got: " + e.getMessage();
                logger.error(s);
                throw new CatnapException(s);
            }
        }

        return Optional.of(entityResult);
    }

    /**
     * This method is used to add the related entity to the field marked with the OneToOne annotation.
     * It queries the database for all entities of the field type and searches through their foreign keys
     * for a match.
     *
     * this method is called on an entity that has the form:
     * <pre>
     * public class RealEntity {
     *     // other fields omitted for brevity
     *     &#64;OneToOne(name = "realentity_id")
     *     public OtherRealEntity otherRealEntity;
     * }
     * </pre>
     *
     * Then otherRealEntity has a foreign key with RealEntity's primary key.
     *
     * @param entityResult   the entity to resolve
     */
    private void resolveOneToOne(CatnapResult entityResult) throws CatnapException {
        List<Field> fields = entityResult.getOneToOneFields();

        for (Field field: fields) {
            field.setAccessible(true);
            List<CatnapResult> entities = getAllFromDatabase(field.getType());

            String foreignKeyFieldName = field.getAnnotation(OneToOne.class).name();
            int id = entityResult.getId()
                    .orElseThrow(() -> new CatnapException("The entity of type: " + entityResult.getEntity().getClass() + "failed to get its id!"));

            Optional<CatnapResult> relation = entities.stream()
                    .filter(r -> {
                        if(r.getForeignKey(foreignKeyFieldName).isPresent()) {
                            return r.getForeignKey(foreignKeyFieldName).get() == id;
                        } else {
                            return false;
                        }
                    })
                    .findFirst();

            if(relation.isPresent()) {
                try {
                    field.set(entityResult.getEntity(), relation.get().getEntity());
                } catch (IllegalAccessException e) {
                    String s = "Was unable to access field: " + field.getName() + "of entity: " + entityResult.getEntityType().getName() +
                            " when trying to set it. Got: " + e.getMessage();
                    logger.error(s);
                    throw new CatnapException(s);
                }
            }
        }
    }

    /**
     * This method adds the entities that an entity declares as associated to it into the relevant fields.
     * This method can't rely on the cache having all the entities, so it queries the database for them.
     *
     * this method is called on an entity that has the form:
     * <pre>
     * public class RealEntity {
     *     // other fields omitted for brevity
     *     &#64;OneToMany(name = "realentity_id")
     *     public OtherRealEntity otherRealEntity;
     * }
     * </pre>
     *
     * Then otherRealEntity has a foreign key with RealEntity's primary key.
     *
     * @param entityResult   the entity to resolve
     */
    private void resolveOneToMany(CatnapResult entityResult) throws CatnapException {
        List<Field> fields = entityResult.getOneToManyFields();

        for(Field field: fields) {
            field.setAccessible(true);
            ParameterizedType entityType = (ParameterizedType) field.getGenericType();
            Class<?> entityClass = (Class<?>) entityType.getActualTypeArguments()[0];
            List<CatnapResult> entities = getAllFromDatabase(entityClass);

            String foreignKeyFieldName = field.getAnnotation(OneToMany.class).name();
            int id = entityResult.getId()
                    .orElseThrow(() -> new CatnapException("The entity of type: " + entityResult.getEntity().getClass() + "failed to get its id!"));

            List<Object> relations = entities.stream()
                    .filter(r -> {
                        if(r.getForeignKey(foreignKeyFieldName).isPresent()) {
                            return r.getForeignKey(foreignKeyFieldName).get() == id;
                        } else {
                            return false;
                        }
                    })
                    .map(CatnapResult::getEntity)
                    .collect(Collectors.toList());

            try {
                field.set(entityResult.getEntity(), relations);
            } catch (IllegalAccessException e) {
                String s = "Was unable to access field: " + field.getName() + "of entity: " + entityResult.getEntityType().getName() +
                        " when trying to set it. Got: " + e.getMessage();
                logger.error(s);
                throw new CatnapException(s);
            }
        }
    }
}
