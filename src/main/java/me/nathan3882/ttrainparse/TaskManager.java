package me.nathan3882.ttrainparse;

import java.util.Timer;
import java.util.TimerTask;

public class TaskManager extends TimerTask {

    private Timer timer;

    public TaskManager(Timer timer) {
        setTimer(timer);
    }

    public void runTaskSynchronously(TimerTask task, long delay, long period) {
        timer.scheduleAtFixedRate(task, delay, period);
    }

    @Override
    public void run() {
    }

    private Timer setTimer(Timer timer) {
        this.timer = timer;
        return timer;
    }

    protected void terminate() {
        timer.cancel();
    }
}
