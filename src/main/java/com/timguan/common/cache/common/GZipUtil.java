package com.timguan.common.cache.common;

import net.sf.ehcache.constructs.web.AlreadyGzippedException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

/**
 * Created by gkq on 16/5/11.
 */
public class GZipUtil {
    private static final int GZIP_MAGIC_NUMBER_BYTE_1 = 31;
    private static final int GZIP_MAGIC_NUMBER_BYTE_2 = -117;

    public static boolean isGzipped(byte[] candidate) {
        if (candidate == null || candidate.length < 2) {
            return false;
        } else {
            return (candidate[0] == GZIP_MAGIC_NUMBER_BYTE_1 && candidate[1] == GZIP_MAGIC_NUMBER_BYTE_2);
        }
    }

    public static byte[] gzip(byte[] ungzipped) throws IOException, AlreadyGzippedException {
        if (isGzipped(ungzipped)) {
            throw new AlreadyGzippedException("The byte[] is already gzipped. It should not be gzipped again.");
        }
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        final GZIPOutputStream gzipOutputStream = new GZIPOutputStream(bytes);
        gzipOutputStream.write(ungzipped);
        gzipOutputStream.close();
        return bytes.toByteArray();
    }
}
