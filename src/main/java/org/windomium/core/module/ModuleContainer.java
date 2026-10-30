package org.windomium.core.module;

import org.windomium.api.IOptimisationModule;

public final class ModuleContainer {
    private final IOptimisationModule module;
    private boolean enabled;
    private boolean faulted;
    private Throwable lastError;

    public ModuleContainer(IOptimisationModule module) {
        this.module = module;
        this.enabled = false;
        this.faulted = false;
    }

    public IOptimisationModule getModule() {
        return this.module;
    }

    public boolean isEnabled() {
        return this.enabled && !this.faulted;
    }

    public boolean isFaulted() {
        return this.faulted;
    }

    public Throwable getLastError() {
        return this.lastError;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void markFaulted(Throwable error) {
        this.faulted = true;
        this.enabled = false;
        this.lastError = error;
    }
}