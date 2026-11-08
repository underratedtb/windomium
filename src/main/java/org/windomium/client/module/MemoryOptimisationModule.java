package org.windomium.client.module;

import org.windomium.api.IOptimisationModule;
import org.windomium.api.OptimisationCategory;
import org.windomium.api.RiskLevel;
import org.windomium.core.memory.MemoryOptimiserController;

public final class MemoryOptimisationModule implements IOptimisationModule {
    @Override
    public String getId() {
        return "memory_optimiser";
    }

    @Override
    public String getDisplayName() {
        return "Fastutil Cache & Memory Optimiser";
    }

    @Override
    public OptimisationCategory getCategory() {
        return OptimisationCategory.MEMORY_AND_LOADING;
    }

    @Override
    public RiskLevel getRiskLevel() {
        return RiskLevel.SAFE;
    }

    @Override
    public boolean guaranteesVisualParity() {
        return true;
    }

    @Override
    public boolean isClientOnly() {
        return true;
    }

    @Override
    public void onInitialise() {
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
        MemoryOptimiserController.trimCaches();
    }
}