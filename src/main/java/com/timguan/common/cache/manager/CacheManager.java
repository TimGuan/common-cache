package com.timguan.common.cache.manager;

import com.timguan.common.cache.common.CacheLoggerUtil;
import org.slf4j.Logger;

/**
 * Cache接口抽象
 * Created by gkq on 16/4/6.
 */
public abstract class CacheManager {
    private static final Logger logger = CacheLoggerUtil.getLogger();
    private String commonPrefix;

    public CacheManager(String namespace, String env) {
        if (namespace == null || "".equals(namespace)) {
            throw new RuntimeException("miss config,namespace can not be empty!");
        }
        if (env == null || "".equals(env)) {
            throw new RuntimeException("miss config,env can not be empty!");
        }
        this.commonPrefix = namespace + "_" + env + "|";
    }

    /**
     * 为key添加环境 命名空间信息
     *
     * @param originKey
     */
    public String rebuildKey(String originKey) {
        return this.commonPrefix + originKey;
    }

    /**
     * 锁key
     */
    public String buildLockKey(String originKey) {
        return rebuildKey(originKey) + "_lock";
    }

    /**
     * set操作
     *
     * @param key
     * @param exp
     * @param o
     * @param <T>
     *
     * @see #innerSet(String, int, Object)
     */
    public final <T> void set(String key, int exp, T o) {
        String tmpkey = rebuildKey(key);
        try {
            if (exp > 0 && o != null) {
                innerSet(tmpkey, exp, o);
            } else {
                logger.error("set failed , expire must large than 0 ! key:{},exp:{}", key, exp);
            }
        } catch (Exception e) {
            logger.error("set failed!key:" + tmpkey, e);
        }
    }

    /**
     * inner函数不要调用父类的非inner函数，因为key发生变更
     *
     * @param key
     * @param exp
     * @param o
     * @param <T>
     */
    abstract <T> void innerSet(String key, int exp, T o);

    /**
     * get操作
     *
     * @param key
     * @param tClass
     * @param <T>
     *
     * @see #innerGet(String, Class)
     */
    public final <T> T get(String key, Class<T> tClass) {
        String tmpkey = rebuildKey(key);
        T result = null;
        try {
            result = innerGet(tmpkey, tClass);
        } catch (Exception e) {
            logger.error("get failed!key:" + tmpkey, e);
        }
        return result;
    }

    /**
     * inner函数不要调用父类的非inner函数，因为key发生变更
     *
     * @param key
     * @param tClass
     * @param <T>
     */
    abstract <T> T innerGet(String key, Class<T> tClass);

    /**
     * delete
     *
     * @param key
     *
     * @see #innerDelete(String)
     */
    public final void delete(String key) {
        String tmpkey = rebuildKey(key);
        try {
            innerDelete(tmpkey);
        } catch (Exception e) {
            logger.error("delete failed!key:" + tmpkey, e);
        }
    }

    /**
     * inner函数不要调用父类的非inner函数，因为key发生变更
     *
     * @param key
     */
    abstract void innerDelete(String key);

    /**
     * 原子读
     *
     * @param key
     *
     * @see #getCas(String)
     */
    public final long getCas(String key) {
        String tmpkey = rebuildKey(key);
        long val = 0;
        try {
            val = innerGetCas(tmpkey);
        } catch (Exception e) {
            logger.error("gets failed!key:" + tmpkey, e);
        }
        return val;
    }

    /**
     * inner函数不要调用父类的非inner函数，因为key发生变更
     *
     * @param key
     */
    abstract long innerGetCas(String key);

    /**
     * 原子写入
     *
     * @param key
     * @param casValue 版本号
     * @param exp
     * @param value
     *
     * @see #innerCas(String, long, int, Object)
     */
    public final boolean cas(String key, long casValue, int exp, Object value) {
        String tmpkey = rebuildKey(key);
        boolean isSuccess = false;
        try {
            if (exp > 0) {
                isSuccess = innerCas(tmpkey, casValue, exp, value);
            } else {
                logger.error("cas failed , expire must large than 0 ! key:{},exp:{}", key, exp);
            }
        } catch (Exception e) {
            logger.error("cas failed!key:" + tmpkey + ",cas:" + casValue, e);
        }
        return isSuccess;
    }

    /**
     * inner函数不要调用父类的非inner函数，因为key发生变更
     *
     * @param key
     * @param casValue
     * @param exp
     * @param value
     */
    abstract boolean innerCas(String key, long casValue, int exp, Object value);

    /**
     * @see #innerClose()
     */
    public void close() {
        try {
            innerClose();
        } catch (Exception e) {
            logger.error("close failed!", e);
        }
    }

    abstract void innerClose();

    /**
     * 获取锁 如果锁可用,立即返回true，否则立即返回false，作为非阻塞式锁使用
     *
     * @param key     key
     * @param timeout 等待的超时时间
     * @param expire  锁被动失效时间
     *
     * @return 如果锁可用   立即返回true，  否则立即返回false
     */
    public boolean tryLock(String key, int timeout, int expire) {
        String tmpkey = buildLockKey(key);
        boolean isSuccess = false;
        try {
            isSuccess = innerTryLock(tmpkey, timeout, expire);
        } catch (Exception e) {
            logger.error("[Cache]try lock failed!", e);
            //认为数据正在被更新,数据将在物理失效时得到更新
        }
        return isSuccess;
    }

    abstract boolean innerTryLock(String key, int timeout, int expiry);

    /**
     * 释放锁
     *
     * @param key
     */
    public void releaseLock(String key) {
        String tmpkey = buildLockKey(key);
        try {
            innerReleaseLock(tmpkey);
        } catch (Exception e) {
            logger.error("[Cache]release lock failed!", e);
        }
    }

    abstract void innerReleaseLock(String key);
}
