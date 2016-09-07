package com.timguan.common.cache.service;

import com.timguan.common.cache.common.Constants;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;

/**
 * 缓存配置,以后再做更nb的缓存方案设计
 * Created by guankaiqiang on 2014/12/23.
 */
@XmlRootElement(name = "cacheConfig")
public final class CacheConfig implements Serializable {
    private List<ServiceCacheConfig> serviceCacheConfigs;

    public List<ServiceCacheConfig> getServiceCacheConfigs() {
        return serviceCacheConfigs;
    }

    @XmlElement(name = "serviceCacheConfig")
    public void setServiceCacheConfigs(List<ServiceCacheConfig> serviceCacheConfigs) {
        this.serviceCacheConfigs = serviceCacheConfigs;
    }

    @XmlRootElement(name = "serviceCacheConfig")
    public static final class ServiceCacheConfig implements Serializable {
        //有效期，缓存到了这个期限需要进行缓存刷新
        private int    expire;
        //名称
        private String cacheKey;
        //版本号
        private String version;
        //缓存最大有效期，如果过了这个期限，缓存失效
        private int    maxExpire;

        public int getMaxExpire() {
            return maxExpire;
        }

        public void setMaxExpire(int maxExpire) {
            this.maxExpire = maxExpire;
        }

        public int getExpire() {
            return expire;
        }

        public void setExpire(int expire) {
            this.expire = expire;
        }

        public String getCacheKey() {
            return cacheKey;
        }

        public void setCacheKey(String cacheKey) {
            this.cacheKey = cacheKey;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String toString() {
            return "ServiceCacheConfig[cacheKey:" + cacheKey + ",expire:" + expire + ",version:" + version + ",maxExpire:" + maxExpire + "]";
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 53 * hash + this.expire;
            hash = 53 * hash + this.cacheKey != null ? this.cacheKey.hashCode() : 0;
            hash = 53 * hash + this.version != null ? this.version.hashCode() : 0;
            hash = 53 * hash + this.maxExpire;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            if (obj instanceof ServiceCacheConfig) {
                ServiceCacheConfig cacheConfig = (ServiceCacheConfig)obj;
                if (this.expire != cacheConfig.expire) {
                    return false;
                }
                if (this.cacheKey == null || !this.cacheKey.equals(cacheConfig.cacheKey)) {
                    return false;
                }
                if (this.version == null || !this.version.equals(cacheConfig.version)) {
                    return false;
                }
                if (this.maxExpire != cacheConfig.maxExpire) {
                    return false;
                }
                return true;
            } else {
                return false;
            }
        }
    }

    public static CacheConfig loadFromXmlConfig(InputStream inputStream) {
        CacheConfig config;
        try {
            if (inputStream == null) {
                throw new RuntimeException("load config failed，input stream is null!");
            }
            JAXBContext context = JAXBContext.newInstance(CacheConfig.class);
            Unmarshaller um = context.createUnmarshaller();
            config = (CacheConfig)um.unmarshal(inputStream);
        } catch (JAXBException e) {
            throw new RuntimeException("load config failed", e);
        }
        return config;
    }

    public static CacheConfig loadFromXmlConfig(String xmlConfig) {
        CacheConfig config;
        if (xmlConfig == null) {
            throw new RuntimeException("load config failed，config is null!");
        }
        byte[] bytes = xmlConfig.getBytes(Constants.UTF8);
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        return loadFromXmlConfig(bais);
    }
}
