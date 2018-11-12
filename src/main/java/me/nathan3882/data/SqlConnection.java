package me.nathan3882.data;

import me.nathan3882.ttrainparse.TTrainParser;

import java.sql.Connection;
import java.sql.DriverManager;

public class SqlConnection {

    private TTrainParser main;
    private boolean open;

    public TTrainParser getTTrainParser() {
        return this.main;
    }

    public interface SqlTableName {
        String TIMETABLE_RENEWAL = "timetablerenewal";
        String TIMETABLE_LESSONS = "timetablelessons";
    }

    private String host = "localhost";
    private String databaseName = "userdata";
    private int port = 3306;
    private String username = "root";
    private String password = "";

    private Connection connection;

    public SqlConnection(TTrainParser main) {
        this.main = main;
    }

    public SqlConnection(TTrainParser main, String host, int port, String databaseName, String username, String password) {
        this.main = main;
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
        if (open) return;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            this.connection = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + databaseName + "?allowMultiQueries=true", username, password);
            open = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void closeConnection() {
        if (open) {
            close(connection);
            open = false;
        }
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
