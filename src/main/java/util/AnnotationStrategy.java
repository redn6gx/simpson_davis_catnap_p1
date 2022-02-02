package util;

import annotations.Entity;
import annotations.Id;
import annotations.Length;
import annotations.OrderBy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * This class is used to map each Java datatype to its appropriate postgresql data type.
 * The methods addModel and buildSchema are intended to be called by the user of the ORM.
 * The rest of the non-helper methods are intended to be utilized by Session Class to
 * generate queries that will be added to prepared statements.
 * For more detailed method descriptions see MappingStrategy Interface.
 */
public class AnnotationStrategy<T> implements MappingStrategy<T>{
    private final static Logger logger = LogManager.getLogger(AnnotationStrategy.class);
    private static List<Object> models = new ArrayList<>();

    @Override
    public Object addModel(Object model){
        models.add(model);

        return model;
    }

    @Override
    public String buildSchema(List<Object> models){
        StringBuilder result = new StringBuilder("");
        models.stream().forEach(model -> {
            try {
                result.append(createTable(model.getClass()));
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        });

        return String.valueOf(result);
    }

    @Override
    public String insert(Object instanceObject) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        StringBuilder query = new StringBuilder("INSERT INTO ");
        Class clazz = instanceObject.getClass();
        query.append(getTableName(clazz)).append(" VALUES (");

        Field[] fields = clazz.getFields();
        int count = 1;
        for(Field f : fields){
            if(f.isAnnotationPresent(Id.class)){
                if(count == fields.length){
                    query.append("default");
                }else{
                    query.append("default, ");
                }
            }else{
                if(count == fields.length){
                    if(hasSingleQuotes(f.getType().getName())){
                        query.append("'" + f.get(instanceObject) + "'");
                    }else{
                        query.append(f.get(instanceObject) + "");
                    }
                }else{
                    if(hasSingleQuotes(f.getType().getName())){
                        query.append("'" + f.get(instanceObject) + "', ");
                    }else{
                        query.append(f.get(instanceObject) + ", ");
                    }
                }
            }
            count++;
        }
        query.append(") RETURNING *;");

        return String.valueOf(query);
    }

    @Override
    public String get(Class clazz, int id) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        StringBuilder query = new StringBuilder("SELECT * FROM ");
        query.append(getTableName(clazz) + " WHERE ");

        Field[] fields = clazz.getFields();
        String pkFieldName = null;
        for(Field f : fields) {
            if (f.isAnnotationPresent(Id.class)) {
                pkFieldName = f.getName();
            }
        }
        query.append(pkFieldName + " = " + id + ";");

