package com.timguan.common.cache.web;

import com.alibaba.fastjson.annotation.JSONField;
import net.sf.ehcache.constructs.web.Header;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * Created by guankaiqiang on 2014/12/23.
 * 抽象的html对象
 */
public class Html implements Serializable {
    public static final int SC_OK = 200;
    public static final int SC_NO_CONTENT = 204;

    //ehcache中定义的Header未实现默认的构造函数导致fastjson无法序列化,定义HeaderAdapter用来解决序列化问题
    static class HeaderAdapter<T extends Serializable> implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name = null;
        private T value = null;
        private net.sf.ehcache.constructs.web.Header.Type type = null;

        public HeaderAdapter() {

        }

        public HeaderAdapter(Header<T> header) {
            String name = header.getName();
            T value = header.getValue();
            this.name = name;
            this.value = value;
            this.type = net.sf.ehcache.constructs.web.Header.Type.determineType(value.getClass());
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public T getValue() {
            return value;
        }

        public void setValue(T value) {
            this.value = value;
        }

        public Header.Type getType() {
            return type;
        }

        public void setType(Header.Type type) {
            this.type = type;
        }
    }

    //缓存时间戳
    private long tag;
    //缓存内容体
    private byte[] bytes;
    private static final long ONE_HOUR = 60 * 60;
    //ehcache中定义的Header未实现默认的构造函数导致fastjson无法序列化，通过localResponseHeaders进行序列化，对外暴露responseHeaders
    private ArrayList<HeaderAdapter<? extends Serializable>> headerAdapters = new ArrayList<HeaderAdapter<? extends Serializable>>();
    //response head
    @JSONField(serialize = false, deserialize = false)
    private ArrayList<Header<? extends Serializable>> responseHeaders = new ArrayList<Header<? extends Serializable>>();
    //HTTP 状态码
    private int statusCode;
    //页面的有效期
    private long timeToLiveSeconds;

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public long getTag() {
        return tag;
    }

    public void setTag(long tag) {
        this.tag = tag;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public long getTimeToLiveSeconds() {
        return timeToLiveSeconds;
    }

    public void setTimeToLiveSeconds(long timeToLiveSeconds) {
        this.timeToLiveSeconds = timeToLiveSeconds;
    }

    public ArrayList<HeaderAdapter<? extends Serializable>> getHeaderAdapters() {
        return headerAdapters;
    }

    public ArrayList<Header<? extends Serializable>> getResponseHeaders() {
        return responseHeaders;
    }

    public void setResponseHeaders(Collection<Header<? extends Serializable>> responseHeaders) {
        this.responseHeaders.addAll(responseHeaders);
        for (Header<? extends Serializable> header : responseHeaders) {
            headerAdapters.add(new HeaderAdapter(header));
        }
    }

    public Html() {
    }

    public Html(final int statusCode,
                final Collection<Header<? extends Serializable>> headers,
                final byte[] body,
                long timeToLiveSeconds) {
        if (headers != null) {
            setResponseHeaders(headers);
        }
        setTimeToLiveWithCheckForNeverExpires(timeToLiveSeconds);
        this.statusCode = statusCode;
        this.timeToLiveSeconds = timeToLiveSeconds;
        this.bytes = body;
        this.tag = System.currentTimeMillis();
    }

    protected void setTimeToLiveWithCheckForNeverExpires(long timeToLiveSeconds) {
        //0 means 0
        if (timeToLiveSeconds == 0 || timeToLiveSeconds > ONE_HOUR) {
            this.timeToLiveSeconds = 0;
        } else {
            this.timeToLiveSeconds = timeToLiveSeconds;
        }
    }

    /**
     * 是否逻辑失效
     *
     * @return
     */
    public boolean isExpire() {
        return this.getTag() + this.getTimeToLiveSeconds() * 1000 < System.currentTimeMillis();
    }

    /**
     * 页面是否正常
     *
     * @return
     */
    public boolean isOk() {
        return (this.getStatusCode() == SC_OK &&
                this.bytes != null && this.bytes.length != 0) || this.getStatusCode() == SC_NO_CONTENT;
    }

    @Override
    public String toString() {
        return "Html{" +
                "tag=" + tag +
                ", bytes=" + Arrays.toString(bytes) +
                ", responseHeaders=" + responseHeaders +
                ", statusCode=" + statusCode +
                ", timeToLiveSeconds=" + timeToLiveSeconds +
                '}';
    }
}
