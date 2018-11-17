package me.nathan3882.ttrainparse;

import java.util.Timer;
import java.util.TimerTask;

public class TaskManager extends TimerTask {

    private Timer timer;

    public TaskManager(Timer timer) {
        setTimer(timer);
    }

    @Override
    public void run() {
    }

    public void runTaskSynchronously(TimerTask task, long delay, long period) {
        timer.scheduleAtFixedRate(task, delay, period);
    }

    public Timer setTimer(Timer timer) {
        this.timer = timer;
        return timer;
    }

    public void terminate() {
        timer.cancel();
    }
}
