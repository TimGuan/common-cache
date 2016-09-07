package com.timguan.common.cache.common;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * 使用二进制序列化方式
 * https://github.com/mybatis/redis-cache
 *
 * @see net.spy.memcached.transcoders.BaseSerializingTranscoder#deserialize(byte[])
 * @see net.spy.memcached.transcoders.BaseSerializingTranscoder#serialize(Object)
 */
public final class SerializeUtil {
    private static final Logger logger = LoggerFactory.getLogger(SerializeUtil.class);

    /**
     * Get the bytes representing the given serialized object.
     */
    public static byte[] serialize(Object o) {
        if (o == null) {
            throw new NullPointerException("Can't serialize null");
        }
        byte[] rv = null;
        ByteArrayOutputStream bos = null;
        ObjectOutputStream os = null;
        try {
            bos = new ByteArrayOutputStream();
            os = new ObjectOutputStream(bos);
            os.writeObject(o);
            os.close();
            bos.close();
            rv = bos.toByteArray();
        } catch (IOException e) {
            throw new IllegalArgumentException("Non-serializable object", e);
        } finally {
            CloseUtil.close(os);
            CloseUtil.close(bos);
        }
        return rv;
    }

    /**
     * Get the object represented by the given serialized bytes.
     */
    public static Object deserialize(byte[] in) {
        Object rv = null;
        ByteArrayInputStream bis = null;
        ObjectInputStream is = null;
        try {
            if (in != null) {
                bis = new ByteArrayInputStream(in);
                is = new ObjectInputStream(bis);
                rv = is.readObject();
                is.close();
                bis.close();
            }
        } catch (IOException e) {
            logger.warn("Caught IOException decoding %d bytes of data",
                    in == null ? 0 : in.length, e);
        } catch (ClassNotFoundException e) {
            logger.warn("Caught CNFE decoding %d bytes of data",
                    in == null ? 0 : in.length, e);
        } finally {
            CloseUtil.close(is);
            CloseUtil.close(bis);
        }
        return rv;
    }
}
