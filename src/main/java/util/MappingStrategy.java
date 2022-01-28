package util;

import java.sql.PreparedStatement;

public interface MappingStrategy<T> {
    public String createTable(Class clazz);
    public String dropTable(Class clazz);
    public String insert(Class clazz);
    public String get(Class clazz);
    public String getAll(Class clazz);
    public String update(Class clazz);
    public String delete(Class clazz);
}
