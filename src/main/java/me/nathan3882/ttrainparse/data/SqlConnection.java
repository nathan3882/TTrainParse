package me.nathan3882.ttrainparse.data;

import me.nathan3882.ttrainparse.TTrainParser;
import me.nathan3882.ttrainparse.TaskManager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

public class SqlConnection {

    private static long latestOpeningMilis;
    private TTrainParser main;
    private boolean open;
    private TaskManager closeConnectionTask;
    private String host = "localhost";
    private String databaseName = "userdata";
    private int port = 3306;
    private String username = "root";
    private String password = "";
    private Connection connection;

    public SqlConnection(TTrainParser main) {
        this.main = main;
        this.connection = newCon();
    }

    public SqlConnection(TTrainParser main, String host, int port, String databaseName, String username, String password) {
        this.main = main;
        this.host = host;
        this.databaseName = databaseName;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    public static long getLatestOpeningMilis() {
        return latestOpeningMilis;
    }

    public static void setLatestOpeningMilis(long latestOpeningMilis) {
        SqlConnection.latestOpeningMilis = latestOpeningMilis;
    }

    /**
     * executeQuery("SELECT * FROM users WHERE databaseName = 'Nathan'");
     *
     * @return success or not
     */
    public void openConnection() {
        if (!connectionEstablished()) {
            newCon();
        }
        if (connectionEstablished()) {
            if (closeConnectionTask == null) {
                closeConnectionTask = new TaskManager(new Timer()) {
                    @Override
                    public void run() {
                        long current = System.currentTimeMillis();
                        long lastOpen = SqlConnection.getLatestOpeningMilis();
                        if (connectionEstablished() && isOpen() &&
                                current - lastOpen >= TimeUnit.SECONDS.toMillis(150)) { //Close after 2 and 1/2 minutes
                            closeConnection();
                            setLatestOpeningMilis(current); //Will be 0, condition will fail unless has been opened again
                        }
                    }
                };
                closeConnectionTask.runTaskSynchronously(closeConnectionTask, 5000L, 5000L);
            }
            try {
                latestOpeningMilis = System.currentTimeMillis();
                if (open) return;
                Class.forName("com.mysql.jdbc.Driver");
                this.connection = newCon();
                open = true;
            } catch (Exception e) {
                TTrainParser.getDebugManager().handle(e);
                e.printStackTrace();
            }
        }
    }

    private Connection newCon() {
        Connection ans;
        try {
            ans = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + databaseName + "?useSSL=false&allowMultiQueries=true", username, password);
            setLatestOpeningMilis(System.currentTimeMillis());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return ans;
    }

    public void closeConnection() {
        if (open) {
            close(connection);
            open = false;
        }
    }

    public boolean connectionEstablished() {
        if (this.connection == null) openConnection();
        return this.connection != null;
    }

    public Connection getConnection() {
        return connection;
    }

    private void close(AutoCloseable resource) {
        try {
            resource.close();
        } catch (Exception e) {
            TTrainParser.getDebugManager().handle(e);
            e.printStackTrace();
        }
    }

    public TTrainParser getTTrainParser() {
        return this.main;
    }

    public boolean isClosed() {
        try {
            return this.connection.isClosed();
        } catch (SQLException e) {
            TTrainParser.getDebugManager().handle(e);
            e.printStackTrace();
        }
        return false;
    }

    public boolean isOpen() {
        return !isClosed();
    }

    public interface SqlTableName {
        String TIMETABLE_RENEWAL = "timetablerenewal";
        String TIMETABLE_LESSONS = "timetablelessons";
        String TIMETABLE_USERDATA = "timetableuserdata";
    }
}
