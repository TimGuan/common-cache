package com.timguan.common.cache.manager;

import com.timguan.common.cache.common.CacheLoggerUtil;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.Configuration;
import org.slf4j.Logger;

/**
 * User: <a href="mailto:lenolix@163.com">李星</a>
 * Version: 1.0.0
 * Since: 16/6/29 下午9:23
 */
public class EhCacheManager extends CacheManager {

    private static final Logger logger = CacheLoggerUtil.getLogger();

    public EhCacheManager(String namespace, String env) {
        super(namespace, env);
    }

    private Ehcache ehcache;

    public EhCacheManager setEhcache(Ehcache ehcache) {
        this.ehcache = ehcache;
        return this;
    }

    public static class Builder extends AbstractCacheManagerBuilder {
        private Configuration configuration;
        private String cacheName;

        public Builder(String namespace, String env) {
            super(namespace, env);
        }

        public Builder setConfiguration(Configuration configuration) {
            this.configuration = configuration;
            return this;
        }

        public Builder setCacheName(String cacheName) {
            this.cacheName = cacheName;
            return this;
        }

        public EhCacheManager build() {
            net.sf.ehcache.CacheManager cacheManager = net.sf.ehcache.CacheManager.newInstance(configuration);
            return new EhCacheManager(getNamespace(), getEnv()).setEhcache(cacheManager.getEhcache(cacheName));
        }

    }


    @Override
    <T> void innerSet(String key, int exp, T o) {
        ehcache.put(new Element(key, o, false, exp, exp));
    }

    @Override
    <T> T innerGet(String key, Class<T> tClass) {
        Element element = ehcache.get(key);
        if (element != null) {
            return (T) element.getObjectValue();
        }
        return null;
    }

    @Override
    void innerDelete(String key) {
        ehcache.remove(key);
    }

    @Override
    long innerGetCas(String key) {
        return 0;
    }

    @Override
    boolean innerCas(String key, long casValue, int exp, Object value) {
        return false;
    }

    @Override
    void innerClose() {
    }

    @Override
    boolean innerTryLock(String key, int timeout, int expiry) {
        return true;
    }

    @Override
    void innerReleaseLock(String key) {

    }
}
