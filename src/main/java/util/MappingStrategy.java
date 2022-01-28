package util;

public interface MappingStrategy {
    public String createTable(Class clazz);
    public String dropTable(Class clazz);
    public String insert(Class clazz, int id);
    public String get(Class clazz, int id);
    public String getAll(Class clazz);
    public String update(Class clazz, int id);
    public String delete(Class clazz, int id);
}
