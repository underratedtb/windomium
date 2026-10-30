package org.windomium.api;

public enum RiskLevel {
    SAFE("Safe"),
    MODERATE("Moderate"),
    EXPERIMENTAL("Experimental");

    private final String displayName;

    RiskLevel(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return this.displayName;
    }
}