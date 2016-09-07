package com.timguan.common.cache.web;

import com.timguan.common.cache.common.CacheLoggerUtil;
import com.timguan.common.cache.common.CompileConfig;
import com.timguan.common.cache.common.Constants;
import com.timguan.common.cache.common.GZipUtil;
import com.timguan.common.cache.manager.CacheManager;
import net.sf.ehcache.constructs.web.Header;
import org.slf4j.Logger;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.TreeSet;

/**
 * Created by guankaiqiang on 2014/12/23.
 */
public abstract class HtmlCacheHandler {
    private static final Logger logger = CacheLoggerUtil.getLogger();
    private static final String SPLITER = "|";
    private static final String UNDERLINE = "_";
    private CacheConfig.HtmlCacheConfig cacheConfig;
    private CacheManager cacheManager;
    private String pageNotFoundSite;
    protected boolean isCacheEnable;

    public HtmlCacheHandler(String pageNotFoundSite, CacheConfig.HtmlCacheConfig cacheConfig, CacheManager cacheManager) {
        this.pageNotFoundSite = pageNotFoundSite;
        setCacheConfig(cacheConfig);
        setCacheManager(cacheManager);
    }

    public String getPageNotFoundSite() {
        return pageNotFoundSite;
    }

    protected int getLockSize() {
        return getCacheConfig() == null || getCacheConfig().getLocks() == null ? 0 : getCacheConfig().getLocks().length;
    }

    public void setCacheManager(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public CacheManager getCacheManager() {
        return this.cacheManager;
    }

    public CacheConfig.HtmlCacheConfig getCacheConfig() {
        return cacheConfig;
    }

    public void setCacheConfig(CacheConfig.HtmlCacheConfig cacheConfig) {
//        CacheConfig.HtmlCacheConfig config = cacheConfig;
//        if ((this.cacheConfig != null && this.cacheConfig.getLockSize() != config.getLockSize()) || this.cacheConfig == null) {
//            logger.warn("[Cache]cacheHandler:{},reset lock!", config.getUrlPattern());
//            resetLocks(config.getLockSize());
//        }
        if ((this.cacheConfig != null && this.cacheConfig.getLockSize() != cacheConfig.getLockSize()) || this.cacheConfig == null) {
            logger.warn("[Cache]cacheHandler:{},reset lock!", cacheConfig.getUrlPattern());
        }
        this.cacheConfig = cacheConfig;
        if (cacheConfig.getExpire() > 0 && cacheConfig.getMaxExpire() > 0) {
            this.isCacheEnable = true;
        } else {
            logger.warn("[Cache]cache of {} is disable!", cacheConfig.getUrlPattern());
            this.isCacheEnable = false;
        }
    }

    public void handle(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws Exception {
        if (!isCacheEnable) {
            chain.doFilter(request, response);
        } else {
            cacheHandle(request, response, chain);
        }
    }

    /**
     * 构建cacheKey
     *
     * @param request
     * @return
     */
    protected String calculateKey(HttpServletRequest request) {
        //环境 APPID APPVERSION属性提前完成拼接
        String key = request.getAttribute(Constants.WEB_CACHE_KEY_HEAD) != null ?
                (String) request.getAttribute(Constants.WEB_CACHE_KEY_HEAD) : null;
        key = key != null ? key : CacheCommonUtil.getCacheKeyByRequest(request);
        StringBuilder sb = new StringBuilder(key);
        sb.append(getCacheConfig().getCacheKey()).append(UNDERLINE)
                .append(getCacheConfig().getVersion()).append(SPLITER);
        return sb.toString();
    }

    protected abstract void cacheHandle(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws Exception;

    /**
     * 回写response
     *
     * @param request
     * @param response
     * @param html
     * @throws IOException
     */
    protected void writeResponse(final HttpServletRequest request,
                                 final HttpServletResponse response,
                                 final Html html) throws IOException {
        if (!response.isCommitted()) {
            if (html != null && html.isOk()) {
                response.setContentType(Constants.CONTENT_TYPE_HTML);
                setStatus(response, html);
                setHeaders(response, html); // do headers last so that users can override with their own header sets
                writeContent(response, html);
            } else {
                //回写的页面存在异常，且未指定跳转页，默认跳转至404
                response.sendRedirect(getPageNotFoundSite());
            }
        }
    }

    /**
     * Status code
     *
     * @param response
     * @param html
     */
    protected void setStatus(final HttpServletResponse response,
                             final Html html) {
        response.setStatus(html.getStatusCode());
    }

    /**
     * Set the headers in the response object
     *
     * @param response
     * @param html
     */
    protected void setHeaders(final HttpServletResponse response,
                              final Html html) {
        final Collection<Header<? extends Serializable>> headers = html.getResponseHeaders();
        // Track which headers have been set so all headers of the same name
        // after the first are added
        final TreeSet<String> setHeaders = new TreeSet<String>(
                String.CASE_INSENSITIVE_ORDER);
        for (final Header<? extends Serializable> header : headers) {
            final String name = header.getName();
            switch (header.getType()) {
                case STRING:
                    if (setHeaders.contains(name)) {
                        response.addHeader(name, (String) header.getValue());
                    } else {
                        setHeaders.add(name);
                        response.setHeader(name, (String) header.getValue());
                    }
                    break;
                case DATE:
                    if (setHeaders.contains(name)) {
                        response.addDateHeader(name, (Long) header.getValue());
                    } else {
                        setHeaders.add(name);
                        response.setDateHeader(name, (Long) header.getValue());
                    }
                    break;
                case INT:
                    if (setHeaders.contains(name)) {
                        response.addIntHeader(name, (Integer) header.getValue());
                    } else {
                        setHeaders.add(name);
                        response.setIntHeader(name, (Integer) header.getValue());
                    }
                    break;
                default:
                    throw new IllegalArgumentException("No mapping for Header: "
                            + header);
            }
        }
    }

    /**
     * 回写
     *
     * @param response
     * @param html
     * @throws IOException
     */
    protected void writeContent(
            final HttpServletResponse response,
            final Html html)
            throws IOException {
        byte[] body = null;
        if (html.getBytes() != null) {
            body = html.getBytes();
        } else {
            body = new byte[0];
        }
        if (CompileConfig.isDebug) {
            if (GZipUtil.isGzipped(html.getBytes())) {
                response.addHeader("Content-Encoding", "gzip");
            }
        }
        OutputStream out = new BufferedOutputStream(response.getOutputStream());
        out.write(body);
        out.flush();
    }
}
