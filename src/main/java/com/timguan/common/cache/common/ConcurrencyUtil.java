package com.timguan.common.cache.common;

public final class ConcurrencyUtil {
    private ConcurrencyUtil() {
    }

    public static int hash(Object object) {
        int h = object.hashCode();
        h ^= h >>> 20 ^ h >>> 12;
        return h ^ h >>> 7 ^ h >>> 4;
    }

    /**
     * @param key
     * @param numberOfLocks
     */
    public static int selectLock(String key, int numberOfLocks) {
        if (numberOfLocks > 1) {
            int number = numberOfLocks & (numberOfLocks - 1);
            if (number != 0) {
                throw new RuntimeException("[Cache]Lock number must be a power of two: " + number);
            }
            return hash(key) & (numberOfLocks - 1);
        } else if (numberOfLocks == 1) {
            return 0;
        } else {
            throw new RuntimeException("[Cache]lockSize is illegal,size: " + numberOfLocks);
        }
    }
}
