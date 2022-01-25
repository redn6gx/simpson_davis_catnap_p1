package util;

import exceptions.ConnectionFailedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.LinkedList;

/**
 * This class is used to obtain free connections to the database in order to
 * perform CRUD operations. The pool employs a simple strategy of using a linked list
 * of connection objects to manage the pool. If the pool is empty when a user requests
 * a connection, it expands by the baseConnections parameter.
 */
public class SimpleConnectionPool implements ConnectionPool {

    private final String databaseUrl;
    private final String username;
    private final String password;
    private final int baseConnections;

    private final static Logger logger = LogManager.getLogger(SimpleConnectionPool.class);

    private final LinkedList<Connection> freeConnections = new LinkedList<>();

    /**
     * Constructor for SimpleConnectionPool. The object maintains a number of connections
     * that are multiples of baseConnections.
     *
     * @param databaseUrl        the url to the database url
     * @param username           username to access the database
     * @param password           password to access the database
     * @param baseConnections    the base number of connections to maintain
     */
    public SimpleConnectionPool(String databaseUrl, String username, String password, int baseConnections) {
        this.databaseUrl = databaseUrl;
        this.username = username;
        this.password = password;
        this.baseConnections = baseConnections;
    }

    /**
     * This method is used to connect to a data source. Implementing classes
     * will need to manage the required parameters for connecting to their data source. Here we use the
     * JDBC DriverManager class to make a connection.
     *
     * @throws ConnectionFailedException occurs when connecting to the database fails
     */
    public void connect() throws ConnectionFailedException {
        try {
            for (int i = 0; i < this.baseConnections; i++) {
                freeConnections.add(DriverManager.getConnection(this.databaseUrl, this.username, this.password));
            }
        } catch (SQLException e) {
            logger.error("SQL Exception when trying to connect to database, got the following message: " + e.getMessage());

            throw new ConnectionFailedException();
        }
    }

    /**
     * This method is used to obtain a Connection object to perform operations on the data source.
     * If the user requests a connection when the pool is empty we create baseConnections number
     * of new connections and add them to the pool.
     *
     * @return an object with the connection information and methods
     * @throws ConnectionFailedException occurs when connecting to the database fails
     */
    public Connection getConnection() throws ConnectionFailedException {
        if(freeConnections.size() == 0) {
            connect();
        }

        Connection connection = freeConnections.getFirst();
        freeConnections.remove(connection);
        return connection;
    }

    /**
     * This method is used to return a Connection object to the pool when a user is finished with it.
     *
     * @param connection the connection to add back to the pool
     */
    public void releaseConnection(Connection connection) {
        freeConnections.add(connection);
    }

    /**
     * This method is used to close all Connection objects when the pool is no longer needed.
     */
    public void shutdown() {
        for (Connection conn: freeConnections) {
            try {
                conn.close();
            } catch (SQLException e) {
                logger.error("SQL Exception trying to shutdown the connection pool, got message: " + e.getMessage());
            }
        }
    }

    public int getConnectionCount() {
        return this.freeConnections.size();
    }
}
