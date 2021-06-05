package com.app.crash;


import android.content.Context;
import android.os.Build;
import android.text.format.Formatter;

import java.math.BigDecimal;

/**
 * bytes格式化工具类
 */
final class BytesUtil {


    private final static long KB_1024 = 1024L;
    private final static long MB_1024 = KB_1024 * KB_1024;
    private final static long GB_1024 = MB_1024 * KB_1024;
    private final static long KB_1000 = 1000L;
    private final static long MB_1000 = KB_1000 * KB_1000;
    private final static long GB_1000 = MB_1000 * KB_1000;


    private static boolean isO() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }

    /**
     * MB to bytes
     *
     * @param mb MB
     * @return 格式化后的数据
     */
    private static long mbToBytes(long mb) {
        return bytesToBytes(mb * MB_1024);
    }

    /**
     * KB to bytes
     *
     * @param kb KB
     * @return 格式化后的数据
     */
    private static long kbToBytes(long kb) {
        return bytesToBytes(kb * KB_1024);
    }

    private static long bytesToBytes(long bytes) {
        if (isO()) {
            if (bytes < KB_1024) {
                return bytes;
            } else if (bytes < MB_1024) {
                return bytes * KB_1000 / KB_1024;
            } else if (bytes < GB_1024) {
                return bytes * MB_1000 / MB_1024;
            } else {
                return BigDecimal.valueOf((double) bytes / (double) GB_1024 * (double) GB_1000).longValue();
            }
        }
        return bytes;
    }

    /**
     * 格式化bytes
     *
     * @param context 上下文
     * @param bytes
     * @return 格式化后的字符串
     */
    static String formatFileSizeByBytes(Context context, long bytes) {
        return Formatter.formatFileSize(context, bytesToBytes(bytes));
    }

    /**
     * 格式化kb
     *
     * @param context 上下文
     * @param kb
     * @return 格式化后的字符串
     */
    static String formatFileSizeByKB(Context context, long kb) {
        return Formatter.formatFileSize(context, kbToBytes(kb));
    }

    /**
     * 格式化mb
     *
     * @param context 上下文
     * @param mb
     * @return 格式化后的字符串
     */
    static String formatFileSizeByMB(Context context, long mb) {
        return Formatter.formatFileSize(context, mbToBytes(mb));
    }
}
