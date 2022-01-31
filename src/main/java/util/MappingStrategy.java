package util;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public interface MappingStrategy<T> {
    public void AddModel(Object model);
    public String buildSchema();
    public String createTable(Class clazz) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException;
    public String insert(Object instanceObject) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException;
    public String get(Class clazz, int id) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException;
    public String getAll(Class clazz) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException;
    public String update(Object instanceObject) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException;
    public String delete(Class clazz, int id) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException;
}
