package com.timguan.common.cache.common;

import net.spy.memcached.compat.log.Logger;
import net.spy.memcached.compat.log.LoggerFactory;

import java.io.Closeable;

/**
 * CloseUtil exists to provide a safe means to close anything closeable. This
 * prevents exceptions from being thrown from within finally blocks while still
 * providing logging of exceptions that occur during close. Exceptions during
 * the close will be logged using the spy logging infrastructure, but will not
 * be propagated up the stack.
 *
 * @see
 */
public final class CloseUtil {

    private static Logger logger = LoggerFactory.getLogger(CloseUtil.class);

    private CloseUtil() {
        // Empty
    }

    /**
     * Close a closeable.
     */
    public static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                logger.info("Unable to close %s", closeable, e);
            }
        }
    }
}