package org.windomium.client.module;

import org.windomium.api.IOptimisationModule;
import org.windomium.api.OptimisationCategory;
import org.windomium.api.RiskLevel;

public final class EntityOptimisationModule implements IOptimisationModule {
    @Override
    public String getId() {
        return "entity_optimiser";
    }

    @Override
    public String getDisplayName() {
        return "Entity Layer & Shadow Optimiser";
    }

    @Override
    public OptimisationCategory getCategory() {
        return OptimisationCategory.ENTITY;
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
    }
}