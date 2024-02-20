package com.kisonix.jgittest;

import android.app.Application;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.annotation.Nullable;
import com.blankj.utilcode.util.ThrowableUtils;
import java.util.Arrays;
import kotlin.collections.ArraysKt;

public class BaseApplication extends Application {
    private static final String AARCH64 = "arm64-v8a";
    private static final String ARM = "armeabi-v7a";
    private static BaseApplication instance;
    private Thread.UncaughtExceptionHandler uncaughtExceptionHandler = null;

    public static boolean isAbiSupported() {
        return Arrays.asList(Build.SUPPORTED_ABIS).contains(getArch());
    }

    public static boolean isAarch64() {
        return ArraysKt.contains(Build.SUPPORTED_64_BIT_ABIS, AARCH64);
    }

    public static boolean isArmv7a() {
        return ArraysKt.contains(Build.SUPPORTED_32_BIT_ABIS, ARM);
    }

    @Nullable
    public static String getArch() {
        if (isAarch64()) {
            return AARCH64;
        } else if (isArmv7a()) {
            return ARM;
        }
        return null;
    }

    @Override
    public void onCreate() {
        instance = this;
        uncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(
                (thread, th) -> {
                    handleCrashed(thread, th);
                });

        super.onCreate();
    }

    private void handleCrashed(Thread thread, Throwable th) {
        // writeException(th);//write exception in data dir
        try {
            var intent = new Intent();
            intent.setAction(CrashHandlerActivity.REPORT_ACTION);
            intent.putExtra(CrashHandlerActivity.TRACE_KEY, ThrowableUtils.getFullStackTrace(th));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            if (uncaughtExceptionHandler != null) {
                uncaughtExceptionHandler.uncaughtException(thread, th);
            }
            System.exit(1);
        } catch (Exception error) {
            Log.e("Unable to show crash handler activity", error.getMessage());
        }
    }
}
