package me.nathan3882.ttrainparse.data;

import me.nathan3882.ttrainparse.TTrainParser;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class SqlUpdate {

    private final Connection connection;
    private final TTrainParser tTrainParser;


    public SqlUpdate(SqlConnection sqlConnection) {
        this.tTrainParser = sqlConnection.getTTrainParser();
        this.connection = sqlConnection.getConnection();
        try {
            if (connection.isClosed()) sqlConnection.openConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * used for insert, delete and update
     * executeUpdate("INSERT INTO table (UserName) VALUES (5)");
     *
     * @return success or not
     */
    public boolean executeUpdate(String sql, String name) {
        if (tTrainParser.hasInternet() && tTrainParser.getSqlConnection().connectionEstablished()) {
            PreparedStatement preparedStatement;
            try {
                connection.setAutoCommit(true);
                preparedStatement = connection.prepareStatement(
                        sql.replace("{table}", name),
                        Statement.RETURN_GENERATED_KEYS);
                preparedStatement.executeUpdate();
                close(preparedStatement);
            } catch (SQLException e) {
                TTrainParser.getDebugManager().handle(e);
                e.printStackTrace();
                return false;
            }
        } else return false;
        return true;
    }

    public void close(AutoCloseable resource) {
        try {
            resource.close();
        } catch (Exception e) {
            TTrainParser.getDebugManager().handle(e);
            e.printStackTrace();
        }
    }
}
