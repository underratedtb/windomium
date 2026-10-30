package org.windomium.client.render;

import net.minecraft.core.BlockPos;

public record RenderTask(BlockPos origin, int priority, Runnable meshAction) implements Runnable, Comparable<RenderTask> {
    @Override
    public void run() {
        this.meshAction.run();
    }

    @Override
    public int compareTo(RenderTask other) {
        return Integer.compare(other.priority, this.priority);
    }
}