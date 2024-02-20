package com.kisonix.jgittest;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.DeviceUtils;
import com.kisonix.jgittest.BuildConfig;
import com.kisonix.jgittest.databinding.FragmentCrashReportBinding;

public class CrashReportFragment extends Fragment {

    public static final String KEY_TITLE = "crash_title";
    public static final String KEY_MESSAGE = "crash_message";
    public static final String KEY_TRACE = "crash_trace";
    public static final String KEY_CLOSE_APP_ON_CLICK = "close_on_app_click";
    private FragmentCrashReportBinding binding;
    private boolean closeAppOnClick = true;

    @NonNull
    public static CrashReportFragment newInstance(@NonNull final String trace) {
        return CrashReportFragment.newInstance(null, null, trace, true);
    }

    @NonNull
    public static CrashReportFragment newInstance(
            @Nullable final String title,
            @Nullable final String message,
            @NonNull final String trace,
            boolean closeAppOnClick) {
        final var frag = new CrashReportFragment();
        final var args = new Bundle();

        args.putString(KEY_TRACE, trace);
        args.putBoolean(KEY_CLOSE_APP_ON_CLICK, closeAppOnClick);

        if (title != null) {
            args.putString(KEY_TITLE, title);
        }

        if (message != null) {
            args.putString(KEY_MESSAGE, message);
        }

        frag.setArguments(args);
        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        binding = FragmentCrashReportBinding.inflate(inflater, parent, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View root, Bundle savedInstanceState) {
        super.onViewCreated(root, savedInstanceState);
        if (BuildConfig.DEBUG) {
            binding.debugContainer.setVisibility(View.VISIBLE);
            binding.releaseContainer.setVisibility(View.GONE);
        } else {
            binding.debugContainer.setVisibility(View.GONE);
            binding.releaseContainer.setVisibility(View.VISIBLE);
        }
        final var args = requireArguments();
        this.closeAppOnClick = args.getBoolean(KEY_CLOSE_APP_ON_CLICK);
        var title = "InfinityLive crashed";
        var message = "Please report us with the following stacktrace :";
        var trace = "";
        if (args.containsKey(KEY_TITLE)) {
            title = args.getString(KEY_TITLE);
        }

        if (args.containsKey(KEY_MESSAGE)) {
            message = args.getString(KEY_MESSAGE);
        }

        if (args.containsKey(KEY_TRACE)) {
            trace = args.getString(KEY_TRACE);
            trace = buildReportText(trace);
        } else {
            trace = "No stack strace was provided for the report";
        }

        //        binding.crashTitle.setText(title);
        //        binding.crashSubtitle.setText(message);
        binding.logText.setText(trace);
        binding.logText.setTextIsSelectable(true);

        //        final var report = trace;
        //        binding.closeButton.setOnClickListener(
        //                v -> {
        //                    if (closeAppOnClick) {
        //                        requireActivity().finishAffinity();
        //                    } else {
        //                        requireActivity().finish();
        //                    }
        //                });
        //        binding.reportButton.setOnClickListener(v -> reportTrace(report));
        //        baseUrl =
        //                BaseApplication.reverseReversed(
        //                        getResources().getString(R.string.mtrl_picker_invalid_ipa));
        //        reporter = new Reporter(baseUrl);
    }

    private void reportTrace(String report) {
        //        Callback<Void> callback =
        //                new Callback<Void>() {
        //                    @Override
        //                    public void onResponse(Call<Void> call, Response<Void> response) {
        //                        if (response.isSuccessful()) {
        //                            BaseApplication.showToast("Successfully reported!");
        //                        } else {
        //                            BaseApplication.showToast("Error: Failed to report");
        //                        }
        //                    }
        //
        //                    @Override
        //                    public void onFailure(Call<Void> call, Throwable t) {
        //                        BaseApplication.showToast("Error: " + t.getMessage());
        //                    }
        //                };
        //        headers.put(
        //                BaseApplication.reverseReversed(
        //                        getResources().getString(R.string.mtrl_picker_invalid_yekipax)),
        //                BaseApplication.getShabarlarbar(
        //                        getResources().getString(R.string.mtrl_picker_invalid_sha256)));
        //        reporter.postMessage(headers, report, callback);
        /*ClipboardUtils.copyText("aWebKit CrashLog", report);

        final var url = ("https://github.com/kisonix/aWebKit/issues");
        final var intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);*/
    }

    @NonNull
    private String buildReportText(String trace) {
        return "InfinityLive crash report\n"
                + "Manufacturer: "
                + DeviceUtils.getManufacturer()
                + "\n"
                + "Device: "
                + DeviceUtils.getModel()
                + "\n"
                + "ABI: "
                + BaseApplication.getArch()
                + "\n"
                + "SDK version: "
                + Build.VERSION.SDK_INT
                + "\n"
                + "App version: "
                + AppUtils.getAppVersionName()
                + " ("
                + AppUtils.getAppVersionCode()
                + ")\n\n Stacktrace: \n"
                + trace;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
