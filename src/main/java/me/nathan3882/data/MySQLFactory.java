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
    public void openConnection() throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.jdbc.Driver");
        this.connection = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + name + "?allowMultiQueries=true", username, password);
    }

    public boolean isConnected() throws SQLException {
        return !connection.isClosed();
    }

    public void closeConnection() throws SQLException {
        if (isConnected()) {
            this.connection.close();
        }
    }

    public boolean executeQuery(String sql) {
        Connection connection = getConnection();
        ResultSet resultSet = null;
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(sql);
            resultSet = preparedStatement.executeQuery();
        } catch (SQLException e) {
            return false;
        } finally {
            closeResource(resultSet);
            closeResource(preparedStatement);
        }
        return true;
    }

    /**
     * used for insert, delete and update
     * executeUpdate("INSERT INTO table (UserName) VALUES (5)");
     *
     * @return success or not
     */
    private void executeUpdate(String sql) {
        ResultSet resultSet = null;
        PreparedStatement preparedStatement = null;
        try {
            connection.setAutoCommit(true);
            preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeResource(resultSet);
            closeResource(preparedStatement);
        }
    }

    private Connection getConnection() {
        return connection;
    }

    private boolean closeResource(AutoCloseable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }
        return false;
    }
}