package org.windomium.core.memory;

import java.util.ArrayDeque;
import net.minecraft.core.BlockPos;

public final class ObjectPoolManager {
    private static final int MAX_POOL_DEPTH = 64;
    private static final ThreadLocal<ArrayDeque<BlockPos.MutableBlockPos>> BLOCK_POS_POOL =
            ThreadLocal.withInitial(() -> new ArrayDeque<>(MAX_POOL_DEPTH));

    private ObjectPoolManager() {
    }

    public static BlockPos.MutableBlockPos borrowBlockPos(int x, int y, int z) {
        ArrayDeque<BlockPos.MutableBlockPos> deque = BLOCK_POS_POOL.get();
        BlockPos.MutableBlockPos pos = deque.pollFirst();
        if (pos == null) {
            return new BlockPos.MutableBlockPos(x, y, z);
        }
        return pos.set(x, y, z);
    }

    public static void releaseBlockPos(BlockPos.MutableBlockPos pos) {
        if (pos == null) {
            return;
        }
        ArrayDeque<BlockPos.MutableBlockPos> deque = BLOCK_POS_POOL.get();
        if (deque.size() < MAX_POOL_DEPTH) {
            deque.offerFirst(pos);
        }
    }

    public static void clearCurrentThreadPools() {
        BLOCK_POS_POOL.get().clear();
    }
}