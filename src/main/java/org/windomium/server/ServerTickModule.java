package org.windomium.server;

import org.windomium.api.IOptimisationModule;
import org.windomium.api.OptimisationCategory;
import org.windomium.api.RiskLevel;

public final class ServerTickModule implements IOptimisationModule {
    @Override
    public String getId() {
        return "server_tick_optimiser";
    }

    @Override
    public String getDisplayName() {
        return "Server AI & Distant Mob Tick Throttler";
    }

    @Override
    public OptimisationCategory getCategory() {
        return OptimisationCategory.SERVER;
    }

    @Override
    public RiskLevel getRiskLevel() {
        return RiskLevel.MODERATE;
    }

    @Override
    public boolean guaranteesVisualParity() {
        return true;
    }

    @Override
    public boolean isClientOnly() {
        return false;
    }

    @Override
    public void onInitialise() {
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }
}