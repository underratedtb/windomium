package org.windomium.api;

public interface IOptimisationModule {
    String getId();

    String getDisplayName();

    OptimisationCategory getCategory();

    RiskLevel getRiskLevel();

    boolean guaranteesVisualParity();

    boolean isClientOnly();

    void onInitialise();

    void onEnable();

    void onDisable();
}