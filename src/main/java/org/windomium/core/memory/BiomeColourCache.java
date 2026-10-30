package org.windomium.core.memory;

import it.unimi.dsi.fastutil.longs.Long2IntLinkedOpenHashMap;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ColorResolver;

public final class BiomeColourCache {
    private static final int DEFAULT_MISS_VALUE = Integer.MIN_VALUE;
    private static final AtomicLong CACHE_HITS = new AtomicLong(0L);
    private static final AtomicLong CACHE_MISSES = new AtomicLong(0L);

    private static final ThreadLocal<Long2IntLinkedOpenHashMap> THREAD_CACHE = ThreadLocal.withInitial(() -> {
        Long2IntLinkedOpenHashMap map = new Long2IntLinkedOpenHashMap(2048, 0.75F);
        map.defaultReturnValue(DEFAULT_MISS_VALUE);
        return map;
    });

    private static volatile int maxEntriesPerThread = 2048;

    private BiomeColourCache() {
    }

    public static int getOrComputeColour(BlockPos pos, ColorResolver resolver, ColourSupplier fallbackSupplier) {
        long key = packKey(pos.getX(), pos.getY(), pos.getZ(), System.identityHashCode(resolver));
        Long2IntLinkedOpenHashMap map = THREAD_CACHE.get();

        int cached = map.getAndMoveToFirst(key);
        if (cached != DEFAULT_MISS_VALUE) {
            CACHE_HITS.incrementAndGet();
            return cached;
        }

        CACHE_MISSES.incrementAndGet();
        int computed = fallbackSupplier.compute();
        if (map.size() >= maxEntriesPerThread) {
            map.removeLastInt();
        }
        map.putAndMoveToFirst(key, computed);
        return computed;
    }

    private static long packKey(int x, int y, int z, int resolverHash) {
        long posHash = BlockPos.asLong(x, y, z);
        return posHash ^ (((long) resolverHash) << 32) ^ (resolverHash & 0xFFFFFFFFL);
    }

    public static void setMaxEntriesPerThread(int maxEntries) {
        maxEntriesPerThread = Math.max(256, maxEntries);
    }

    public static void invalidateCurrentThread() {
        THREAD_CACHE.get().clear();
    }

    public static void resetStats() {
        CACHE_HITS.set(0L);
        CACHE_MISSES.set(0L);
    }

    public static double getHitRatioPercentage() {
        long hits = CACHE_HITS.get();
        long total = hits + CACHE_MISSES.get();
        if (total == 0L) {
            return 0.0;
        }
        return (hits * 100.0) / total;
    }

    @FunctionalInterface
    public interface ColourSupplier {
        int compute();
    }
}