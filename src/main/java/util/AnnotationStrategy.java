package util;

import annotations.Entity;
import annotations.Id;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;


//TODO
//change to string builders

public class AnnotationStrategy implements MappingStrategy{
    @Override
    public String createTable(Class clazz){
        StringBuilder query = new StringBuilder("CREATE TABLE ");

        Object tableName = null;
        try{
            Annotation annotation = clazz.getAnnotation(Entity.class);
            Method m = annotation.annotationType().getMethod("name");
            tableName = m.invoke(annotation);
        } catch(NoSuchMethodException | java.lang.IllegalAccessException | java.lang.reflect.InvocationTargetException e){
            System.out.println(e.getMessage());
        }

        String name = !tableName.equals("none") ? tableName + "" : clazz.getSimpleName();
        query.append(name).append(" (\n");

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
            if(!(count >= fields.length)){
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
    public String dropTable(Class clazz){
        StringBuilder query = new StringBuilder();
        return String.valueOf(query);
    }
    @Override
    public String insert(Class clazz, int id){
        StringBuilder query = new StringBuilder();
        return String.valueOf(query);
    }
    @Override
    public String get(Class clazz, int id){
        StringBuilder query = new StringBuilder();
        return String.valueOf(query);
    }
    @Override
    public String getAll(Class clazz){
        StringBuilder query = new StringBuilder();
        return String.valueOf(query);
    }
    @Override
    public String update(Class clazz, int id){
        StringBuilder query = new StringBuilder();
        return String.valueOf(query);
    }
    @Override
    public String delete(Class clazz, int id){
        StringBuilder query = new StringBuilder();
        return String.valueOf(query);
    }
}
