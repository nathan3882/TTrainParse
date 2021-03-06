package me.nathan3882.ttrainparse;

import me.nathan3882.ttrainparse.data.SqlConnection;
import me.nathan3882.ttrainparse.data.SqlQuery;
import me.nathan3882.ttrainparse.data.SqlUpdate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class User {

    public static final long DEFAULT_RENEW_COOLDOWN_DAYS = 7;
    private final TTrainParser tTrainParser;
    private final SqlConnection connection;
    private String userEmail;

    public User(TTrainParser tTrainParser, String userEmail) {
        this.tTrainParser = tTrainParser;
        this.userEmail = userEmail;
        this.connection = tTrainParser.getSqlConnection();
    }

    public static boolean hasSqlEntry(TTrainParser tTrainParser, String table, String userEmail) {
        SqlConnection connection = tTrainParser.getSqlConnection();
        if (!tTrainParser.hasInternet() || !connection.connectionEstablished()) return false;
        SqlQuery query = new SqlQuery(connection);
        query.executeQuery("SELECT * FROM {table} WHERE userEmail = '" + userEmail + "'", table);
        boolean has = query.next(false);
        return has;
    }

    public boolean hasSqlEntry(String table, String column) {
        return hasSqlEntry(tTrainParser, table, column);
    }

    public boolean hasSqlEntry(String table) {
        return hasSqlEntry(tTrainParser, table, getUserEmail());
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

    public void setPreviousUploadTime(long currentTimeMillis) {
        SqlUpdate update = new SqlUpdate(connection);
        update.executeUpdate("UPDATE {table} SET lastRenewMillis = '" + currentTimeMillis + "' WHERE userEmail = '" + getUserEmail() + "'",
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

    public void setTableUpdatesLeft(int number) {
        SqlUpdate update = new SqlUpdate(connection);
        update.executeUpdate("UPDATE {table} SET renewsLeft = '" + number + "' WHERE userEmail = '" + getUserEmail() + "'",
                SqlConnection.SqlTableName.TIMETABLE_RENEWAL);
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
            info.add(new LessonInfo(words, day));
        }
        return info;
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
                "INSERT INTO {table} (userEmail, renewsLeft, lastRenewMillis) VALUES ('" + getUserEmail() + "', " + TTrainParser.DEFAULT_FORCE_UPDATE_QUANTITY + ", " + System.currentTimeMillis() + ")",
                SqlConnection.SqlTableName.TIMETABLE_RENEWAL);
    }

    public Date getTableRenewDate() {
        Date date = new Date();
        date.setTime(getPreviousUploadTime() + TimeUnit.DAYS.toMillis(DEFAULT_RENEW_COOLDOWN_DAYS));
        return date;
    }

    public boolean hasOcrTextStored(DayOfWeek day) {
        if (!hasInternet() || !connection.connectionEstablished() || !hasSqlEntry(SqlConnection.SqlTableName.TIMETABLE_LESSONS)) {
            return false;
        }
        SqlQuery query = new SqlQuery(connection);
        query.executeQuery("SELECT " + day.name() + " FROM {table} WHERE userEmail = '" + getUserEmail() + "'",
                SqlConnection.SqlTableName.TIMETABLE_LESSONS);
        boolean next = query.next(false);
        ResultSet rs = query.getResultSet();
        try {
            String str = rs.getString(day.name());
            if (!rs.wasNull() && str != null && next) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
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
            String table = SqlConnection.SqlTableName.TIMETABLE_LESSONS;
            SqlUpdate storeUpdate = new SqlUpdate(connection);
            String dayName = day.name();
            if (hasSqlEntry(table)) {
                if (!hasOcrTextStored(day)) {
                    storeUpdate.executeUpdate("UPDATE {table} SET " + dayName + " = \"" + ocrText + "\"" + " WHERE userEmail = \"" + getUserEmail() + "\"",
                            table);
                }
            } else {
                storeUpdate.executeUpdate("INSERT INTO {table} (userEmail, " + dayName + ") VALUES (\"" + getUserEmail() + "\", \"" + ocrText + "\") ON DUPLICATE KEY UPDATE " + dayName + "='" + ocrText + "'",
                        table);
            }
        }
    }

    public boolean hasInternet() {
        return tTrainParser.hasInternet();
    }

    public void setEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getDatabaseStoredPwBytes(String email) {
        SqlQuery query = new SqlQuery(connection);
        query.executeQuery("SELECT password FROM {table} WHERE userEmail = '" + email + "'",
                SqlConnection.SqlTableName.TIMETABLE_USERDATA);
        return getString(query);
    }

    public void storeEmailAndPasswordWithCrs(String email, byte[] password, byte[] salt, String CRS) {
        SqlUpdate update = new SqlUpdate(connection);
        String valPw = Base64.getEncoder().encodeToString(password);
        String valSalt = Base64.getEncoder().encodeToString(salt);
        update.executeUpdate("INSERT INTO {table} (userEmail, password, salt, homeCrs) VALUES (\"" + email + "\", \"" + valPw + "\", \"" + valSalt + "\", \"" + CRS + "\")",
                SqlConnection.SqlTableName.TIMETABLE_USERDATA);
    }

    public String getDatabaseSalt(String email) {
        SqlQuery query = new SqlQuery(connection);
        query.executeQuery("SELECT salt FROM {table} WHERE userEmail = '" + email + "'",
                SqlConnection.SqlTableName.TIMETABLE_USERDATA);
        return getString(query);
    }

    public boolean hasEmailPwAndHomeData() {
        return hasSqlEntry(SqlConnection.SqlTableName.TIMETABLE_USERDATA);
    }

    public void updateHomeCrs(String crs) {
        SqlUpdate update = new SqlUpdate(connection);
        update.executeUpdate("UPDATE {table} SET homeCrs='" + crs + "' WHERE userEmail='" + getUserEmail() + "'",
                SqlConnection.SqlTableName.TIMETABLE_USERDATA);
    }

    public String getHomeCrs() {
        SqlQuery query = new SqlQuery(connection);
        query.executeQuery("SELECT homeCrs FROM {table} WHERE userEmail = '" + getUserEmail() + "'",
                SqlConnection.SqlTableName.TIMETABLE_USERDATA);
        ResultSet set = query.getResultSet();
        try {
            if (set.next()) {
                return set.getString(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getString(SqlQuery query) {
        ResultSet set = query.getResultSet();
        try {
            if (!set.next()) return "invalid email";
            return set.getString(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


}