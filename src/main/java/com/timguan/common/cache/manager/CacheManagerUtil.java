package com.timguan.common.cache.manager;

import com.timguan.common.cache.common.CloseUtil;
import com.timguan.common.cache.common.ConfLoaderUtil;
import com.timguan.common.cache.common.Constants;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;

import java.io.InputStream;

/**
 * Created by gkq on 16/5/11.
 */
public class CacheManagerUtil {
    /**
     * 初始化cacheManager
     */
    public static CacheManager getCacheManager(String cacheClientConfigLo) {
        //memcache redis 等客户端初始化
        ConfLoaderUtil confLoaderUtil = ConfLoaderUtil.newInstance().load(cacheClientConfigLo);
        return getCacheManager(confLoaderUtil);
    }

    /**
     * configLoader 已完成配置文件装载的loadUtil实例
     *
     * @param confLoaderUtilInstance
     */
    public static CacheManager getCacheManager(ConfLoaderUtil confLoaderUtilInstance) {
        CacheManager cacheManager = null;
        //memcache redis 等客户端初始化
        Constants.CacheManagerType cacheManagerType = Constants.CacheManagerType
                .getCacheMangerType(confLoaderUtilInstance.getString(Constants.CONFIG_KEY_CACHEMANAGER, "memcache"));
        String namespace = confLoaderUtilInstance.getString(Constants.CONFIG_KEY_CACHE_NAMESPACE, null);
        String env = confLoaderUtilInstance.getString(Constants.CONFIG_ENV, null);
        switch (cacheManagerType) {
            case MEMCACHE:
                cacheManager = new MemcacheManager.Builder(namespace, env)
                        .setHost(confLoaderUtilInstance.getString(Constants.CONFIG_KEY_MEMCACHE_HOST, null))
                        .setPort(confLoaderUtilInstance.getString(Constants.CONFIG_KEY_MEMCACHE_PORT, null))
                        .setUsername(confLoaderUtilInstance.getString(Constants.CONFIG_KEY_MEMCACHE_USERNAME, null))
                        .setPassword(confLoaderUtilInstance.getString(Constants.CONFIG_KEY_MEMCACHE_PASSWORD, null))
                        .build();
                break;
            case REDIS:
                throw new RuntimeException("redis read-write is not support");
//                cacheManager = new RedisCacheManager.Builder(namespace, env).
//                        setReadOnly(confLoaderUtilInstance.getString(Constants.CONFIG_KEY_REDIS_READONLY, null))
//                        .setMasterName(confLoaderUtilInstance.getString(Constants.CONFIG_KEY_REDIS_MASTERNAME, null))
//                        .setsentienls(confLoaderUtilInstance.getString(Constants.CONFIG_KEY_REDIS_SENTIENLS, null))
//                        .build();
//                break;
            case REDISCLUSTER:
                cacheManager = new RedisClusterCacheManager.Builder(namespace, env)
                        .setRedisServerHosts(confLoaderUtilInstance.getString(Constants.CONFIG_KEY_REDISCLUSTER_HOSTS, null))
                        .setRedisMaxTotal(confLoaderUtilInstance.getInt(Constants.CONFIG_KEY_REDISCLUSTER_MAXTOTAL, 1000))
                        .setRedisMaxIdle(confLoaderUtilInstance.getInt(Constants.CONFIG_KEY_REDISCLUSTER_MAXIDLE, 500))
                        .setRedisMinIdle(confLoaderUtilInstance.getInt(Constants.CONFIG_KEY_REDISCLUSTER_MINIDLE, 400))
                        .setRedisMaxWaitTime(confLoaderUtilInstance.getInt(Constants.CONFIG_KEY_REDISCLUSTER_MAXWAITTIME, 500))
                        .setRedisTimeout(confLoaderUtilInstance.getInt(Constants.CONFIG_KEY_REDISCLUSTER_TIMEOUT, 500))
                        .setRedisTestOnBorrow(confLoaderUtilInstance.getBoolean(Constants.CONFIG_KEY_REDISCLUSTER_TESTONBORROW, false))
                        .setRedisMaxRedirections(confLoaderUtilInstance.getInt(Constants.CONFIG_KEY_REDISCLUSTER_MAXREDIRECTIONS, 2))
                        .build();
                break;
            case EHCACHE:
                InputStream in = null;
                try {
                    String configPath = confLoaderUtilInstance.getString(Constants.CONFIG_EHCACHE_CONFIG_PATH, "ehcache.xml");
                    in = CacheManagerUtil.class.getClassLoader().getResourceAsStream(configPath);
                    Configuration configuration = ConfigurationFactory.parseConfiguration(in);
                    cacheManager = new EhCacheManager.Builder(namespace, env)
                            .setConfiguration(configuration)
                            .setCacheName(confLoaderUtilInstance.getString(Constants.CONFIG_EHCACHE_CACHE_NAME, "defaultCache"))
                            .build();
                } finally {
                    CloseUtil.close(in);
                }
                break;
            default:
                throw new RuntimeException("can not resolve " + cacheManagerType);
        }
        return cacheManager;
    }
}
