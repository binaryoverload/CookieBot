package io.github.binaryoverload.scheduler;

import io.github.binaryoverload.CookieBot;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Scheduler {

    private static final ScheduledExecutorService timer = Executors
            .newScheduledThreadPool(10, r -> new Thread(r, "CookieBot Scheduled Task"));

    private static final Map<String, ScheduledFuture<?>> tasks = new HashMap<>();
    private static final Map<String, ScheduledFuture<?>> persistentTasks = new HashMap<>();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(timer::shutdownNow));
    }

    public static boolean scheduleRepeating(Runnable task, String taskName, long delay, long interval) {
        if (tasks.containsKey(taskName)) {
            return false;
        }
        tasks.put(taskName,
                timer.scheduleAtFixedRate(() -> {
                    try {
                        task.run();
                    } catch (Exception e) {
                        CookieBot.LOGGER.error("Error in " + taskName + " task scheduler!", e);
                    }
                }, delay, interval, TimeUnit.MILLISECONDS));
        return true;
    }

    public static void delayTask(Runnable task, String taskName, long delay) {
        tasks.put(taskName, timer.schedule(task, delay, TimeUnit.MILLISECONDS));
    }

    public static boolean cancelTask(String taskName) {
        Iterator<Map.Entry<String, ScheduledFuture<?>>> i = tasks.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry<String, ScheduledFuture<?>> next = i.next();
            if (next.getKey() == null) continue;
            if (next.getKey().equals(taskName)) {
                next.getValue().cancel(false);
                i.remove();
                return true;
            }
        }
        return false;
    }

    public static Map<String, ScheduledFuture<?>> getTasks() {
        return tasks;
    }

    public static Map<String, ScheduledFuture<?>> getPersistentTasks() {
        return persistentTasks;
    }

}
