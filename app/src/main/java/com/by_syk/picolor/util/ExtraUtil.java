package com.by_syk.picolor.util;

import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.view.View;

import com.by_syk.lib.toast.GlobalToast;
import com.by_syk.picolor.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    /**
     * 实现 Markdown 链接，如：
     *     [Unicode Consortium](http://www.unicode.org)
     */
    public static SpannableString getLinkableMessage(final Context CONTEXT, String message) {
        String newMessage = message;
        final List<String> tagsList = new ArrayList<>();

        Pattern pattern = Pattern.compile("\\[(.*?)\\]\\((.*?)\\)");
        Matcher matcher = pattern.matcher(message);
        while (matcher.find()) {
            tagsList.add(matcher.group(1));
            tagsList.add(matcher.group(2));
            newMessage = newMessage.replaceFirst("\\[(.*?)\\]\\((.*?)\\)", matcher.group(1));
        }

        SpannableString spannableString = new SpannableString(newMessage);
        int temp_pos;
        for (int i = 0, len = tagsList.size(); i < len - 1; i += 2) {
            temp_pos = newMessage.indexOf(tagsList.get(i));
            if (tagsList.get(i + 1).startsWith("copy:")) {
                final String TEXT = tagsList.get(i + 1).substring(5);
                spannableString.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        copy2Clipboard(CONTEXT, TEXT);

                        GlobalToast.showToast(CONTEXT,
                                CONTEXT.getString(R.string.toast_copied, TEXT));
                    }
                }, temp_pos, temp_pos + tagsList.get(i).length(),
                        Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            } else {
                spannableString.setSpan(new URLSpan(tagsList.get(i + 1)),
                        temp_pos, temp_pos + tagsList.get(i).length(),
                        Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            }
        }

        return spannableString;
    }

    @TargetApi(11)
    @SuppressWarnings("deprecation")
    public static void copy2Clipboard(Context context, String text) {
        if (text == null) {
            return;
        }

        if (C.SDK >= 11) {
            ClipboardManager clipboardManager = (ClipboardManager)
                    context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clipData = ClipData.newPlainText(null, text);
            clipboardManager.setPrimaryClip(clipData);
        } else {
            android.text.ClipboardManager clipboardManager = (android.text.ClipboardManager)
                    context.getSystemService(Context.CLIPBOARD_SERVICE);
            clipboardManager.setText(text);
        }
    }
}
