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
    private TTrainParser tTrainParser;
    private boolean open;
    private TaskManager closeConnectionTask;

    private String host = "51.77.194.49";
    private String databaseName = "ttrainparseUserdata";
    private int port = 3307;
    private String username = "nathan";
    private String password = "PaSs123";
    private Connection connection;

    public SqlConnection(TTrainParser tTrainParser) {
        this.tTrainParser = tTrainParser;
        establishConnection();
    }

    public SqlConnection(TTrainParser tTrainParser, String host, int port, String databaseName, String username, String password) {
        this.tTrainParser = tTrainParser;
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
        if (!hasConnection() || isClosed()) {
            establishConnection();
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
            latestOpeningMilis = System.currentTimeMillis();
            if (!open) {
                establishConnection();
            }

        }
    }

    public void closeConnection() {
        if (open) {
            close(connection);
            open = false;
        }
    }

    public boolean hasConnection() {
        return this.connection != null;
    }

    public boolean connectionEstablished() {
        if (!hasConnection()) {
            establishConnection();
        }
        return hasConnection() && !isClosed();
    }

    public Connection getConnection() {
        return connection;
    }

    public TTrainParser getTTrainParser() {
        return this.tTrainParser;
    }

    public boolean isClosed() {
        try {
            return this.connection.isClosed();
        } catch (SQLException e) {
            TTrainParser.getDebugManager().handle(e);
            e.printStackTrace();
            return true;
        }
    }

    public boolean isOpen() {
        return !isClosed();
    }

    private void establishConnection() {
        Connection ans = null;
        try {
            ans = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + databaseName + "?useSSL=false&allowMultiQueries=true", username, password);
            setLatestOpeningMilis(System.currentTimeMillis());
            open = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (ans == null) open = false;
        this.connection = ans;
    }

    private void close(AutoCloseable resource) {
        try {
            resource.close();
        } catch (Exception e) {
            TTrainParser.getDebugManager().handle(e);
            e.printStackTrace();
        }
    }

    public interface SqlTableName {
        String TIMETABLE_RENEWAL = "timetablerenewal";
        String TRAINS = "trains";
        String TIMETABLE_LESSONS = "timetablelessons";
        String TIMETABLE_USERDATA = "timetableuserdata";
    }
}
