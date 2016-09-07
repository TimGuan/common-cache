package com.timguan.common.cache.common;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CacheLoggerUtil {
    private static final Logger logger = LoggerFactory.getLogger(Constants.LOGGER_NAME);

    public static Logger getLogger() {
        return logger;
    }
}