package me.nathan3882.data;

import java.sql.*;

public class MySQLFactory {

    private String host = "localhost";
    private String name = "test";
    private int port = 3306;
    private String username = "root";
    private String password = "";

    private Connection connection;

    public MySQLFactory() {
    }

    public MySQLFactory(String host, int port, String name, String username, String password) {
        this.host = host;
        this.name = name;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    /**
     * executeQuery("SELECT * FROM users WHERE name = 'Nathan'");
     *
     * @return success or not
     */
    public void openConnection() {
        if (isConnected()) {
            return;
        }
        try {
            Class.forName("com.mysql.jdbc.Driver");
            this.connection = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + name + "?allowMultiQueries=true", username, password);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isConnected() {
        try {
            return !connection.isClosed();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void closeConnection() {
        if (isConnected()) {
            close(connection);
        }
    }

    public ResultSet executeQuery(String sql) {
        Connection connection = getConnection();
        ResultSet resultSet = null;
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(sql);
            resultSet = preparedStatement.executeQuery();

            close(resultSet);
            close(preparedStatement);
        } catch (SQLException e) {
            return null;
        }
        return resultSet;
    }

    /**
     * used for insert, delete and update
     * executeUpdate("INSERT INTO table (UserName) VALUES (5)");
     * @return success or not
     */
    private boolean executeUpdate(String sql) {
        PreparedStatement preparedStatement = null;
        try {
            connection.setAutoCommit(true);
            preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.executeUpdate();

            close(preparedStatement);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private Connection getConnection() {
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