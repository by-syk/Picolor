package com.by_syk.picolor.util;

import android.content.Context;
import android.net.Uri;

import java.io.File;

/**
 * Created by By_syk on 2016-07-17.
 */
public class ExtraUtil {
    /**
     * @param get 为false则清理
     */
    public static File getTempFile(Context context, boolean get) {
        // context.getCacheDir() 无法被其他图库程序访问
        File tempFile = new File(context.getExternalCacheDir(), "temp");

        if (!get && tempFile.exists()) {
            tempFile.delete();
        }

        return tempFile;
    }

    public static Uri getTempUri(Context context, boolean get) {
        return Uri.fromFile(getTempFile(context, get));
    }

    public static int calculateInSampleSize(int[] max, int width, int height) {
        // 默认4096*4096
        max[0] = max[0] > 0 ? max[0] : 4096;
        max[1] = max[1] > 0 ? max[1] : 4096;

        int in_sample_size = width / max[0];
        in_sample_size = (height / max[1] > in_sample_size)
                ? (height / max[1]) : in_sample_size;
        ++in_sample_size;

        return in_sample_size;
    }
}
