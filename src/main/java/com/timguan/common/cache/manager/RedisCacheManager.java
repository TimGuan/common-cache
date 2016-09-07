package com.timguan.common.cache.manager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.timguan.common.cache.common.CacheLoggerUtil;
import com.timguan.common.cache.common.Constants;
import org.slf4j.Logger;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.jedis.Transaction;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * redis 读写分离，读通过slb进行
 * 性能有瓶颈
 * Created by gkq on 16/4/6.
 */
@Deprecated
public class RedisCacheManager extends CacheManager {
    private static final Logger logger = CacheLoggerUtil.getLogger();
    private Jedis master;
    private Jedis slave;

    public RedisCacheManager(String namespace, String env) {
        super(namespace, env);
    }

    public RedisCacheManager setMaster(Jedis master) {
        this.master = master;
        return this;
    }

    public RedisCacheManager setSlave(Jedis slave) {
        this.slave = slave;
        return this;
    }

    public static class Builder extends AbstractCacheManagerBuilder {
        private String readOnly;
        private String sentienls;
        private String masterName;

        public Builder(String namespace, String env) {
            super(namespace, env);
        }

        public Builder setMasterName(String masterName) {
            this.masterName = masterName;
            logger.info("[RedisCacheManager.init]" + Constants.CONFIG_KEY_REDIS_MASTERNAME + "={}", masterName);
            return this;
        }

        public Builder setReadOnly(String readOnly) {
            this.readOnly = readOnly;
            logger.info("[RedisCacheManager.init]" + Constants.CONFIG_KEY_REDIS_READONLY + "={}", readOnly);
            return this;
        }

        public Builder setsentienls(String sentienls) {
            this.sentienls = sentienls;
            logger.info("[RedisCacheManager.init]" + Constants.CONFIG_KEY_REDIS_SENTIENLS + "={}", sentienls);
            return this;
        }

        public RedisCacheManager build() {
            Jedis slave = null;
            Jedis master = null;
            try {
                Set<String> sentinels = null;
                String[] sentienlsList = sentienls.split(",");
                if (sentienlsList.length < 0) {
                    throw new RuntimeException("[RedisCacheManager.init]sentienl is null.");
                }
                if (readOnly == null || readOnly.equals("")) {
                    throw new RuntimeException("[RedisCacheManager.init]readonly is null.");
                }
                sentinels = new HashSet<String>();
                for (String sentienl : sentienlsList) {
                    sentinels.add(sentienl);
                }
                JedisSentinelPool sentinelPool = new JedisSentinelPool(masterName, sentinels);
                master = sentinelPool.getResource();
                String[] slaveHostPort = readOnly.split(":");
                slave = new Jedis(slaveHostPort[0], Integer.valueOf(slaveHostPort[1]), 2000, 2000);
            } catch (Exception e) {
                throw new RuntimeException("[RedisCacheManager.init]can't initialize redis client!", e);
            }
            return new RedisCacheManager(getNamespace(), getEnv()).setMaster(master).setSlave(slave);
        }

    }

    @Override
    <T> void innerSet(String key, int exp, T o) {
        master.setex(key.getBytes(Constants.UTF8), exp, JSON.toJSONBytes(o, SerializerFeature.WriteClassName));
    }

    @Override
    <T> T innerGet(String key, Class<T> tClass) {
        T result = null;
        //            byte[] value = slave.get(key.getBytes(ConstField.UTF8));
        //            if (value != null) {
        //                result = (T) SerializeUtil.deserialize(value);
        //            }
        String value = slave.get(key);
        if (value != null) {
            result = JSON.parseObject(value, tClass);
        }
        return result;
    }

    @Override
    void innerDelete(String key) {
        master.del(key);
    }

    @Override
    long innerGetCas(String key) {
        return 0;
    }

    @Override
    boolean innerCas(String key, long casValue, int exp, Object value) {
        //        innerSet(key, exp, value);
        //        return true;
        master.watch(key);
        Transaction transaction = master.multi();
        transaction.setex(key.getBytes(Constants.UTF8), exp, JSON.toJSONBytes(value, SerializerFeature.WriteClassName));
        List<Object> objects = transaction.exec();
        if (objects != null && objects.size() > 0 && "OK".equalsIgnoreCase(String.valueOf(objects.get(0)))) {
            return true;
        }
        return false;
    }

    @Override
    public void innerClose() {
        slave.close();
        master.close();
    }

    @Override
    boolean innerTryLock(String key, int timeout, int expire) {
        boolean isSuccess = false;
        try {
            //纳秒
            long begin = System.nanoTime();
            do {
                //EX seconds -- Set the specified expire time, in seconds.
                //PX milliseconds -- Set the specified expire time, in milliseconds.
                //NX -- Only set the key if it does not already exist.
                //XX -- Only set the key if it already exist.
                String result = master.set(key, key, "NX", "EX", expire);
                if ("OK".equalsIgnoreCase(result)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("[Cache]get {} lock success,set expire {}", key,
                                expire);
                    }
                    isSuccess = true;
                    break;
                }
                if (timeout == 0) {
                    break;
                }
                Thread.sleep(100);
            } while ((System.nanoTime() - begin) < TimeUnit.MILLISECONDS.toNanos(timeout));
            //因超时没有获得锁
        } catch (Throwable e) {
            logger.error("[Cache]try lock failed!", e);
            //当Redis异常时,不阻塞业务流程
            isSuccess = true;
        }
        return isSuccess;
    }

    @Override
    void innerReleaseLock(String key) {
        innerDelete(key);
    }
}
