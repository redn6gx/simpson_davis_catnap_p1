package util;

import annotations.Entity;
import annotations.Id;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;


//TODO
//change to string builders

public class AnnotationStrategy implements MappingStrategy{
    @Override
    public String createTable(Class clazz){
        String query = "CREATE TABLE ";

        Object tableName = null;
        try{
            Annotation annotation = clazz.getAnnotation(Entity.class);
            Method m = annotation.annotationType().getMethod("name");
            tableName = m.invoke(annotation);
        } catch(NoSuchMethodException | java.lang.IllegalAccessException | java.lang.reflect.InvocationTargetException e){
            System.out.println(e.getMessage());
        }

        String name = !tableName.equals("none") ? tableName + "" : clazz.getSimpleName();
        query += (name + " (\n");

        Properties props = new Properties();
        try{
            props.load(AnnotationStrategy.class.getClassLoader().getResourceAsStream("mapping.properties"));
        } catch(IOException e){
            System.out.println(e.getMessage());
        }

        boolean hasPk = false;
        String pkName = "";
        int count = 1;
        Field[] fields = clazz.getDeclaredFields();
        for(Field f : fields){
            String fieldName = f.getName();
            query += "  " + fieldName + " ";
            if(f.isAnnotationPresent(Id.class)){
                query += "serial";
                hasPk = true;
                pkName = fieldName;
            }else{
                query += (props.getProperty(f.getType().getName()));
            }
            if(!(count >= fields.length)){
                query += ",\n";
            }
            count++;
        }
        if(hasPk){
            query += ",\n  primary key (" + pkName + ")";
        }

        query += "\n);";

        return query;
    }
    @Override
    public String dropTable(Class clazz){
        String query = null;
        return query;
    }
    @Override
    public String insert(Class clazz){
        String query = null;
         return query;
    }
    @Override
    public String get(Class clazz){
        String query = null;
        return query;
    }
    @Override
    public String getAll(Class clazz){
        String query = null;
        return query;
    }
    @Override
    public String update(Class clazz){
        String query = null;
        return query;
    }
    @Override
    public String delete(Class clazz){
        String query = null;
        return query;
    }
}
