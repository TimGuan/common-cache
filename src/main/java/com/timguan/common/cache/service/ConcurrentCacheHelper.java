package com.timguan.common.cache.service;

import com.alibaba.fastjson.JSON;
import com.timguan.common.cache.common.CacheLoggerUtil;
import com.timguan.common.cache.common.Constants;
import com.timguan.common.cache.common.Md5Util;
import com.timguan.common.cache.manager.CacheManager;
import org.slf4j.Logger;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 分段式+锁保护缓存实现
 * Created by guankaiqiang on 2015/2/3.
 */
public class ConcurrentCacheHelper {
    private static final Logger                        logger               = CacheLoggerUtil.getLogger();
    private CacheManager cacheManager         = null;
    private static final LinkedBlockingQueue<Runnable> queue                = new LinkedBlockingQueue<Runnable>(256);
    private static final ThreadPoolExecutor            cacheRefreshExecutor = new ThreadPoolExecutor(4, 16, 30, TimeUnit.SECONDS, queue,
            new ThreadFactory() {
                private final AtomicInteger threadNumber = new AtomicInteger(1);
                private final String namePrefix = "cacheRefreshPool-t-";

                public Thread newThread(Runnable r) {
                    Thread t = new Thread(Thread.currentThread().getThreadGroup(), r,
                            namePrefix + threadNumber.getAndIncrement(), 0);
                    if (t.isDaemon())
                        t.setDaemon(false);
                    if (t.getPriority() != Thread.NORM_PRIORITY)
                        t.setPriority(Thread.NORM_PRIORITY);
                    return t;
                }
            },
            new RejectedExecutionHandler() {
                @Override
                public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                    logger.warn("rejected cache key:" + r.toString());
                }
            });

    /**
     * 外部注入cacheManager创建实例
     *
     * @param cacheManager
     */
    public ConcurrentCacheHelper(CacheManager cacheManager) {
        if (cacheManager == null) {
            logger.error("cache manager can not be empty!");
            throw new RuntimeException("cache manager can not be empty!");
        }
        this.cacheManager = cacheManager;
    }

    /**
     * 更新缓存
     *
     * @param cacheKey       缓存key
     * @param object         缓存对象
     * @param physicalExpire 物理过期时间
     */
    private <T extends Serializable> boolean flushCache(String cacheKey, T object, int physicalExpire, int logicExpire) {
        boolean isSuccess = true;
        try {
            CacheObject<T> cacheObject = new CacheObject<T>();
            cacheObject.setTag(System.currentTimeMillis());
            cacheObject.setTimeToLiveSeconds(logicExpire);
            cacheObject.setObject(object);
            cacheManager.set(cacheKey, physicalExpire, cacheObject);
        } catch (Exception e) {
            logger.warn("[Cache]缓存更新失败,原子写入冲突,cacheKey:" + cacheKey, e);
            isSuccess = false;
        }
        return isSuccess;
    }

    public static abstract class Delegate<T extends Serializable> {
        private T cachedValue;

        public abstract T execute(Object... params);

        protected void setCachedValue(T cachedValue) {
            this.cachedValue = cachedValue;
        }

        /**
         * 缓存的结果集
         */
        public T getCachedValue() {
            return cachedValue;
        }
    }

    private static final char SPLITER   = '|';
    private static final char UNDERLINE = '_';

    /**
     * 构造缓存Key
     *
     * @param key
     * @param version
     * @param params
     */
    public String buildCacheKey(String key, String version, Object... params) {
        StringBuilder sb = new StringBuilder();
        sb.append(key).append(UNDERLINE)
                .append(version).append(SPLITER);
        String cacheKey = null;
        if (params != null) {
            List<Object> tempList = new ArrayList<Object>();
            for (Object param : params) {
                tempList.add(param);
            }
            String tempStr = JSON.toJSONString(tempList);
            cacheKey = sb.toString() + tempStr;
            if (cacheKey.length() >= 200) {
                cacheKey = sb.toString() + Md5Util.computeToHex(tempStr.getBytes(Constants.UTF8));
            }
        } else {
            cacheKey = sb.toString();
            cacheKey = cacheKey.substring(0, cacheKey.length() - 1);
        }
        return cacheKey;
    }

    public static class CacheRefreshTask implements Runnable {
        private final String   cacheKey;
        private final int      hashCode;
        private final Runnable task;

        public CacheRefreshTask(String cacheKey, Runnable task) {
            if (cacheKey == null || task == null) {
                throw new NullPointerException("cache key or task is null");
            }
            this.cacheKey = cacheKey;
            this.task = task;
            hashCode = cacheKey.hashCode();
        }

        @Override
        public void run() {
            task.run();
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (hashCode != obj.hashCode()) {
                return false;
            }
            return (obj instanceof CacheRefreshTask) && this.cacheKey.equals(((CacheRefreshTask)obj).cacheKey);
        }

        @Override
        public String toString() {
            return cacheKey;
        }
    }

    private <T extends Serializable> T execute(String cacheName, String version, final int logicExpire, final int physicalExpire,
            final Delegate<T> delegate, final Object... params) {
        T result = null;
        if (logicExpire > 0 && physicalExpire > 0) {
            final String cacheKey = buildCacheKey(cacheName, version, params);
            final CacheObject<T> cacheObject = cacheManager.get(cacheKey, CacheObject.class);
            //1.获取缓存
            if (cacheObject == null) {
                //2.缓存物理失效
                //执行业务并刷新缓存
                result = delegate.execute(params);
                if (result != null) {
                    flushCache(cacheKey, result, physicalExpire, logicExpire);
                }
            } else {
                //给业务使用的缓存数据
                delegate.setCachedValue(cacheObject.getObject());
                //3.缓存命中，比较缓存是否逻辑过期
                if (cacheObject.isExpire()) {
                    //4.如果已经逻辑过期,尝试获取分布式锁。分布式锁固定存在2秒,不进行主动释放,这个使得依赖本库的缓存时间不能短与2秒
                    if (cacheManager.tryLock(cacheKey, 0, 2)) {
                        if (cacheObject.isExpire()) {
                            cacheRefreshExecutor.execute(new CacheRefreshTask(cacheKey, new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        T result = null;
                                        result = delegate.execute(params);
                                        if (result != null) {
                                            flushCache(cacheKey, result, physicalExpire, logicExpire);
                                        }
                                    } catch (Throwable throwable) {
                                        logger.error("[Cache]delegate method execute failed!", throwable);
                                    }
                                }
                            }));
                        }
                    }
                }
                result = cacheObject.getObject();
            }
        } else {//未开启缓存
            result = delegate.execute(params);
        }
        return result;
    }

    /**
     * 查询缓存:1.缓存未命中，尝试获取更新锁:1.1获取更新锁成功，执行业务并刷新缓存；1.2获取更新锁失败，说明缓存正在被更新，直接执行业务；
     * 2.缓存命中，缓存是否逻辑过期：2.1缓存逻辑未过期，数据有效且正常，直接返回；2.2缓存逻辑过期，获取更新锁：2.2.1更新锁获取成功，执行
     * 业务刷新缓存，2.2.2获取更新锁失败，说明有线程在刷新缓存直接返回逻辑过期的数据；
     *
     * @param cacheConfig
     * @param delegate
     * @param params
     * @param <T>
     */
    public <T extends Serializable> T getCachedResult(CacheConfig.ServiceCacheConfig cacheConfig, Delegate<T> delegate, Object... params) {
        if (cacheConfig != null) {
            return execute(cacheConfig.getCacheKey(), cacheConfig.getVersion(), cacheConfig.getExpire(), cacheConfig.getMaxExpire(), delegate,
                    params);
        } else {
            return delegate.execute(params);
        }
    }

    /**
     * 清除缓存
     *
     * @param cacheConfig cache配置
     * @param params      缓存参数参数
     */
    public <T extends Serializable> void cleanCache(CacheConfig.ServiceCacheConfig cacheConfig, Object... params) {
        if (cacheConfig == null) {
            return;
        }
        String cacheKey = buildCacheKey(cacheConfig.getCacheKey(), cacheConfig.getVersion(), params);
        cacheManager.delete(cacheKey);
    }

    /**
     * 注意：返回的key不包含namespace env等环境相关信息，仅包含业务含义
     * 对缓存操作请通过注入的cacheManager完成，cacheManager封装了namespace env信息
     */
    public String getCacheKey(CacheConfig.ServiceCacheConfig cacheConfig, Object... params) {
        if (cacheConfig == null) {
            throw new RuntimeException("cacheconfig can not be empty");
        }
        return buildCacheKey(cacheConfig.getCacheKey(), cacheConfig.getVersion(), params);
    }
}
