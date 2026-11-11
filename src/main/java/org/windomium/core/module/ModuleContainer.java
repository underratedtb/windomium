package org.windomium.core.module;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.windomium.api.IOptimisationModule;

public final class ModuleContainer {
    private static final int MAX_CONSECUTIVE_FAILURES = 10;
    private static final int MAX_WINDOW_FAILURES = 20;
    private static final long FAILURE_WINDOW_NS = 5_000_000_000L;

    private final IOptimisationModule module;
    private final AtomicBoolean enabled = new AtomicBoolean(false);
    private final AtomicBoolean faulted = new AtomicBoolean(false);
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicInteger windowFailures = new AtomicInteger(0);
    private final AtomicLong windowStartNs = new AtomicLong(0L);
    private volatile Throwable lastError;

    public ModuleContainer(IOptimisationModule module) {
        this.module = module;
    }

    public IOptimisationModule getModule() {
        return this.module;
    }

    public boolean isEnabled() {
        return this.enabled.get() && !this.faulted.get();
    }

    public boolean isFaulted() {
        return this.faulted.get();
    }

    public Throwable getLastError() {
        return this.lastError;
    }

    public int getConsecutiveFailures() {
        return this.consecutiveFailures.get();
    }

    public void setEnabled(boolean enable) {
        this.enabled.set(enable);
    }

    public void recordSuccess() {
        if (this.consecutiveFailures.get() != 0) {
            this.consecutiveFailures.set(0);
        }
    }

    public boolean recordFailure(Throwable error) {
        this.lastError = error;
        int consecutive = this.consecutiveFailures.incrementAndGet();

        long now = System.nanoTime();
        long start = this.windowStartNs.get();
        if (now - start > FAILURE_WINDOW_NS && this.windowStartNs.compareAndSet(start, now)) {
            this.windowFailures.set(0);
        }
        int inWindow = this.windowFailures.incrementAndGet();

        if (consecutive >= MAX_CONSECUTIVE_FAILURES || inWindow >= MAX_WINDOW_FAILURES) {
            return tripBreaker(error);
        }
        return false;
    }

    public boolean tripBreaker(Throwable error) {
        this.lastError = error;
        this.enabled.set(false);
        return this.faulted.compareAndSet(false, true);
    }

    public void markFaulted(Throwable error) {
        tripBreaker(error);
    }

    public void resetFaultState() {
        this.faulted.set(false);
        this.consecutiveFailures.set(0);
        this.windowFailures.set(0);
        this.windowStartNs.set(0L);
        this.lastError = null;
    }
}