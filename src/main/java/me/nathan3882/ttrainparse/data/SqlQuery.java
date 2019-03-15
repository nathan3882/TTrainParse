package me.nathan3882.ttrainparse.data;

import me.nathan3882.ttrainparse.TTrainParser;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SqlQuery {

    private final TTrainParser main;
    private final Connection connection;
    private String host = "localhost";
    private String databaseName = "userdata";
    private int port = 3306;
    private String username = "root";
    private String password = "";
    private PreparedStatement preparedStatement;
    private ResultSet resultSet = null;

    public SqlQuery(SqlConnection cction) {
        this.main = cction.getTTrainParser();
        this.connection = cction.getConnection();
        try {
            if (connection.isClosed()) cction.openConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public ResultSet executeQuery(String sql, String name) {
        if (main.hasInternet() && main.getSqlConnection().connectionEstablished()) {
            if (resultSet != null) {
                close();
            }
            try {
                this.preparedStatement = connection.prepareStatement(
                        sql.replace("{table}", name));

                this.resultSet = preparedStatement.executeQuery();
            } catch (SQLException e) {
                TTrainParser.getDebugManager().handle(e);
                e.printStackTrace();
            }
        }
        return this.resultSet;
    }


    public ResultSet getResultSet() {
        return this.resultSet;
    }

    public ResultSet getResultSet(String sql, String tableName) {
        executeQuery(sql, tableName);
        return getResultSet();
    }

    public boolean next(boolean close) {
        boolean next = false;
        try {
            next = resultSet.next();
            if (close) connection.close();
        } catch (SQLException e) {
            TTrainParser.getDebugManager().handle(e);
            e.printStackTrace();
        }
        return next;
    }

    public String getString(String columnName) {
        try {
            return resultSet.getString(columnName);
        } catch (SQLException e) {
            TTrainParser.getDebugManager().handle(e);
            e.printStackTrace();
        }
        return null;
    }

    public String getString(int column) {
        try {
            return resultSet.getString(column);
        } catch (SQLException e) {
            TTrainParser.getDebugManager().handle(e);
            e.printStackTrace();
        }
        return null;
    }

    public void close() {
        try {
            resultSet.close();
        } catch (SQLException e) {
            TTrainParser.getDebugManager().handle(e);
            e.printStackTrace();
        }
    }

    public int getInt(int i) {
        try {
            return resultSet.getInt(i);
        } catch (SQLException e) {
            TTrainParser.getDebugManager().handle(e);
            e.printStackTrace();
        }
        return -1;
    }
}