package com.timguan.common.cache.web;

import com.timguan.common.cache.common.CacheLoggerUtil;
import org.slf4j.Logger;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.PathMatcher;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by guankaiqiang on 2014/12/23.
 */
public class SimpleUrlPatternHandlerMapping implements CacheHandlerMapping {
    private static final Logger logger = CacheLoggerUtil.getLogger();
    private Map<String, HtmlCacheHandler> handlerMap = new ConcurrentHashMap<String, HtmlCacheHandler>();
    private HtmlCacheHandler rootHandler;
    private HtmlCacheHandler defaultHandler;

    private String getHandlerDescription(HtmlCacheHandler handler) {
        return "handler of type [" + handler.getClass() + "]";
    }

    private void setRootHandler(HtmlCacheHandler rootHandler) {
        this.rootHandler = rootHandler;
    }

    private void setDefaultHandler(HtmlCacheHandler defaultHandler) {
        this.defaultHandler = defaultHandler;
    }

    protected synchronized void registHandler(String urlPath, HtmlCacheHandler cacheHandler) {
        HtmlCacheHandler mappedHandler = this.handlerMap.get(urlPath);
        if (mappedHandler == null) {//double check
            if (!urlPath.startsWith("/")) {
                urlPath = "/" + urlPath;
            }
            if (urlPath.equals("/")) {
                if (logger.isInfoEnabled()) {
                    logger.info("[Cache]Root mapping to " + getHandlerDescription(cacheHandler));
                }
                setRootHandler(cacheHandler);
            } else if (urlPath.equals("/*")) {
                if (logger.isInfoEnabled()) {
                    logger.info("[Cache]Default mapping to " + getHandlerDescription(cacheHandler));
                }
                setDefaultHandler(cacheHandler);
            } else {
                this.handlerMap.put(urlPath, cacheHandler);
                if (logger.isInfoEnabled()) {
                    logger.info("[Cache]Mapped URL path [" + urlPath + "] onto " + getHandlerDescription(cacheHandler));
                }
            }
        }
    }

    protected void registHandlers(Map<String, HtmlCacheHandler> urlMap) {
        if (urlMap.isEmpty()) {
            logger.warn("[Cache]Neither 'urlMap' nor 'mappings' set on SimpleUrlHandlerMapping");
        } else {
            for (Map.Entry<String, HtmlCacheHandler> entry : urlMap.entrySet()) {
                String url = entry.getKey();
                HtmlCacheHandler handler = entry.getValue();
                if (!url.startsWith("/")) {
                    url = "/" + url;
                }
                registHandler(url, handler);
            }
        }
    }

    private PathMatcher pathMatcher = new AntPathMatcher();

    /**
     * Set the PathMatcher implementation to use for matching URL paths
     * against registered URL patterns. Default is AntPathMatcher.
     *
     * @see org.springframework.util.AntPathMatcher
     */
    public void setPathMatcher(PathMatcher pathMatcher) {
        Assert.notNull(pathMatcher, "PathMatcher must not be null");
        this.pathMatcher = pathMatcher;
    }

    /**
     * Return the PathMatcher implementation to use for matching URL paths
     * against registered URL patterns.
     */
    public PathMatcher getPathMatcher() {
        return this.pathMatcher;
    }

    private UrlPathHelper urlPathHelper = new UrlPathHelper();

    /**
     * Set the UrlPathHelper to use for resolution of lookup paths.
     * <p>Use this to override the default UrlPathHelper with a custom subclass,
     * or to share manager UrlPathHelper settings across multiple HandlerMappings
     * and MethodNameResolvers.
     */
    public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
        Assert.notNull(urlPathHelper, "UrlPathHelper must not be null");
        this.urlPathHelper = urlPathHelper;
    }

    /**
     * Return the UrlPathHelper implementation to use for resolution of lookup paths.
     */
    public UrlPathHelper getUrlPathHelper() {
        return urlPathHelper;
    }

    /**
     * @param urlPath
     * @param request
     * @return
     * @throws Exception
     */
    protected HtmlCacheHandler lookupHandler(String urlPath, HttpServletRequest request) {
        //直接match
        HtmlCacheHandler handler = this.handlerMap.get(urlPath);
        if (handler != null) {
            return this.handlerMap.get(urlPath);
        }
        // 正则match
        List<String> matchingPatterns = new ArrayList<String>();
        for (String registeredPattern : this.handlerMap.keySet()) {
            if (getPathMatcher().match(registeredPattern, urlPath)) {
                matchingPatterns.add(registeredPattern);
            }
        }
        String bestPatternMatch = null;
        Comparator<String> patternComparator = getPathMatcher().getPatternComparator(urlPath);
        if (!matchingPatterns.isEmpty()) {
            Collections.sort(matchingPatterns, patternComparator);
            if (logger.isDebugEnabled()) {
                logger.debug("[Cache]Matching patterns for request [" + urlPath + "] are " + matchingPatterns);
            }
            bestPatternMatch = matchingPatterns.get(0);
        }
        if (bestPatternMatch != null) {
            handler = this.handlerMap.get(bestPatternMatch);
            if (logger.isDebugEnabled()) {
                Map<String, String> uriTemplateVariables = new HashMap<String, String>();
                for (String matchingPattern : matchingPatterns) {
                    if (patternComparator.compare(bestPatternMatch, matchingPattern) == 0) {
                        Map<String, String> vars = getPathMatcher().extractUriTemplateVariables(matchingPattern, urlPath);
                        Map<String, String> decodedVars = getUrlPathHelper().decodePathVariables(request, vars);
                        uriTemplateVariables.putAll(decodedVars);
                    }
                }
                logger.debug("[Cache]URI Template variables for request [" + urlPath + "] are " + uriTemplateVariables);
            }
            return handler;
        }
        // 不进行cache
        return null;
    }

    @Override
    public HtmlCacheHandler getCacheHandler(HttpServletRequest request) {
        String lookupPath = getUrlPathHelper().getLookupPathForRequest(request);
        return lookupHandler(lookupPath, request);
    }

    public HtmlCacheHandler getCacheHandler(String urlPattern) {
        return handlerMap.get(urlPattern);
    }

    public String dumpCacheHandlersConfig() {
        List<CacheConfig.HtmlCacheConfig> cacheConfigs = new ArrayList<CacheConfig.HtmlCacheConfig>();
        for (HtmlCacheHandler handler : handlerMap.values()) {
            cacheConfigs.add(handler.getCacheConfig());
        }
        return cacheConfigs.toString();
    }
}
