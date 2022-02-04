package util;

import exceptions.ConnectionFailedException;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public interface MappingStrategy {
    /**
     * This method is used to add a model to the list of objects that will be added to the
     * database schema.
     *
     * @return the same object added to the schema. (Mainly for testing purposes)
     * @param model the model to be added to the database schema.
     */
    public Object addModel(Object model);

    /**
     * This method is used to create the database schema by calling the createTable() method on
     * each model object in the models list.
     *
     * @return a String with all the generated sql create table statements that were used to make the
     * database schema. Create table statements will be separated by a semicolon ";".
     * @throws InvocationTargetException Helper method getTableName() throws this exception.
     * @throws NoSuchMethodException Helper method getTableName() throws this exception.
     * @throws IllegalAccessException Helper method getTableName() throws this exception.
     * @param models the list of models to be added to the schema.
     */
    public String buildSchema(List<Object> models) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException;

    /**
     * This method is used to generate a sql insert statement for a new instance of an entity that is to be added to a
     * database table. Reflection is used to get the values of the properties inside a class and generate the
     * sql statement accordingly. This method also uses reflection to check if the @Id annotation is present. If the @Id
     * annotation is found, then the value will be set to default.
     * Makes use of the getTableName() helper method.
     *
     * @return a String with the generated sql insert statement.
     * @throws IllegalArgumentException thrown if an invalid argument is passed to the field.get() reflection method.
     * @throws IllegalAccessException thrown if field.get() reflection method does not have access to the class or field.
     *                                Helper method getTableName() throws this exception.
     * @throws InvocationTargetException Helper method getTableName() throws this exception.
     * @throws NoSuchMethodException Helper method getTableName() throws this exception.
     * @param instanceObject the instance of the model to be inserted into the database.
     */
    public String insert(Object instanceObject) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, NoSuchMethodException;

    /**
     * This method generates a get sql statement that retrieves a single row matching the primary key provided.
     * Reflection is used to find the @Id annotation and associated retrieve the variable name (name mapped to the primary kay column name).
     * Makes use of the getTableName() helper method.
     *
     * @return a String with the generated sql select statement.
     * @throws InvocationTargetException Helper method getTableName() throws this exception.
     * @throws NoSuchMethodException Helper method getTableName() throws this exception.
     * @throws IllegalAccessException Helper method getTableName() throws this exception.
     * @param clazz the class associated to the table we want to retrieve data from.
     * @param id the primary key of the row we want to retrieve.
     */
    public String get(Class clazz, int id) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException;

    /**
     * This method generates a sql select statement to retrieve all rows from a given table in the database.
     * Makes use of the getTableName() helper method.
     *
     * @return a String with the generated sql select statement.
     * @throws InvocationTargetException Helper method getTableName() throws this exception.
     * @throws NoSuchMethodException Helper method getTableName() throws this exception.
     * @throws IllegalAccessException Helper method getTableName() throws this exception.
     * @param clazz the class associated to the table we want to retrieve data from.
     */
    public String getAll(Class clazz) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException;

    /**
     * This method generates a sql update statement for an existing row in the database.
     * Reflection is used to get the values of the properties inside a class. This method also
     * uses reflection to check if the @Id annotation is present. If the @Id
     * annotation is found, then that value will be inserted into the WHERE clause.
     * Makes use of the getTableName() helper method.
     *
     * @return a String with the generated sql update statement.
     * @throws IllegalAccessException Helper method getTableName() throws this exception.
     * @throws InvocationTargetException Helper method getTableName() throws this exception.
     * @throws NoSuchMethodException Helper method getTableName() throws this exception.
     * @param instanceObject the instance of the model containing the new values to be updated in the database.
     */
    public String update(Object instanceObject) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException;

    /**
     * This method generates a sql delete statement for an existing row in the database.
     * Reflection is used to check if the @Id annotation is present. If the @Id
     * annotation is found, then the name of the variable is retrieved for the
     * corresponding column name.
     * Makes use of the getTableName() helper method.
     *
     * @return a String with the generated sql delete statement.
     * @throws IllegalAccessException Helper method getTableName() throws this exception.
     * @throws InvocationTargetException Helper method getTableName() throws this exception.
     * @throws NoSuchMethodException Helper method getTableName() throws this exception.
     * @param clazz the class associated to the table we want to delete data from.
     * @param id the primary key of the row we want to delete.
     */
    public String delete(Class clazz, int id) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException;
}
