package persistence;

import annotations.Id;
import annotations.OneToMany;
import annotations.OneToOne;
import exceptions.CatnapException;
import exceptions.RollbackException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.Cache;
import util.MappingStrategy;
import util.SimpleConnectionPool;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * This class represents the persistence and is used to persist entities. It maintains a cache of
 * entities.
 *
 * Entities are cached in the database operating methods (defined in EntityManager).
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

    @Override
    public Optional<Object> get(Class<?> clazz, int id) throws CatnapException {
        if(cache.contains(clazz, id)) {
            return cache.get(clazz, id);
        }

        String sql = this.mappingStrategy.get(clazz, id);

        Optional<Object> entity;
        try {
            PreparedStatement query = this.connection.prepareStatement(sql);
            ResultSet rs = query.executeQuery();

            entity = buildEntity(clazz, rs);
            entity.ifPresent(cache::store);
        } catch (SQLException e) {
            logger.error("There was an error performing a select on the database for entity type: " + clazz.getName() + ", error message:" + e.getMessage());
            throw new CatnapException(e.getMessage());
        }

        return entity;
    }


    @Override
    public List<Object> getAll(Class<?> clazz) throws CatnapException {
        return null;
    }

    @Override
    public void delete(Object entity) throws CatnapException {

    }

    @Override
    public void persist(Object entity) throws CatnapException {

    }

    @Override
    public void update(Object entity) throws CatnapException {

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

    private Optional<Object> buildEntity(Class<?> clazz, ResultSet rs) throws CatnapException {
        try {
            if(!rs.next()) {
                return Optional.empty();
            }
        } catch (SQLException e) {
            String s = "There was an error when trying to move the cursor in the ResultSet for entity" + clazz.getName() + ". Got error: " + e.getMessage();
            logger.error(s);
            throw new CatnapException(s);
        }

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

        Field[] fields = clazz.getDeclaredFields();
        for (Field f: fields) {
            extractFieldData(f, entity, rs);
        }

        return Optional.of(entity);
    }

    private void extractFieldData(Field f, Object entity, ResultSet rs) throws CatnapException {
        f.setAccessible(true);

        // First we check if field has OneToMany annotation, where we then load all of the entities
        // of that type into the database and build the assocations
        if(f.isAnnotationPresent(OneToMany.class)) {
            ParameterizedType entityType = (ParameterizedType) f.getGenericType();
            Class<?> entityClass = (Class<?>) entityType.getActualTypeArguments()[0];
            List<Object> entities = getAll(entityClass);

            try {
                f.set(entity, entities.stream().map((e) -> e.getClass().cast(e)).collect(Collectors.toList()));
            } catch (IllegalAccessException e) {
                String s = "Tried to set field: " + f.getName() + " of type" + entity.getClass().getName() +
                            " and failed. Got message: " + e.getMessage();
                logger.error(s);
                throw new CatnapException(s);
            }

            // Secondly we check if the field has the OneToOne annotation on it. If it does we load
            // all of the entities into the database and filter out the one we need.
        } else if(f.isAnnotationPresent(OneToOne.class)) {
            List<Object> entities = getAll(entity.getClass());
            String foreignKeyFieldName = entity.getClass().getAnnotation(OneToOne.class).name();
            int id;
            try {
                id = rs.getInt(foreignKeyFieldName);
            } catch (SQLException e) {
                String s = "There was an error trying to get the foreign key field from the database. Got: " + e.getMessage();
                logger.error(s);
                throw new CatnapException(s);
            }

            Optional<Object> relatedObject = entities.stream().filter((relatedEntity) -> {
                Optional<Field> idField = getIdFromEntity(entity);

                if(idField.isPresent()) {
                    idField.get().setAccessible(true);

                    try {
                        return idField.get().getInt(relatedEntity) == id;
                    } catch (IllegalAccessException e) {
                        logger.error("Unable to access related entity id, " + e.getMessage());
                        return false;
                    }
                } else {
                    return false;
                }
            }).findFirst();

            if(relatedObject.isPresent()) {
                try {
                    f.set(entity, relatedObject.get());
                } catch (IllegalAccessException e) {
                    String s = "Was unable to access field: " + f.getName() + "of entity: " + entity.getClass().getName() +
                                " when trying to set it. Got: " + e.getMessage();
                    logger.error(s);
                    throw new CatnapException(s);
                }
            } else {
                String s = "Dependent entity: " + entity.getClass().getName() + " had no related entity! " +
                        f.getType().getName() + "Make sure the foreign keys are set properly in the database.";
                logger.error(s);
                throw new CatnapException(s);
            }
            // If we have neither annotation we're a normal field, and we can just set the data
        } else {
            try {
                f.set(entity, rs.getObject(f.getName()));
            } catch (IllegalAccessException e) {
                String s = "Was unable to access field: " + f.getName() + "of entity: " + entity.getClass().getName() +
                        " when trying to set it. Got: " + e.getMessage();
                logger.error(s);
                throw new CatnapException(s);
            } catch (SQLException e) {
                String s = "There was an error trying to get the foreign key field from the database. Got: " + e.getMessage();
                logger.error(s);
                throw new CatnapException(s);
            }
        }
    }

    /**
     * Gets the id field of the entity
     *
     * @param entity          the entity from which the field comes
     * @return                an optional contains the field
     */
    private Optional<Field> getIdFromEntity(Object entity) {
        return Arrays.stream(entity.getClass().getDeclaredFields())
                .filter((f) -> f.isAnnotationPresent(Id.class))
                .findFirst();
    }
}