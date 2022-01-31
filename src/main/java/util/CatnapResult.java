package util;

import annotations.Id;
import exceptions.CatnapException;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class is a container for entity results from the database. It contains the entity
 * that was retrieved from the database, any foreign keys associated with the object, and
 * common methods that are used to retrieve data about the entity. These objects are what
 * are stored in the cache.
 */
public class CatnapResult {

    private final Object entity;

    public CatnapResult(Object entity) {
        this.entity = entity;
    }

    public Object getEntity() {
        return entity;
    }

    public Class<?> getEntityType() { return entity.getClass(); }

    /**
     * This method returns an Optional containing the id of the entity if it finds the id field.
     * @return                    optional containing the id of the entity
     * @throws CatnapException    thrown when the field was inaccessible
     */
    public Optional<Integer> getId() throws CatnapException {
        Optional<Field> idField = getIdField();
        if(idField.isPresent()) {
            idField.get().setAccessible(true);
            try {
                return Optional.of(idField.get().getInt(this.entity));
            } catch (IllegalAccessException e) {
                String s = "Unable to access the id field of type: " + this.entity.getClass().getName() + "!";
                throw new CatnapException(s + ", error message: " + e.getMessage());
            }
        } else {
            return Optional.empty();
        }
    }

    /**
     * This method returns an empty Optional if no field was found.
     *
     * @return                a field object containing the entity's id field
     */
    public Optional<Field> getIdField() {
        return Arrays.stream(this.entity.getClass().getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Id.class))
                .findFirst();
    }

    /**
     * returns a list of declared fields
     *
     * @return               a list of the declared fields
     */
    public List<Field> getFields() {
        return Arrays.stream(this.entity.getClass().getDeclaredFields()).collect(Collectors.toList());
    }
}
