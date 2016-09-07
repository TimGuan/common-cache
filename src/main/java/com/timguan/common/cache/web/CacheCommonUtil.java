package com.timguan.common.cache.web;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by guankaiqiang on 2015/1/22.
 */
public class CacheCommonUtil {
    /**
     * 根据Http请求获取缓存Key
     *
     * @param request
     * @return ${domian}/${URI}?${queryString}
     */
    public static String getCacheKeyByRequest(HttpServletRequest request) {
        if (request != null) {
            return request.getRequestURL().toString().replace("http://", "").replace("//", "/");
        } else {
            return "";
        }
    }
}
