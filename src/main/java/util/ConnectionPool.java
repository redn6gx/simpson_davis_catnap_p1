package util;

import exceptions.ConnectionFailedException;

import java.sql.Connection;

public interface ConnectionPool {
    /**
     * This method is used to connect to a data source. Implementing classes
     * will need to manage the required parameters for connecting to their data source.
     *
     * @throws ConnectionFailedException occurs when connecting to the database fails
     */
    public void connect() throws ConnectionFailedException;

    /**
     * This method is used to obtain a Connection object to perform operations on the data source.
     * This interface assumes that this method may result in new connections having to be made,
     * hence why it throws a ConnectionFailedException.
     *
     * @return an object with the connection information and methods
     * @throws ConnectionFailedException occurs when connecting to the database fails
     */
    public Connection getConnection() throws ConnectionFailedException;

    /**
     * This method is used to return a Connection object to the pool when a user is finished with it.
     *
     * @param connection the connection to add back to the pool
     */
    public void releaseConnection(Connection connection);

    /**
     * This method is used to close all Connection objects when the pool is no longer needed.
     */
    public void shutdown();
}
