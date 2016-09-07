package com.timguan.common.cache.manager;

/**
 * CacheManagerBuilder抽象类，提取CacheManager相关的功能属性
 * Created by gkq on 16/5/16.
 */
public abstract class AbstractCacheManagerBuilder {
    private String namespace;
    private String env;

    public AbstractCacheManagerBuilder(String namespace, String env) {
        this.namespace = namespace != null ? namespace.toUpperCase() : "";
        this.env = env;
        if ("".equals(this.namespace)) {
            throw new RuntimeException("miss config,namespace can not be empty!");
        }
        if (this.env == null || "".equals(this.env)) {
            throw new RuntimeException("miss config,env can not be empty!");
        }
    }

    /**
     * 缓存命名空间
     *
     * @return
     */
    protected String getNamespace() {
        return this.namespace;
    }

    /**
     * 环境信息
     *
     * @return
     */
    protected String getEnv() {
        return env;
    }
}
