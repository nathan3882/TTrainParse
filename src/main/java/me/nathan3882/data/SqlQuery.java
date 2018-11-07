package me.nathan3882.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SqlQuery {

    private String host = "localhost";
    private String databaseName = "userdata";
    private int port = 3306;
    private String username = "root";
    private String password = "";

    private final Connection connection;
    private PreparedStatement preparedStatement;
    private ResultSet resultSet;

    public SqlQuery(SqlConnection cction) {
        this.connection = cction.getConnection();
    }

    public SqlQuery executeQuery(String sql, SqlConnection.SqlTableName name) {
        try {
            this.preparedStatement = connection.prepareStatement(
                    sql.replace("{table}", name.toString()));

            this.resultSet = preparedStatement.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return this;
    }

    public ResultSet getResultSet() {
        return this.resultSet;
    }

    public ResultSet getResultSet(String sql, SqlConnection.SqlTableName tableName) {
        executeQuery(sql, tableName);
        return getResultSet();
    }
}