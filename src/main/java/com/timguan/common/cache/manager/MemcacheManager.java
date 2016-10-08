package com.timguan.common.cache.manager;

import com.timguan.common.cache.common.CacheLoggerUtil;
import com.timguan.common.cache.common.Constants;
import net.spy.memcached.*;
import net.spy.memcached.auth.AuthDescriptor;
import net.spy.memcached.auth.PlainCallbackHandler;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import java.util.concurrent.TimeUnit;

/**
 * memcache
 */
public class MemcacheManager extends CacheManager {
    private static final Logger logger = CacheLoggerUtil.getLogger();
    private MemcachedClient memcachedClient;

    public MemcacheManager(String namespace, String env) {
        super(namespace, env);
    }

    MemcacheManager setMemcachedClient(MemcachedClient memcachedClient) {
        this.memcachedClient = memcachedClient;
        return this;
    }

    public static class Builder extends AbstractCacheManagerBuilder {
        private String host;
        private String port;
        private String username;
        private String password;

        public Builder(String namespace, String env) {
            super(namespace, env);
        }

        public Builder setHost(String ht) {
            host = ht;
            logger.info("[MemcacheHelper.init]" + Constants.CONFIG_KEY_MEMCACHE_HOST + "={}", ht);
            return this;
        }

        public Builder setPort(String pt) {
            port = pt;
            logger.info("[MemcacheHelper.init]" + Constants.CONFIG_KEY_MEMCACHE_PORT + "={}", pt);
            return this;
        }

        public Builder setUsername(String username) {
            this.username = username;
            logger.info("[MemcacheHelper.init]" + Constants.CONFIG_KEY_MEMCACHE_USERNAME + "={}", username);
            return this;
        }

        public Builder setPassword(String password) {
            this.password = password;
            logger.info("[MemcacheHelper.init]" + Constants.CONFIG_KEY_MEMCACHE_PASSWORD + "={}", password);
            return this;
        }


        public MemcacheManager build() {
            MemcachedClient memcachedClient = null;
            try {
                if (StringUtils.isNotBlank(this.username) &&
                        StringUtils.isNotBlank(this.password)) {
                    AuthDescriptor ad = new AuthDescriptor(new String[]{"PLAIN"}, new PlainCallbackHandler(this.username, this.password));
                    memcachedClient = new MemcachedClient(new ConnectionFactoryBuilder()
                            .setProtocol(ConnectionFactoryBuilder.Protocol.BINARY)
                            .setAuthDescriptor(ad)
                            .build(),
                            AddrUtil.getAddresses(host + ":" + port));
                } else {
                    memcachedClient = new MemcachedClient(new ConnectionFactoryBuilder().setProtocol(ConnectionFactoryBuilder.Protocol.BINARY).build(),
                            AddrUtil.getAddresses(host + ":" + port));
                }
            } catch (Exception e) {
                throw new RuntimeException("[MemcacheHelper.init]can't initialize memcache client!", e);
            }
            return new MemcacheManager(getNamespace(), getEnv()).setMemcachedClient(memcachedClient);
        }

    }

    @Override
    <T> void innerSet(String key, int exp, T o) {
        memcachedClient.set(key, exp, o);
    }

    @Override
    <T> T innerGet(String key, Class<T> tClass) {
        T result = null;
        Object obj = memcachedClient.get(key);
        if (obj != null) {
            result = (T) obj;
        }
        return result;
    }

    @Override
    void innerDelete(String key) {
        memcachedClient.delete(key);
    }

    @Override
    long innerGetCas(String key) {
        CASValue<Object> result = memcachedClient.gets(key);
        return result == null ? 0 : result.getCas();
    }

    @Override
    boolean innerCas(String key, long casValue, int exp, Object value) {
        return memcachedClient.cas(key, casValue, exp, value,
                memcachedClient.getTranscoder()) == CASResponse.OK;
    }

    @Override
    public void innerClose() {
        memcachedClient.shutdown();
    }

    public boolean innerTryLock(String key, int timeout, int expire) {
        boolean isSuccess = false;
        try {
            //纳秒
            long begin = System.nanoTime();
            do {
//                if (innerCas(key, 0, expire, key)) {
                if (memcachedClient.add(key, expire, key).getStatus().isSuccess()) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("[Cache]get {} lock success,set expire {}", key, expire);
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
