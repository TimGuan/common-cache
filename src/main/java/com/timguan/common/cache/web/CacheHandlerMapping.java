package com.timguan.common.cache.web;

import javax.servlet.http.HttpServletRequest;

/**
 * 顶层HTML缓存的mapping处理，根据请求定位对应的缓存处理器
 * Created by guankaiqiang on 2014/12/23.
 */
public interface CacheHandlerMapping {
    public HtmlCacheHandler getCacheHandler(HttpServletRequest request);
}
