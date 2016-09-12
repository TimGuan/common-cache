package com.timguan.common.cache.web;

import com.timguan.common.cache.common.*;
import com.timguan.common.cache.manager.CacheManager;
import net.sf.ehcache.constructs.web.GenericResponseWrapper;
import org.slf4j.Logger;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 页面缓存处理类
 * 1.使用memcache/redis做缓存实现
 * 2.使用可重入锁进行主动缓存更新
 * 3.使用memcache/redis原子写入
 * Created by guankaiqiang on 2014/12/23.
 */
public class ConcurrentHtmlCacheHandler extends HtmlCacheHandler {
    private static final Logger logger = CacheLoggerUtil.getLogger();

    public ConcurrentHtmlCacheHandler(String pageNotFoundSite, CacheConfig.HtmlCacheConfig cacheConfig, CacheManager cacheManager) {
        super(pageNotFoundSite, cacheConfig, cacheManager);
    }

    /**
     * 根据资源key散列出对应的锁ID
     *
     * @param key
     */
    protected ReentrantLock getLock(String key) {
        if (getCacheConfig().getLocks() != null && getCacheConfig().getLocks().length > 0) {
            return getCacheConfig().getLocks()[ConcurrencyUtil.selectLock(key, getCacheConfig().getLocks().length)];
        } else {
            logger.error("[Cache]lockSize is illegal, cacheConfig:{}", getCacheConfig());
            return null;
        }
    }

    @Override
    protected void cacheHandle(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        //尝试获取缓存内容
        String cacheKey = calculateKey(request);
        Html html = getCacheManager().get(cacheKey, Html.class);
        if (html != null) {
            //缓存命中，比较缓存是否逻辑过期
            if (html.isExpire()) {
                //缓存过期（逻辑过期），触发主动更新
                ReentrantLock lock = getLock(cacheKey);
                try {
                    if (lock.tryLock(0, TimeUnit.MICROSECONDS)) {//锁被占用说明已经有线程在处理更新，放弃本次缓存刷新
                        try {
                            if (logger.isInfoEnabled()) {
                                logger.info("[Cache]{}获取本地更新锁成功!", cacheKey);
                            }
                            //分布式锁
                            if (getCacheManager().tryLock(cacheKey, 0, (int) html.getTimeToLiveSeconds())) {
                                if (logger.isInfoEnabled()) {
                                    logger.info("[Cache]{}获取分布式更新锁成功!", cacheKey);
                                }
                                try {
                                    Html tmpHtml = buildHtml(request, response, chain, cacheKey);
                                    if ((tmpHtml != null && tmpHtml.isOk())) {
                                        //正常返回
                                        html = tmpHtml;
                                        flushCache(cacheKey, html);
                                    } else {
                                        if (request.getAttribute(Constants.DISASTER_RECOVERY_FLAGNAME) != null) {
                                            //出现非预期的错误&&容灾标志位开启，进行容灾;容灾标志位未开启但内容为空则按照默认行为处理(正常业务错误);
                                            //将过期的缓存页面返回
                                            html.setTag(System.currentTimeMillis());
                                            flushCache(cacheKey, html);
                                        } else {
                                            //发生预期的业务错误，清除缓存
                                            getCacheManager().delete(cacheKey);
                                        }
                                    }
                                } finally {
                                    //释放锁
                                    if (logger.isInfoEnabled()) {
                                        logger.info("[Cache]{}释放分布式更新锁!", cacheKey);
                                    }
                                    getCacheManager().releaseLock(cacheKey);
                                }
                            } else {
                                //数据正在被更新，返回过期数据
                                if (logger.isInfoEnabled()) {
                                    logger.info("[Cache]{}获取分布式更新锁失败，数据正在更新，直接返回过期数据!", cacheKey);
                                }
                            }
                        } finally {
                            //释放锁
                            if (logger.isInfoEnabled()) {
                                logger.info("[Cache]{}释放本地更新锁!", cacheKey);
                            }
                            lock.unlock();
                        }
                    } else {
                        if (logger.isInfoEnabled()) {
                            logger.info("[Cache]{}获取更新锁失败，数据正在更新，直接返回过期数据!", cacheKey);
                        }
                    }
                } catch (InterruptedException e) {
                    logger.warn("[Cache]" + cacheKey + "获取更新锁失败,将过期内容返回!", e);
                }
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("[Cache]{}缓存命中，且未过期，直接返回!", cacheKey);
                }
            }
        } else {
            //2.缓存未命中，触发被动更新
            if (logger.isInfoEnabled()) {
                logger.info("[Cache]{}缓存未命中，被动更新缓存!", cacheKey);
            }
            html = buildHtml(request, response, chain, cacheKey);
            if (html != null && html.isOk()) {
                ReentrantLock lock = getLock(cacheKey);
                try {
                    if (lock.tryLock(0, TimeUnit.SECONDS)) {
                        if (logger.isInfoEnabled()) {
                            logger.info("[Cache]{}获取更新锁成功!", cacheKey);
                        }
                        try {
                            flushCache(cacheKey, html);
                        } finally {
                            if (logger.isInfoEnabled()) {
                                logger.info("[Cache]{}释放更新锁!", cacheKey);
                            }
                            lock.unlock();
                        }
                    } else {
                        if (logger.isInfoEnabled()) {
                            logger.info("[Cache]{}获取更新锁失败，数据正在更新，直接返回数据!", cacheKey);
                        }
                    }
                } catch (InterruptedException e) {
                    logger.error("[Cache]" + cacheKey + "更新缓存失败!", e);
                }
            }
        }
        if (!response.isCommitted()) {
            writeResponse(request, response, html);
        }
    }

    /**
     * 构造HTML
     *
     * @param request
     * @param response
     * @param chain
     * @throws IOException
     * @throws ServletException
     */
    private Html buildHtml(HttpServletRequest request,
                           HttpServletResponse response,
                           FilterChain chain, String cacheKey) throws IOException, ServletException {
        Html result = null;
        try {
            final ByteArrayOutputStream outstr = new ByteArrayOutputStream();
            final GenericResponseWrapper wrapper = new GenericResponseWrapper(
                    response, outstr);
            chain.doFilter(request, wrapper);
            wrapper.flush();
            result = new Html(wrapper.getStatus(), wrapper.getAllHeaders(), outstr.toByteArray(),
                    getCacheConfig().getExpire());
        } catch (Exception e) {
            logger.error("[Cache]" + cacheKey + "更新失败!", e);
        }
        return result;
    }

    /**
     * 更新缓存
     *
     * @param cacheKey
     * @param newHtml
     */
    private void flushCache(String cacheKey, Html newHtml) {
        if (newHtml != null && newHtml.isOk()) {
            try {
                //性能测试分支，gzip压缩
                if (CompileConfig.isDebug) {
                    newHtml.setBytes(GZipUtil.gzip(newHtml.getBytes()));
                }
                getCacheManager().set(cacheKey, getCacheConfig().getMaxExpire(), newHtml);
            } catch (Exception e) {
                logger.error("[Cache]缓存更新失败,cacheKey:" + cacheKey, e);
            }
        } else {
            logger.warn("[Cache]{}缓存更新失败,数据内容为空", cacheKey);
        }
    }
}
