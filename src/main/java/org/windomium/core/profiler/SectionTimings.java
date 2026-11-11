package org.windomium.core.profiler;

public final class SectionTimings {
    private long cpuFrameTimeNs;
    private long chunkMeshTimeNs;
    private long activeCullingTimeNs;
    private long lastCullingTimeNs;

    private int activeRenderedSections;
    private int activeCulledSections;
    private int lastRenderedSections;
    private int lastCulledSections;

    private int activeRenderedEntities;
    private int activeCulledEntities;
    private int lastRenderedEntities;
    private int lastCulledEntities;

    private int activeSkippedEntityLayers;
    private int lastSkippedEntityLayers;

    private int activeRenderedBlockEntities;
    private int activeCulledBlockEntities;
    private int lastRenderedBlockEntities;
    private int lastCulledBlockEntities;

    public void resetCounters() {
        int totalSectionsThisFrame = this.activeRenderedSections + this.activeCulledSections;
        if (totalSectionsThisFrame >= 64) {
            this.lastRenderedSections = this.activeRenderedSections;
            this.lastCulledSections = this.activeCulledSections;
            this.lastCullingTimeNs = this.activeCullingTimeNs;
        }
        this.lastRenderedEntities = this.activeRenderedEntities;
        this.lastCulledEntities = this.activeCulledEntities;
        this.lastSkippedEntityLayers = this.activeSkippedEntityLayers;
        this.lastRenderedBlockEntities = this.activeRenderedBlockEntities;
        this.lastCulledBlockEntities = this.activeCulledBlockEntities;

        this.activeCullingTimeNs = 0L;
        this.activeRenderedSections = 0;
        this.activeCulledSections = 0;
        this.activeRenderedEntities = 0;
        this.activeCulledEntities = 0;
        this.activeSkippedEntityLayers = 0;
        this.activeRenderedBlockEntities = 0;
        this.activeCulledBlockEntities = 0;
    }

    public void setCpuFrameTimeNs(long ns) {
        this.cpuFrameTimeNs = ns;
    }

    public void setChunkMeshTimeNs(long ns) {
        this.chunkMeshTimeNs = ns;
    }

    public void addCullingTimeNs(long ns) {
        this.activeCullingTimeNs += ns;
    }

    public void setCullingTimeNs(long ns) {
        addCullingTimeNs(ns);
    }

    public void incrementRenderedSections(int count) {
        this.activeRenderedSections += count;
    }

    public void incrementCulledSections(int count) {
        this.activeCulledSections += count;
    }

    public void incrementRenderedEntities(int count) {
        this.activeRenderedEntities += count;
    }

    public void incrementCulledEntities(int count) {
        this.activeCulledEntities += count;
    }

    public void incrementSkippedEntityLayers(int count) {
        this.activeSkippedEntityLayers += count;
    }

    public void incrementRenderedBlockEntities(int count) {
        this.activeRenderedBlockEntities += count;
    }

    public void incrementCulledBlockEntities(int count) {
        this.activeCulledBlockEntities += count;
    }

    public double getCpuFrameTimeMs() {
        return this.cpuFrameTimeNs / 1_000_000.0;
    }

    public double getChunkMeshTimeMs() {
        return this.chunkMeshTimeNs / 1_000_000.0;
    }

    public double getCullingTimeMs() {
        return this.lastCullingTimeNs / 1_000_000.0;
    }

    public int getRenderedSections() {
        return this.lastRenderedSections;
    }

    public int getCulledSections() {
        return this.lastCulledSections;
    }

    public int getRenderedEntities() {
        return this.lastRenderedEntities;
    }

    public int getCulledEntities() {
        return this.lastCulledEntities;
    }

    public int getSkippedEntityLayers() {
        return this.lastSkippedEntityLayers;
    }

    public int getRenderedBlockEntities() {
        return this.lastRenderedBlockEntities;
    }

    public int getCulledBlockEntities() {
        return this.lastCulledBlockEntities;
    }
}