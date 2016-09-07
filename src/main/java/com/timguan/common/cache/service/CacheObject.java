package com.timguan.common.cache.service;

import java.io.Serializable;

/**
 * 缓存对象
 */
public class CacheObject<T extends Serializable> implements Serializable {
    private T object;
    private long tag;
    private int timeToLiveSeconds;

    /**
     * 缓存对象
     *
     * @return
     */
    public T getObject() {
        return object;
    }

    public void setObject(T object) {
        this.object = object;
    }

    /**
     * 缓存tag,缓存时间戳
     *
     * @return
     */
    public long getTag() {
        return tag;
    }

    public void setTag(long tag) {
        this.tag = tag;
    }

    /**
     * 生存周期
     *
     * @return
     */
    public int getTimeToLiveSeconds() {
        return timeToLiveSeconds;
    }

    public void setTimeToLiveSeconds(int timeToLiveSeconds) {
        this.timeToLiveSeconds = timeToLiveSeconds;
    }

    /**
     * 是否过期
     *
     * @return
     */
    public boolean isExpire() {
        return this.getTag() + this.getTimeToLiveSeconds() * 1000 < System.currentTimeMillis();
    }
}