package util;

public interface MappingStrategy<T> {
    public String createTable(Class clazz);
    public String dropTable(Class clazz);
    public String insert(T instanceObject) throws IllegalAccessException;
    public String get(Class clazz, int id);
    public String getAll(Class clazz);
    public String update(T instanceObject);
    public String delete(Class clazz, int id);
}
