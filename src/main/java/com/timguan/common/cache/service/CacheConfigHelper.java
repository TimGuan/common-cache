package com.timguan.common.cache.service;

import com.timguan.common.cache.common.CacheLoggerUtil;
import com.timguan.common.cache.common.CloseUtil;
import org.slf4j.Logger;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by guankaiqiang on 2015/1/8.
 * 暂时使用硬编码key再去diamond中查找对应cache配置的方式进行cache
 * 业务的cache对象发生变更都需要将版本号升级，在增量发布的时候可以规避缓存对象不一致的问题
 */
public class CacheConfigHelper {
    private static final Logger logger = CacheLoggerUtil.getLogger();
    private Map<String, CacheConfig.ServiceCacheConfig> serviceCacheConfigMap = new HashMap<String, CacheConfig.ServiceCacheConfig>();

    /**
     * 初始化配置文件
     *
     * @param serviceStaticConfigFile 静态配置文件路径
     */
    public synchronized void init(String serviceStaticConfigFile) {
        //1.读取静态的缓存配置(xml中加载)
        InputStream in = null;
        try {
            in = CacheConfigHelper.class.getResourceAsStream(serviceStaticConfigFile);
            List<CacheConfig.ServiceCacheConfig> cacheConfigList = CacheConfig.loadFromXmlConfig(in).getServiceCacheConfigs();
            if (cacheConfigList != null && cacheConfigList.size() != 0) {
                for (CacheConfig.ServiceCacheConfig cacheConfig : cacheConfigList) {
                    this.serviceCacheConfigMap.put(cacheConfig.getCacheKey(), cacheConfig);
                }
            } else {
                logger.warn("[Cache]未设置缓存静态配置!");
            }
        } catch (Exception e) {
            throw new RuntimeException("can't find " + serviceStaticConfigFile + "!", e);
        } finally {
            CloseUtil.close(in);
        }
    }

    /**
     * 缓存配置
     *
     * @param serviceCacheConfigMap
     */
    public synchronized void init(Map<String, CacheConfig.ServiceCacheConfig> serviceCacheConfigMap) {
        this.serviceCacheConfigMap = serviceCacheConfigMap;
    }

    /**
     * 私有化构造函数
     */
    private CacheConfigHelper() {
    }

    public static CacheConfigHelper getInstance() {
        return CacheConfigHelperHolder.INSTANCE;
    }

    public CacheConfig.ServiceCacheConfig getServiceCacheConfig(String cacheKey) {
        return this.serviceCacheConfigMap.get(cacheKey);
    }

    static final class CacheConfigHelperHolder {
        private static final CacheConfigHelper INSTANCE = new CacheConfigHelper();
    }
}
