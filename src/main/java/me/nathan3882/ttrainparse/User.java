package me.nathan3882.ttrainparse;

import me.nathan3882.data.SqlConnection;
import me.nathan3882.data.SqlQuery;
import me.nathan3882.data.SqlUpdate;

import java.sql.ResultSet;
import java.sql.SQLException;

public class User {

    private final String userIp;
    private final TTrainParser main;
    private final SqlConnection connection;

    public User(TTrainParser main, String userIp) {
        this.main = main;
        this.userIp = userIp;
        this.connection = main.getSqlConnection();
    }

    //TODO What's a clean way to check if a record / row exists?
    public boolean hasSqlEntry() {
        SqlQuery query = new SqlQuery(main.getSqlConnection());
        query.executeQuery("SELECT renewsLeft FROM {table} WHERE userIp = '" + getUserIp() + "'",
                SqlConnection.SqlTableName.TIMETABLE_RENEWAL);
        ResultSet result = query.getResultSet();
        int renewsLeft = 0;
        try {
            result.next();
            renewsLeft = result.getInt(1);
        } catch (SQLException e) {
            return false;
        }
        return renewsLeft != 0;
    }

    private String getUserIp() {
        return this.userIp;
    }

    public long getPreviousUploadTime() {
        SqlQuery query = new SqlQuery(connection);
        String str = "SELECT lastRenewMillis FROM {table} WHERE userIp = '" + getUserIp() + "'";
        System.out.println(str);
        ResultSet result = query.getResultSet(str,
                SqlConnection.SqlTableName.TIMETABLE_RENEWAL);
        long previousUploadTime = 0L;
        try {
            System.out.println(result.next());
            previousUploadTime = result.getLong(1);
            result.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return previousUploadTime;
    }

    public void setTableUpdatesLeft(int number) {
        SqlUpdate update = new SqlUpdate(connection);
        update.executeUpdate("UPDATE {table} SET renewsLeft = '" + number + "' WHERE userIp = '" + getUserIp() + "'",
                SqlConnection.SqlTableName.TIMETABLE_RENEWAL);
    }

    public int getTableUpdatesLeft() {
        SqlQuery query = new SqlQuery(connection);

        ResultSet result = query.getResultSet("SELECT renewsLeft FROM {table} WHERE userIp = '" + getUserIp() + "'",
                SqlConnection.SqlTableName.TIMETABLE_RENEWAL);

        int left = 0;
        try {
            result.next(); //Assure ResultSet getInt is acted upon the first row
            left = result.getInt(1);
            result.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return left;
    }

    public void setPreviousUploadTime(long currentTimeMillis) {
        SqlUpdate update = new SqlUpdate(connection);
        update.executeUpdate("UPDATE {table} SET lastRenewMillis = '" + currentTimeMillis + "' WHERE userIp = '" + getUserIp() + "'",
                SqlConnection.SqlTableName.TIMETABLE_RENEWAL);
    }

    public void generateDefaultValues() {
        SqlUpdate defaultValues = new SqlUpdate(connection);
        defaultValues.executeUpdate(
                "INSERT INTO {table} (userIp, renewsLeft, lastRenewMillis) VALUES ('" + getUserIp() + "', 3, " + System.currentTimeMillis() + ")",
                SqlConnection.SqlTableName.TIMETABLE_RENEWAL);
    }
}
