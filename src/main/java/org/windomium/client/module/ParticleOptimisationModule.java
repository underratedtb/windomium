package org.windomium.client.module;

import org.windomium.api.IOptimisationModule;
import org.windomium.api.OptimisationCategory;
import org.windomium.api.RiskLevel;

public final class ParticleOptimisationModule implements IOptimisationModule {
    @Override
    public String getId() {
        return "particle_optimiser";
    }

    @Override
    public String getDisplayName() {
        return "Particle Distance & Budget Limiter";
    }

    @Override
    public OptimisationCategory getCategory() {
        return OptimisationCategory.PARTICLE_AND_UI;
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