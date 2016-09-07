package com.timguan.common.cache.web;

import com.timguan.common.cache.common.CacheLoggerUtil;
import com.timguan.common.cache.common.CompileConfig;
import com.timguan.common.cache.common.Constants;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.constructs.web.*;
import net.sf.ehcache.constructs.web.filter.CachingFilter;
import org.slf4j.Logger;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;

/**
 * Created by gkq on 16/1/25.
 */
public class LocalHtmlCacheFilter extends CachingFilter {
    private static final String  SPLITER                       = "|";
    private static final String  DEFAULT_CACHE_NAME            = "LOCALCACHE";
    private static final String  EHCACHE_CONF_LOCATION         = "ehcacheConfigLocation";
    private static final String  DEFAULT_EHCACHE_CONF_LOCATION = "/web-ehcacheConfig.xml";
    private static final Logger  logger                        = CacheLoggerUtil.getLogger();
    //    关闭gzip功能, tomcat完成gzip会导致nginx的ssi等功能失效
    private static final boolean ACCEPT_GZIP                   = false & CompileConfig.isDebug;

    /**
     * 关闭gzip功能
     * tomcat完成gzip会导致nginx的ssi等功能失效
     *
     * @param request
     * @param response
     * @param chain
     * @return
     * @throws AlreadyGzippedException
     * @throws Exception
     */
    protected PageInfo buildPage(final HttpServletRequest request,
            final HttpServletResponse response, final FilterChain chain)
            throws Exception {
        // Invoke the next entity in the chain
        final ByteArrayOutputStream outstr = new ByteArrayOutputStream();
        final GenericResponseWrapper wrapper = new GenericResponseWrapper(
                response, outstr);
        chain.doFilter(request, wrapper);
        wrapper.flush();
        long timeToLiveSeconds = blockingCache.getCacheConfiguration()
                .getTimeToLiveSeconds();
        // Return the page info
        return new PageInfo(wrapper.getStatus(), wrapper.getContentType(),
                wrapper.getCookies(), outstr.toByteArray(), ACCEPT_GZIP,
                timeToLiveSeconds, wrapper.getAllHeaders());
    }

    /**
     * 回写数据
     *
     * @param request
     * @param response
     * @param pageInfo
     * @throws IOException
     * @throws ResponseHeadersNotModifiableException
     */
    protected void writeContent(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final PageInfo pageInfo)
            throws IOException {
        byte[] body;
        boolean shouldBodyBeZero = ResponseUtil.shouldBodyBeZero(request,
                pageInfo.getStatusCode());
        if (shouldBodyBeZero) {
            body = new byte[0];
        } else if (ACCEPT_GZIP && acceptsGzipEncoding(request)) {
            //        modify by guankaiqiang,nginx进行gzip,tomcat完成gzip会导致nginx的ssi等功能失效
            body = pageInfo.getGzippedBody();
            if (ResponseUtil.shouldGzippedBodyBeZero(body, request)) {
                body = new byte[0];
            } else {
                ResponseUtil.addGzipHeader(response);
            }
        } else {
            body = pageInfo.getUngzippedBody();
        }
        response.setContentLength(body.length);
        OutputStream out = new BufferedOutputStream(response.getOutputStream());
        out.write(body);
        out.flush();
    }

    @Override
    protected String getCacheName() {
        if (cacheName != null && cacheName.length() > 0) {
            logger.info("[LOCALCACHE]Using configured cacheName of {}.", cacheName);
            return cacheName;
        } else {
            cacheName = DEFAULT_CACHE_NAME;
            logger.info("[LOCALCACHE]No cacheName configured. Using default of {}.", DEFAULT_CACHE_NAME);
            return DEFAULT_CACHE_NAME;
        }
    }

    private CacheManager cacheManager = null;

    @Override
    public void doInit(FilterConfig filterConfig) throws CacheException {
        String location = null;
        if (filterConfig != null) {
            location = filterConfig.getInitParameter(EHCACHE_CONF_LOCATION);
        }
        location = location != null && location.length() > 0 ? location : DEFAULT_EHCACHE_CONF_LOCATION;
        URL url = getClass().getResource(location);
        cacheManager = CacheManager.newInstance(url);//内部已单例实现
        super.doInit(filterConfig);
    }

    //实例化
    protected CacheManager getCacheManager() {
        return cacheManager;
    }

    @Override
    protected String calculateKey(HttpServletRequest request) {
        String key = request.getAttribute(Constants.WEB_CACHE_KEY_HEAD) != null ?
                (String) request.getAttribute(Constants.WEB_CACHE_KEY_HEAD) : null;
        key = key != null ? key : CacheCommonUtil.getCacheKeyByRequest(request);
        StringBuilder sb = new StringBuilder(key).append(cacheName).append(SPLITER);
        return sb.toString();
    }
}
