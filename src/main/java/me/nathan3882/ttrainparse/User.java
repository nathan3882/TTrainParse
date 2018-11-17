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
    private String userIp;
    private final TTrainParser main;
    private final SqlConnection connection;

    public User(TTrainParser main, String userIp) {
        this.main = main;
        this.userIp = userIp;
        this.connection = main.getSqlConnection();
    }

    //TODO What's a clean way to check if a record / row exists?
    public boolean hasSqlEntry(String table) {
        if (!hasInternet()) return false;
        SqlQuery query = new SqlQuery(main.getSqlConnection());
        query.executeQuery("SELECT * FROM {table} WHERE userIp = '" + getUserIp() + "'", table);
        return query.next(false);
    }


    public String getUserIp() {
        return this.userIp;
    }

    public long getPreviousUploadTime() {
        SqlQuery query = new SqlQuery(connection);
        String str = "SELECT lastRenewMillis FROM {table} WHERE userIp = '" + getUserIp() + "'";
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

            query.executeQuery("SELECT " + dayName + " FROM {table} WHERE userIp = '" + getUserIp() + "'",
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
        update.executeUpdate("UPDATE {table} SET lastRenewMillis = '" + currentTimeMillis + "' WHERE userIp = '" + getUserIp() + "'",
                SqlConnection.SqlTableName.TIMETABLE_RENEWAL);
    }


    public void generateDefaultRenewValues() {
        SqlUpdate defaultValues = new SqlUpdate(connection);
        defaultValues.executeUpdate(
                "INSERT INTO {table} (userIp, renewsLeft, lastRenewMillis) VALUES ('" + getUserIp() + "', 3, " + System.currentTimeMillis() + ")",
                SqlConnection.SqlTableName.TIMETABLE_RENEWAL);
    }

    public Date getTableRenewDate(boolean close) {
        Date date = new Date();
        date.setTime(getPreviousUploadTime() + TimeUnit.DAYS.toMillis(DEFAULT_RENEW_COOLDOWN_DAYS));
        return date;
    }

    public boolean hasOcrTextStored(DayOfWeek day) {
        if (!hasInternet() || !hasSqlEntry(SqlConnection.SqlTableName.TIMETABLE_LESSONS)) {
            System.out.println("no int");
            return false;
        }
        boolean hasEntry = false;
        SqlQuery query = new SqlQuery(connection);
        query.executeQuery("SELECT " + day.name() + " FROM {table} WHERE userIp = '" + getUserIp() + "'",
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
        if (hasInternet) {
            connection.openConnection();
            SqlUpdate storeUpdate = new SqlUpdate(connection);
            if (hasOcrTextStored(day)) {
                storeUpdate.executeUpdate("UPDATE {table} SET " + day.name() + " = \"" + ocrText + "\"" + " WHERE userIp = \"" + getUserIp() + "\"",
                        SqlConnection.SqlTableName.TIMETABLE_LESSONS);
            } else {
                storeUpdate.executeUpdate("INSERT INTO {table} (userIp, " + day.name() + ") VALUES (\"" + getUserIp() + "\", \"" + ocrText + "\")",
                        SqlConnection.SqlTableName.TIMETABLE_LESSONS);
            }
        }
    }

    public boolean hasInternet() {
        return main.hasInternet();
    }

    public void setIp(String fetchIp) {
        this.userIp = fetchIp;
    }

}