        return String.valueOf(query);
    }

    @Override
    public String getAll(Class clazz) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        StringBuilder query = new StringBuilder("SELECT * FROM ");
        query.append(getTableName(clazz));

        int count = 0;
        Field[] fields = clazz.getFields();
        for(Field f : fields) {
            if (f.isAnnotationPresent(OrderBy.class)) {
                Object direction = null;
                Annotation annotation = f.getAnnotation(OrderBy.class);
                Method m = annotation.annotationType().getMethod("direction");
                direction = m.invoke(annotation);
                if(count == 0){
                    query.append(" ORDER BY " + f.getName() + " " + direction);
                }else if(count > 0){
                    query.append(", " + f.getName() + " " + direction);
                }
                count++;
            }
        }
        query.append(";");

        return String.valueOf(query);
    }

    @Override
    public String update(Object instanceObject) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        StringBuilder query = new StringBuilder("UPDATE ");
        Class clazz = instanceObject.getClass();
        query.append(getTableName(clazz) + " SET ");

        int count = 1;
        String pkFieldName = null;
        Object pkValue = null;
        Field[] fields = clazz.getFields();
        for(Field f : fields){
            String fieldName = f.getName();
            if(!f.isAnnotationPresent(Id.class)) {
                if(count != fields.length){
                    if(hasSingleQuotes(f.getType().getName())){
                        query.append(fieldName + " = '" + f.get(instanceObject) + "', ");
                    }else{
                        query.append(fieldName + " = " + f.get(instanceObject) + ", ");
                    }
                }else{
                    if(hasSingleQuotes(f.getType().getName())){
                        query.append(fieldName + " = '" + f.get(instanceObject) + "' WHERE ");
                    }else{
                        query.append(fieldName + " = " + f.get(instanceObject) + " WHERE ");
                    }
                }
            }else{
                pkFieldName = fieldName;
                pkValue = f.get(instanceObject);
            }
            count++;
        }
        query.append(pkFieldName + " = " + pkValue + " RETURNING *;");

        return String.valueOf(query);
    }

    @Override
    public String delete(Class clazz, int id) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        StringBuilder query = new StringBuilder("DELETE FROM ");
        query.append(getTableName(clazz) + " WHERE ");

        Field[] fields = clazz.getFields();
        String pkFieldName = null;
        for(Field f : fields) {
            if (f.isAnnotationPresent(Id.class)) {
                pkFieldName = f.getName();
            }
        }
        query.append(pkFieldName + " = " + id + " RETURNING *;");

        return String.valueOf(query);
    }

    //HELPER METHODS
    /**
     * This is a helper method used to generate a create table sql statement for a class. This method utilizes
     * reflection to check the data types of the properties inside a class and then maps them to the corresponding
     * postgresql data types. The file resources.mapping.properties is used to look up the correct data type mappings.
     * The variable names for each property are used as the data column names in postgresql. Variable names are
     * also retrieved through reflection.
     * Makes use of the getTableName() helper method.
     *
     * @return a String containing the generated sql create table statement.
     * @throws InvocationTargetException Helper method getTableName() throws this exception.
     * @throws NoSuchMethodException Helper method getTableName() throws this exception.
     * @throws IllegalAccessException Helper method getTableName() throws this exception.
     * @param clazz the class to be translated into a create table statement.
     */
    public String createTable(Class clazz) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        StringBuilder query = new StringBuilder("CREATE TABLE ");
        query.append(getTableName(clazz)).append(" (\n");

        Properties props = new Properties();
        try{
            props.load(AnnotationStrategy.class.getClassLoader().getResourceAsStream("mapping.properties"));
        } catch(IOException e){
            logger.error(e.getMessage());
        }

        boolean hasPk = false;
        String pkName = "";
        int count = 1;
        Field[] fields = clazz.getFields();
        for(Field f : fields){
            String fieldName = f.getName();
            query.append("  ").append(fieldName).append(" ");
            if(f.isAnnotationPresent(Id.class)){
                query.append("serial");
                hasPk = true;
                pkName = fieldName;
            }else if(f.getType().getSimpleName().equals("String")){
                query.append(handleString(props, f));
            }
            else{
                query.append(props.getProperty(f.getType().getSimpleName()));
            }
            if(!(count == fields.length)){
                query.append(",\n");
            }
            count++;
        }

        if(hasPk){
            query.append(",\n  primary key (").append(pkName).append(")");
        }
        query.append("\n);");

        return String.valueOf(query);
    }

    /**
     * This is a helper method that retrieves the table name from the class provided.
     * Reflection is used to get the @Entity annotation and execute its property
     * method to retrieve the value. The table name is set to the values retrieved.
     * If the value is equal to the default, "none", then the class name is used as the
     * table name instead.
     *
     * @return a String with the table's name.
     * @throws IllegalAccessException occurs if the method being executed through reflection is inaccessible.
     * @throws InvocationTargetException occurs if the method being executed through reflection cannot be found.
     * @throws NoSuchMethodException occurs if the @Entity property method is not found through reflection.
     * @param clazz the class to retrieve the table name from.
     */
    private String getTableName(Class clazz) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Object tableName = null;
        Annotation annotation = clazz.getAnnotation(Entity.class);
        Method m = annotation.annotationType().getMethod("name");
        tableName = m.invoke(annotation);

        return !tableName.equals("none") ? tableName + "" : clazz.getSimpleName();
    }

    /**
     * This is a helper method that handles the mapping of a String to its appropriate
     * postgresql datatype. The method uses reflection to check if the @Length annotation
     * is present. If a @Length annotation is found, then the size is retrieved from the
     * property method belonging to the annotation and inserted into the sql String.
     * If no @Length annotation is present, then the size is set to '(50)';
     *
     * @return a String with the table's name.
     * @throws IllegalAccessException occurs if the method being executed through reflection is inaccessible.
     * @throws InvocationTargetException occurs if the method being executed through reflection cannot be found.
     * @throws NoSuchMethodException occurs if the @Entity property method is not found through reflection.
     * @param props the class to retrieve the table name from.
     * @param f the field to
     */
    private String handleString(Properties props, Field f) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if(f.isAnnotationPresent(Length.class)) {
            Object size = null;
            Annotation annotation = f.getAnnotation(Length.class);
            Method m = annotation.annotationType().getMethod("size");
            size = m.invoke(annotation);
            return props.getProperty(f.getType().getSimpleName()) + "(" + size + ")";
        }else{
            return props.getProperty(f.getType().getSimpleName()) + "(50)";
        }
    }

    /**
     * This is a helper method that checks to see if a sql value should be wrapped in single quotes.
     * If the datatype matches either a String, Character, or char, then the method returns true.
     *
     * @return a boolean indicating if the datatype should be wrapped in single quotes.
     * @param dataType the name of the variable's datatype.
     */
    private boolean hasSingleQuotes(String dataType){
        if("java.lang.String".equals(dataType) || "java.lang.Character".equals(dataType) || "char".equals(dataType)) {
            return true;
        }
        return false;
    }
}
