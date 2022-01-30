package util;

import annotations.Id;
import annotations.OneToMany;
import annotations.OneToOne;
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
    private final Map<String, Integer> foreignKeys;

    public CatnapResult(Object entity) {
        this.entity = entity;
        this.foreignKeys = new HashMap<>();
    }

    public Object getEntity() {
        return entity;
    }

    public Class<?> getEntityType() { return entity.getClass(); }

    public Optional<Integer> getForeignKey(String fieldName) {
        return Optional.of(this.foreignKeys.get(fieldName));
    }

    public void addForeignKey(String fieldName, int value) {
        this.foreignKeys.put(fieldName, value);
    }

    /**
     * This method returns an Optional containing the id of the entity if it finds the id field.
     * @return                    optional containing the id of the entity
     * @throws CatnapException    thrown when the field was inaccessible
     */
    public Optional<Integer> getId() throws CatnapException {
        // may refactor getIdField into this method if it's never used
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
     * Filters through the fields of the entity and returns ones that are not annotated with
     * one of the assication annotations: OneToOne or OneToMany
     *
     * @return               a list of fields that are not marked with OneToOne or OneToMany
     */
    public List<Field> getNonAssociativeFields() {
        return Arrays.stream(this.entity.getClass().getDeclaredFields())
                .filter(field -> !(field.isAnnotationPresent(OneToOne.class) || field.isAnnotationPresent(OneToMany.class)))
                .collect(Collectors.toList());
    }

    /**
     * Checks to see if the entity has a one to one relationship defined
     *
     * @return                whether the entity has a one to one relationship defined
     */
    public boolean hasOneToOneField() {
        return Arrays.stream(this.entity.getClass().getDeclaredFields())
                .anyMatch(e -> e.isAnnotationPresent(OneToOne.class));
    }

    /**
     * Checks to see if the entity has a one to many relationship defined
     *
     * @return                whether the entity has a one to many relationship defined
     */
    public boolean hasOneToManyField() {
        return Arrays.stream(this.entity.getClass().getDeclaredFields())
                .anyMatch(e -> e.isAnnotationPresent(OneToMany.class));
    }

    /**
     * Retrieves a list of fields with the OneToOne annotation
     *
     * @return                list of fields with the OneToOne annotation
     */
    public List<Field> getOneToOneFields() {
        return Arrays.stream(this.entity.getClass().getDeclaredFields())
                .filter(e -> e.isAnnotationPresent(OneToOne.class))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a list of fields with the OneToMany annotation
     *
     * @return                list of fields with the OneToOne annotation
     */
    public List<Field> getOneToManyFields() {
        return Arrays.stream(this.entity.getClass().getDeclaredFields())
                .filter(e -> e.isAnnotationPresent(OneToMany.class))
                .collect(Collectors.toList());
    }
}
