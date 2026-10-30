package org.windomium.client.render;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class MesherThreadPool {
    private static final int DEFAULT_THREADS = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
    private static final PriorityBlockingQueue<Runnable> TASK_QUEUE = new PriorityBlockingQueue<>();
    private static ThreadPoolExecutor executor = createExecutor(DEFAULT_THREADS);

    private MesherThreadPool() {
    }

    private static ThreadPoolExecutor createExecutor(int threads) {
        AtomicInteger threadId = new AtomicInteger(0);
        return new ThreadPoolExecutor(
                threads,
                threads,
                60L,
                TimeUnit.SECONDS,
                TASK_QUEUE,
                task -> {
                    Thread thread = new Thread(task, "Windomium-Mesher-" + threadId.getAndIncrement());
                    thread.setPriority(Thread.NORM_PRIORITY - 1);
                    thread.setDaemon(true);
                    return thread;
                }
        );
    }

    public static synchronized void ensureActive(int threads) {
        int targetThreads = Math.max(1, threads);
        if (executor.isShutdown() || executor.isTerminated()) {
            TASK_QUEUE.clear();
            executor = createExecutor(targetThreads);
        } else if (executor.getCorePoolSize() != targetThreads) {
            executor.setCorePoolSize(targetThreads);
            executor.setMaximumPoolSize(targetThreads);
        }
    }

    public static void submit(RenderTask task) {
        if (!executor.isShutdown()) {
            executor.execute(task);
        }
    }

    public static synchronized void clearQueue() {
        TASK_QUEUE.clear();
    }

    public static int getPendingTaskCount() {
        return TASK_QUEUE.size();
    }
}