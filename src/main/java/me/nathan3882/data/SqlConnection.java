package me.nathan3882.data;

import java.sql.Connection;
import java.sql.DriverManager;

public class SqlConnection {

    public enum SqlTableName {
        TIMETABLE_RENEWAL {
            @Override
            public String toString() {
                return "timetablerenewal";
            }
        }
    }

    private String host = "localhost";
    private String databaseName = "userdata";
    private int port = 3306;
    private String username = "root";
    private String password = "";

    private Connection connection;

    public SqlConnection() {
    }

    public SqlConnection(String host, int port, String databaseName, String username, String password) {
        this.host = host;
        this.databaseName = databaseName;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    /**
     * executeQuery("SELECT * FROM users WHERE databaseName = 'Nathan'");
     *
     * @return success or not
     */
    public void openConnection() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            this.connection = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + databaseName + "?allowMultiQueries=true", username, password);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void closeConnection() {
        close(connection);
    }

    public Connection getConnection() {
        return connection;
    }

    private void close(AutoCloseable resource) {
        try {
            resource.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
