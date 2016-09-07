package com.timguan.common.cache.common;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Properties;

/**
 * properties文件装载
 */
public class ConfLoaderUtil {
    private static final Logger logger = LoggerFactory.getLogger(ConfLoaderUtil.class);
    private Properties p = new Properties();

    public static ConfLoaderUtil newInstance() {
        return new ConfLoaderUtil();
    }

    private ConfLoaderUtil() {

    }

    /**
     * 装载配置
     *
     * @param propPath
     * @return
     */
    public ConfLoaderUtil load(String propPath) {
        InputStream inputStream = null;
        try {
            inputStream = ConfLoaderUtil.class.getResourceAsStream(propPath);
            p.load(inputStream);
            logger.info("dump config:{},values:{}", propPath, JSON.toJSONString(p));
        } catch (Throwable e) {
            logger.error("load config failed,path:" + propPath, e);
            throw new RuntimeException(e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    throw new RuntimeException("failed close stream!", e);
                }
            }
        }
        return this;
    }

    public void setProperty(String name, String value) {
        p.setProperty(name, value);
    }

    private String getProperty(String name, String defaultValue) {
        return p.getProperty(name, defaultValue);
    }

    /**
     * getProperty方法的简写
     *
     * @param name 属性名
     * @return value
     */
    public String getString(String name, String defaultValue) {
        return getProperty(name, defaultValue);
    }

    /**
     * 从配制文件中获取一个整形的配制值，如果没有配制，则返回默认值
     *
     * @param item         属性名
     * @param defaultValue 默认值
     * @return int value
     */
    public int getInt(String item, int defaultValue) {
        String value = getProperty(item, String.valueOf(defaultValue));
        return Integer.parseInt(value);
    }

    public boolean getBoolean(String item, boolean defaultValue) {
        String value = getProperty(item, String.valueOf(defaultValue));
        return Boolean.parseBoolean(value);
    }

    public <T extends Serializable> T getObject(String item, Class<T> clazz, String defaultValue) {
        String value = getProperty(item, defaultValue);
        if (value != null && value.length() != 0) {
            return JSON.parseObject(value, clazz);
        } else {
            return null;
        }
    }
}
