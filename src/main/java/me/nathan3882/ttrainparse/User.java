package me.nathan3882.ttrainparse;

import me.nathan3882.data.SqlConnection;
import me.nathan3882.data.SqlQuery;
import me.nathan3882.data.SqlUpdate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class User {

    public static final long DEFAULT_RENEW_COOLDOWN_DAYS = 7;
    private final TTrainParser main;
    private final SqlConnection connection;
    private String userEmail;

    public User(TTrainParser main, String userEmail) {
        this.main = main;
        this.userEmail = userEmail;
        this.connection = main.getSqlConnection();
    }

    public boolean hasSqlEntry(String table) {
        if (!hasInternet() || !connection.connectionEstablished()) return false;
        SqlQuery query = new SqlQuery(connection);
        query.executeQuery("SELECT * FROM {table} WHERE userEmail = '" + getUserEmail() + "'", table);
        boolean has = query.next(false);

        return has;
    }

    public String getUserEmail() {
        return this.userEmail;
    }

    public long getPreviousUploadTime() {
        SqlQuery query = new SqlQuery(connection);
        String str = "SELECT lastRenewMillis FROM {table} WHERE userEmail = '" + getUserEmail() + "'";
        ResultSet result = query.getResultSet(str,
                SqlConnection.SqlTableName.TIMETABLE_RENEWAL);
        long previousUploadTime = 0L;
        try {
            System.out.println(result.next());
            previousUploadTime = result.getLong(1);
            result.close();
        } catch (SQLException e) {
            TTrainParser.getDebugManager().handle(e);
            e.printStackTrace();
        }
        return previousUploadTime;
    }

    public void setTableUpdatesLeft(int number) {
        SqlUpdate update = new SqlUpdate(connection);
        update.executeUpdate("UPDATE {table} SET renewsLeft = '" + number + "' WHERE userEmail = '" + getUserEmail() + "'",
                SqlConnection.SqlTableName.TIMETABLE_RENEWAL);
    }

    public int getTableUpdatesLeft() {
        SqlQuery query = new SqlQuery(connection);

        ResultSet result = query.getResultSet("SELECT renewsLeft FROM {table} WHERE userEmail = '" + getUserEmail() + "'",
                SqlConnection.SqlTableName.TIMETABLE_RENEWAL);

        query.next(false);
        int left = query.getInt(1);

        query.close();
        return left;
    }


    public List<LessonInfo> getLessonInformation(DayOfWeek[] showThese) {
        List<LessonInfo> info = new LinkedList<>();
        connection.openConnection();

        SqlQuery query = new SqlQuery(connection);

        for (DayOfWeek day : showThese) {

            String dayName = day.name();

            query.executeQuery("SELECT " + dayName + " FROM {table} WHERE userEmail = '" + getUserEmail() + "'",
                    SqlConnection.SqlTableName.TIMETABLE_LESSONS);

            String depletedOcrText = "";
            if (query.next(false)) {
                depletedOcrText = query.getString(dayName);
            }

            List<String> words = new LinkedList<>(Arrays.asList(depletedOcrText.split(" ")));
//            for (String word : words) {
//                System.out.print(word + " ");
//            }
            info.add(new LessonInfo(words, day));
        }
        return info;
    }


    public void setPreviousUploadTime(long currentTimeMillis) {
        SqlUpdate update = new SqlUpdate(connection);
        update.executeUpdate("UPDATE {table} SET lastRenewMillis = '" + currentTimeMillis + "' WHERE userEmail = '" + getUserEmail() + "'",
                SqlConnection.SqlTableName.TIMETABLE_RENEWAL);
    }

    public void removeEntryFromTable(String table) {
        SqlUpdate update = new SqlUpdate(connection);

        String updateStr = "DELETE FROM {table} WHERE userEmail = '" + getUserEmail() + "'";
        System.out.println(updateStr);
        update.executeUpdate(updateStr, table);
    }


    public void generateDefaultRenewValues() {
        SqlUpdate defaultValues = new SqlUpdate(connection);
        defaultValues.executeUpdate(
                "INSERT INTO {table} (userEmail, renewsLeft, lastRenewMillis) VALUES ('" + getUserEmail() + "', 3, " + System.currentTimeMillis() + ")",
                SqlConnection.SqlTableName.TIMETABLE_RENEWAL);
    }

    public Date getTableRenewDate(boolean close) {
        Date date = new Date();
        date.setTime(getPreviousUploadTime() + TimeUnit.DAYS.toMillis(DEFAULT_RENEW_COOLDOWN_DAYS));
        return date;
    }

    public boolean hasOcrTextStored(DayOfWeek day) {
        if (!hasInternet() || !connection.connectionEstablished() || !hasSqlEntry(SqlConnection.SqlTableName.TIMETABLE_LESSONS)) {
            return false;
        }
        boolean hasEntry = false;
        SqlQuery query = new SqlQuery(connection);
        query.executeQuery("SELECT " + day.name() + " FROM {table} WHERE userEmail = '" + getUserEmail() + "'",
                SqlConnection.SqlTableName.TIMETABLE_LESSONS);

        if (query.next(false)) {
            hasEntry = true;
        }
        return hasEntry;
    }

    public boolean hasOcrTextStored(DayOfWeek... days) {
        boolean hasEntries = true;
        for (DayOfWeek day : days) {
            if (hasOcrTextStored(day)) {
                continue;
            } else {
                hasEntries = false;
            }
        }
        return hasEntries;
    }

    public void storeOcrText(String ocrText, DayOfWeek day, boolean hasInternet) {
        if (hasInternet && connection.connectionEstablished()) {
            connection.openConnection();
            SqlUpdate storeUpdate = new SqlUpdate(connection);
            if (hasOcrTextStored(day)) {
                storeUpdate.executeUpdate("UPDATE {table} SET " + day.name() + " = \"" + ocrText + "\"" + " WHERE userEmail = \"" + getUserEmail() + "\"",
                        SqlConnection.SqlTableName.TIMETABLE_LESSONS);
            } else {
                storeUpdate.executeUpdate("INSERT INTO {table} (userEmail, " + day.name() + ") VALUES (\"" + getUserEmail() + "\", \"" + ocrText + "\")",
                        SqlConnection.SqlTableName.TIMETABLE_LESSONS);
            }
        }
    }

    public boolean hasInternet() {
        return main.hasInternet();
    }

    public void setEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getPasswordFromSql() throws UnsupportedOperationException {
        if (hasSqlEntry(SqlConnection.SqlTableName.TIMETABLE_USERDATA)) {
            SqlQuery query = new SqlQuery(main.getSqlConnection());
            query.executeQuery("SELECT password from {table} WHERE userEmail = '" + getUserEmail() + "'",
                    SqlConnection.SqlTableName.TIMETABLE_USERDATA);
            query.next(false);
            int one = query.getInt(1);
            System.out.println("pw = " + one);
            return String.valueOf(one);
        }
        return null;
    }

    public void storeEmailAndPassword(String email, String password) {
        SqlUpdate update = new SqlUpdate(main.getSqlConnection());
        update.executeUpdate("INSERT INTO {table} (userEmail, password) VALUES (\"" + getUserEmail() + "\", \"" + password + "\")",
                SqlConnection.SqlTableName.TIMETABLE_USERDATA);

    }
}
