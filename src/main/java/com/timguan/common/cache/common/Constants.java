package com.timguan.common.cache.common;

import java.nio.charset.Charset;

/**
 * Created by guankaiqiang on 2014/12/17.
 */
public class Constants {
    public static final String LOGGER_NAME = "com.timguan.common.cache";
    public static final Charset UTF8 = Charset.forName("utf-8");
    public static final String DISASTER_RECOVERY_FLAGNAME = "com.timguan.commoncache.disasterRecovery";
    public static final String CONTENT_TYPE_HTML = "text/html;charset=UTF-8";
    public static final String WEB_CACHE_KEY_HEAD = "cacheKeyHead";
    public static final String DEFAULT_CACHE_CONF_FILE_LO = "/service-config.properties";
//    public static final String DEFAULT_SERVICE_CACHE_STATIC_CONF_LO = "/service-cacheConfig.xml";
    public static final String DEFAULT_WEB_CACHE_STATIC_CONF_LO = "/web-cacheConfig.xml";
    public static final String DEFAULT_PAGE_NOT_FOUND = "/error.html";
    /**
     * 系统属性：cache配置文件路径,默认/service-config.properties
     */
    public static final String SYS_PROPERTY_CACHE_CONF_LO = "cache.configFileLocation";
    public static final String CONFIG_KEY_CACHE_NAMESPACE = "com.timguan.commoncache.cache.namespace";
//    /**
//     * 业务缓存的静态缓存配置文件路径
//     */
//    public static final String CONFIG_KEY_SERVICE_CACHE_STATIC_CONF_LO = "com.timguan.commoncache.cache.service.staticconfigfile";
    /**
     * 页面缓存的静态缓存配置文件路径
     */
    public static final String CONFIG_KEY_WEB_CACHE_STATIC_CONF_LO = "com.timguan.commoncache.cache.web.staticconfigfile";
    /**
     * cache manager 使用 memcache/redis/rediscluster做缓存实现
     *
     * @see CacheManagerType
     */
    public static final String CONFIG_KEY_CACHEMANAGER = "com.timguan.commoncache.cache.cachemanager";
    /**
     * memcache host
     */
    public static final String CONFIG_KEY_MEMCACHE_HOST = "com.timguan.commoncache.cache.memcache.host";
    /**
     * memcache port
     */
    public static final String CONFIG_KEY_MEMCACHE_PORT = "com.timguan.commoncache.cache.memcache.port";
    /**
     * memcache username
     */
    public static final String CONFIG_KEY_MEMCACHE_USERNAME = "com.timguan.commoncache.cache.memcache.username";
    /**
     * memcache password
     */
    public static final String CONFIG_KEY_MEMCACHE_PASSWORD = "com.timguan.commoncache.cache.memcache.password";
    /**
     * redis 哨兵配置
     */
    public static final String CONFIG_KEY_REDIS_SENTIENLS = "com.timguan.commoncache.cache.redis.sentienls";
    /**
     * redis 只读节点
     */
    public static final String CONFIG_KEY_REDIS_READONLY = "com.timguan.commoncache.cache.redis.readonly";
    /**
     * redis master节点
     */
    public static final String CONFIG_KEY_REDIS_MASTERNAME = "com.timguan.commoncache.cache.redis.mastername";
    /**
     * redis 集群列表
     */
    public static final String CONFIG_KEY_REDISCLUSTER_HOSTS = "com.timguan.commoncache.cache.rediscluster.hosts";

    /**
     * redis 集群列表
     */
    public static final String CONFIG_KEY_REDISCLUSTER_MAXTOTAL = "com.timguan.commoncache.cache.rediscluster.maxtotal";

    /**
     * redis 集群列表
     */
    public static final String CONFIG_KEY_REDISCLUSTER_MINIDLE = "com.timguan.commoncache.cache.rediscluster.minidle";

    /**
     * redis 集群列表
     */
    public static final String CONFIG_KEY_REDISCLUSTER_MAXIDLE = "com.timguan.commoncache.cache.rediscluster.maxidle";

    /**
     * redis 集群列表
     */
    public static final String CONFIG_KEY_REDISCLUSTER_MAXWAITTIME = "com.timguan.commoncache.cache.rediscluster.maxwaittime";

    /**
     * redis 集群列表
     */
    public static final String CONFIG_KEY_REDISCLUSTER_TIMEOUT = "com.timguan.commoncache.cache.rediscluster.timeout";

    /**
     * redis 集群列表
     */
    public static final String CONFIG_KEY_REDISCLUSTER_MAXREDIRECTIONS = "com.timguan.commoncache.cache.rediscluster.maxrediretions";

    /**
     * redis 集群列表
     */
    public static final String CONFIG_KEY_REDISCLUSTER_TESTONBORROW = "com.timguan.commoncache.cache.rediscluster.testonborrow";

    /**
     * 缓存的环境区分
     */
    public static final String CONFIG_ENV = "com.timguan.commoncache.cache.env";
    /**
     * 404发生时跳转的页面地址
     */
    public static final String CONFIG_KEY_PAGE_NOT_FOUND_SITE = "com.timguan.commoncache.cache.web.pageNotFound";


    /**
     * ehcache的相关配置 start
     */
    public static final String CONFIG_EHCACHE_CONFIG_PATH = "com.timguan.commoncache.cache.ehcache.configpath";

    public static final String CONFIG_EHCACHE_CACHE_NAME = "com.timguan.commoncache.cache.ehcache.cachename";

    /**
     * ehcache的相关配置 end
     */

    /**
     * CacheManager实现
     */
    public enum CacheManagerType {
        REDIS,
        MEMCACHE,
        REDISCLUSTER,
        EHCACHE;

        public static CacheManagerType getCacheMangerType(String name) {
            for (CacheManagerType cacheManagerType : CacheManagerType.values()) {
                if (cacheManagerType.name().equalsIgnoreCase(name)) {
                    return cacheManagerType;
                }
            }
            throw new RuntimeException(name + " is not support");
        }
    }
}
