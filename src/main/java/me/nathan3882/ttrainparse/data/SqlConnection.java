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

    private String host = "51.77.194.49";
    private String databaseName = "ttrainparseUserdata";
    private int port = 3307;
    private String username = "";
    private String password = "";
    private Connection connection;

    public SqlConnection(TTrainParser main) {
        this.main = main;
        establishConnection();
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
        if (!hasConnection() || isClosed()) {
            establishConnection();
        }
        if (connectionEstablished()) {
            System.out.println("con est");
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
                establishConnection();
                open = true;
            } catch (Exception e) {
                TTrainParser.getDebugManager().handle(e);
                e.printStackTrace();
            }
        } else {
            System.out.println("con not est");
        }
    }

    private Connection establishConnection() {
        Connection ans;
        try {
            ans = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + databaseName + "?useSSL=false&allowMultiQueries=true", username, password);
            setLatestOpeningMilis(System.currentTimeMillis());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        this.connection = ans;
        return ans;
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
