package com.timguan.common.cache.web;


import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 缓存配置
 * 边界层的cache设置版本号的作用不在于缓存的对象数据结构是否发生了变更而是，页面的内容是否做了升级
 * 等待所有缓存失效再发布js是不合理的，所以每次输出的页面中有js依赖的数据源需要将这个cache版本进行升级
 * Created by guankaiqiang on 2014/12/23.
 */
@XmlRootElement(name = "cacheConfig")
public final class CacheConfig implements Serializable {
    private List<HtmlCacheConfig> htmlCacheConfigs;

    public List<HtmlCacheConfig> getHtmlCacheConfigs() {
        return htmlCacheConfigs;
    }

    @XmlElement(name = "htmlCacheConfig")
    public void setHtmlCacheConfigs(List<HtmlCacheConfig> htmlCacheConfigs) {
        this.htmlCacheConfigs = htmlCacheConfigs;
    }

    @XmlRootElement(name = "htmlCacheConfig")
    public static final class HtmlCacheConfig implements Serializable {
        //url规则，cache所作用的域
        private String urlPattern;
        //有效期，缓存到了这个期限需要进行缓存刷新
        private int expire;
        //名称
        private String cacheKey;
        //版本号
        private String version;
        //缓存最大有效期，如果过了这个期限，缓存失效
        private int maxExpire;
        private int lockSize;
        private ReentrantLock[] locks;

        public ReentrantLock[] getLocks() {
            return locks;
        }

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

        public String getUrlPattern() {
            return urlPattern;
        }

        public void setUrlPattern(String urlPattern) {
            this.urlPattern = urlPattern;
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

        public int getLockSize() {
            return lockSize;
        }

        public void setLockSize(int lockSize) {
            this.lockSize = lockSize;
            if (lockSize > 0) {
                ReentrantLock[] locks = new ReentrantLock[lockSize];
                for (int i = 0; i < locks.length; i++) {
                    locks[i] = new ReentrantLock();
                }
                this.locks = locks;
            }
        }

        @Override
        public String toString() {
            return "HtmlCacheConfig[urlPattern:" + urlPattern + ",expire:" + expire + ",cacheKey:" + cacheKey + ",version:" + version + ",maxExpire:" + maxExpire + ",lockSize:" + lockSize + "]";
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 53 * hash + this.urlPattern != null ? this.urlPattern.hashCode() : 0;
            hash = 53 * hash + this.expire;
            hash = 53 * hash + this.cacheKey != null ? this.cacheKey.hashCode() : 0;
            hash = 53 * hash + this.version != null ? this.version.hashCode() : 0;
            hash = 53 * hash + this.maxExpire;
            hash = 53 * hash + this.lockSize;
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
            if (obj instanceof HtmlCacheConfig) {
                HtmlCacheConfig cacheConfig = (HtmlCacheConfig) obj;
                if (this.urlPattern == null || !this.urlPattern.equals(cacheConfig.urlPattern)) {
                    return false;
                }
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
                if (this.lockSize != cacheConfig.lockSize) {
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
            config = (CacheConfig) um.unmarshal(inputStream);
        } catch (JAXBException e) {
            throw new RuntimeException("load config failed", e);
        }
        return config;
    }

    private static final Charset UTF8 = Charset.forName("utf-8");

    public static CacheConfig loadFromXmlConfig(String xmlConfig) {
        CacheConfig config;
        if (xmlConfig == null) {
            throw new RuntimeException("load config failed，config is null!");
        }
        byte[] bytes = xmlConfig.getBytes(UTF8);
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        return loadFromXmlConfig(bais);
    }

    public static CacheConfig loadFromXmlFile(String filePath) {
        InputStream in = null;
        CacheConfig cacheConfig = null;
        try {
            in = RemoteHtmlCacheFilter.class.getResourceAsStream(filePath);
            cacheConfig = CacheConfig.loadFromXmlConfig(in);
        } catch (Exception e) {
            throw new RuntimeException("can't find " + filePath, e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    throw new RuntimeException("failed close stream!", e);
                }
            }
        }
        return cacheConfig;
    }

}
