package com.timguan.common.cache.web;

import com.timguan.common.cache.common.CacheLoggerUtil;
import com.timguan.common.cache.common.ConfLoaderUtil;
import com.timguan.common.cache.common.Constants;
import com.timguan.common.cache.manager.CacheManager;
import com.timguan.common.cache.manager.CacheManagerUtil;
import org.apache.catalina.connector.ClientAbortException;
import org.slf4j.Logger;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * HTML分布式缓存实现
 * Created by guankaiqiang on 2014/12/23.
 */
public class RemoteHtmlCacheFilter implements Filter {
    private static final Logger logger       = CacheLoggerUtil.getLogger();
    private              String pageNotFound = null;
    //缓存实现
    private CacheManager cacheManager;
    private static SimpleUrlPatternHandlerMapping cacheHandlerMapping = new SimpleUrlPatternHandlerMapping();

    /**
     * 1.初始化cacheConfig
     * 2.建立urlPattern和cache作用的关联
     *
     * @param filterConfig filter配置
     *
     * @throws ServletException
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        //1.初始化参数
        String staticConfigLo = null, cacheConfigFileLocation = null;
        if (filterConfig != null) {
            cacheConfigFileLocation = filterConfig.getInitParameter(Constants.SYS_PROPERTY_CACHE_CONF_LO);
        }
        if (cacheConfigFileLocation == null || "".equalsIgnoreCase(cacheConfigFileLocation)) {
            cacheConfigFileLocation = Constants.DEFAULT_CACHE_CONF_FILE_LO;
        }
        ConfLoaderUtil confLoaderUtil = ConfLoaderUtil.newInstance().load(cacheConfigFileLocation);
        staticConfigLo = confLoaderUtil.getString(Constants.CONFIG_KEY_WEB_CACHE_STATIC_CONF_LO, Constants.DEFAULT_WEB_CACHE_STATIC_CONF_LO);
        pageNotFound = confLoaderUtil.getString(Constants.CONFIG_KEY_PAGE_NOT_FOUND_SITE, Constants.DEFAULT_PAGE_NOT_FOUND);
        //1.CacheManager初始化
        cacheManager = CacheManagerUtil.getCacheManager(cacheConfigFileLocation);
        //2.读取静态的缓存配置(xml中加载)，当发布页面中数据与元数据发生了不兼容需要缓存版本号
        List<CacheConfig.HtmlCacheConfig> cacheConfigList = CacheConfig.loadFromXmlFile(staticConfigLo).getHtmlCacheConfigs();
        if (cacheConfigList != null && cacheConfigList.size() != 0) {
            for (CacheConfig.HtmlCacheConfig cacheConfig : cacheConfigList) {
                //注册URL pattern 处理handler
                if (cacheHandlerMapping.getCacheHandler(cacheConfig.getUrlPattern()) == null) {
                    cacheHandlerMapping
                            .registHandler(cacheConfig.getUrlPattern(), new ConcurrentHtmlCacheHandler(pageNotFound, cacheConfig, cacheManager));
                    logger.warn("[Cache] 新增cacheHandler for urlPattern:{}", cacheConfig.getUrlPattern());
                }
            }
        } else {
            logger.warn("[Cache] 未读取到有效的静态配置信息");
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = null;
        HttpServletResponse resp = null;
        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            try {
                req = (HttpServletRequest)request;
                resp = (HttpServletResponse)response;
                //                if (req.getMethod().equals(HttpMethod.GET.name())) {//仅对get方法生效
                HtmlCacheHandler cacheHandler = cacheHandlerMapping.getCacheHandler(req);
                if (cacheHandler != null) {
                    cacheHandler.handle(req, resp, chain);
                } else {
                    //url没有对应的缓存Handler进行处理，使用默认的Filter规则继续处理
                    chain.doFilter(request, response);
                }
                //                } else {
                //                    chain.doFilter(request, response);
                //                }
            } catch (ClientAbortException e) {
                logger.warn("[Cache] CacheHandler failed!client abort!url:" + req.getRequestURL(), e);
            } catch (Exception e) {
                logger.error("[Cache] CacheHandler failed!url:" + req.getRequestURL(), e);
                if (!resp.isCommitted()) {
                    resp.sendRedirect(pageNotFound);
                }
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {
        //资源销毁
        cacheManager.close();
    }
}
