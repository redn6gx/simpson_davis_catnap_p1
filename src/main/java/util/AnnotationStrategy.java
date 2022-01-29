package util;

import annotations.Entity;
import annotations.Id;
import javafx.scene.effect.Reflection;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class AnnotationStrategy<T> implements MappingStrategy<T>{
    @Override
    public String createTable(Class clazz){
        StringBuilder query = new StringBuilder("CREATE TABLE ");
        query.append(getTableName(clazz)).append(" (\n");

        Properties props = new Properties();
        try{
            props.load(AnnotationStrategy.class.getClassLoader().getResourceAsStream("mapping.properties"));
        } catch(IOException e){
            System.out.println(e.getMessage());
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
            }else{
                query.append(props.getProperty(f.getType().getName()));
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
    @Override
    public String insert(Object instanceObject) throws IllegalArgumentException, IllegalAccessException {
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
        query.append(");");

        return String.valueOf(query);
    }
    @Override
    public String get(Class clazz, int id){
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
    public String getAll(Class clazz){
        StringBuilder query = new StringBuilder("SELECT * FROM ");
        query.append(getTableName(clazz) + ";");

        return String.valueOf(query);
    }
    @Override
    public String update(Object instanceObject) throws IllegalAccessException {
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
        query.append(pkFieldName + " = " + pkValue + ";");

        return String.valueOf(query);
    }
    @Override
    public String delete(Class clazz, int id){
        StringBuilder query = new StringBuilder();
        return String.valueOf(query);
    }

    //Helper Method
    private String getTableName(Class clazz){
        Object tableName = null;
        try{
            Annotation annotation = clazz.getAnnotation(Entity.class);
            Method m = annotation.annotationType().getMethod("name");
            tableName = m.invoke(annotation);
        } catch(NoSuchMethodException | java.lang.IllegalAccessException | java.lang.reflect.InvocationTargetException e){
            System.out.println(e.getMessage());
        }

        return !tableName.equals("none") ? tableName + "" : clazz.getSimpleName();
    }
    private boolean hasSingleQuotes(String dataType){
        if("java.lang.String".equals(dataType) || "java.lang.Character".equals(dataType) || "char".equals(dataType)) {
            return true;
        }
        return false;
    }
}
