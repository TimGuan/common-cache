package com.timguan.common.cache.web;

import com.googlecode.htmlcompressor.compressor.HtmlCompressor;
import com.timguan.common.cache.common.CacheLoggerUtil;
import org.slf4j.Logger;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * HTML压缩
 */
public class CompressResponseFilter implements Filter {
    private static final Logger logger = CacheLoggerUtil.getLogger();
    private HtmlCompressor compressor;

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp,
            FilterChain chain) throws IOException, ServletException {
        try {
            CharResponseWrapper responseWrapper = new CharResponseWrapper(
                    (HttpServletResponse)resp);
            chain.doFilter(req, responseWrapper);
            String compressedHtml = compressor.compress(responseWrapper.toString());
            resp.getWriter().write(compressedHtml);
        } catch (Exception e) {
            logger.error("[CompressResponseFilter]compress html failed!");
            throw new RuntimeException("[CompressResponseFilter]compress html failed!", e);
        }
    }

    @Override
    public void init(FilterConfig config) throws ServletException {
        compressor = new HtmlCompressor();
        compressor.setCompressCss(false);
        //不要开启,会把ssi标签也给干掉的
        compressor.setRemoveComments(false);
        //这个开启会有问题,不要开启
        compressor.setCompressJavaScript(false);
    }

    @Override
    public void destroy() {
    }

    public class CharResponseWrapper extends HttpServletResponseWrapper {
        private final CharArrayWriter output;

        @Override
        public String toString() {
            return output.toString();
        }

        public CharResponseWrapper(HttpServletResponse response) {
            super(response);
            output = new CharArrayWriter();
        }

        @Override
        public PrintWriter getWriter() {
            return new PrintWriter(output);
        }
    }
}