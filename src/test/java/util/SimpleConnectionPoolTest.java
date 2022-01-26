package util;

import exceptions.ConnectionFailedException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class SimpleConnectionPoolTest {

    private static String url;
    private static String username;
    private static String password;

    @BeforeAll
    public static void init() {
        Properties props = new Properties();

        try {
            props.load(SimpleConnectionPoolTest.class.getClassLoader().getResourceAsStream("connection.properties"));
            url = "jdbc:postgresql://" + props.getProperty("endpoint") + "/postgres";
            username = props.getProperty("username");
            password = props.getProperty("password");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testConnectSuccess() {
        int connectionCount = 3;
        final SimpleConnectionPool pool = new SimpleConnectionPool(url, username, password, connectionCount);

        assertDoesNotThrow(pool::connect);
        pool.shutdown();
    }

    @Test
    public void testConnectFailure() {
        int connectionCount = 3;
        final SimpleConnectionPool pool = new SimpleConnectionPool("", username, password, connectionCount);

        assertThrows(ConnectionFailedException.class, pool::connect);
        pool.shutdown();
    }

    @Test
    public void getConnection() {
        int connectionCount = 3;
        final SimpleConnectionPool pool = new SimpleConnectionPool(url, username, password, connectionCount);
        try {
            pool.connect();

            Connection connection = pool.getConnection();

            assertEquals(pool.getConnectionCount(), 2);
            connection.close();
            pool.shutdown();
        } catch (ConnectionFailedException | SQLException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getConnectionWhenEmpty() {
        int connectionCount = 2;
        final SimpleConnectionPool pool = new SimpleConnectionPool(url, username, password, connectionCount);
        try {
            pool.connect();
            Connection conn1 = pool.getConnection();
            Connection conn2 = pool.getConnection();
            Connection conn3 = pool.getConnection();

            assertEquals(pool.getConnectionCount(), 1);
            conn1.close();
            conn2.close();
            conn3.close();
            pool.shutdown();

        } catch (ConnectionFailedException | SQLException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void releaseConnection() {
        int connectionCount = 2;
        final SimpleConnectionPool pool = new SimpleConnectionPool(url, username, password, connectionCount);
        try {
            pool.connect();
            Connection conn1 = pool.getConnection();

            assertEquals(pool.getConnectionCount(), 1);
            pool.releaseConnection(conn1);
            assertEquals(pool.getConnectionCount(), 2);
            pool.shutdown();
        } catch (ConnectionFailedException e) {
            e.printStackTrace();
        }
    }
}
