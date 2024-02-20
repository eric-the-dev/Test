package com.kisonix.jgittest;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.kisonix.jgittest.databinding.ActivityCrashHandlerBinding;

public class CrashHandlerActivity extends AppCompatActivity {
    public static final String REPORT_ACTION = "com.kisonix.jgittest.REPORT_CRASH";
    public static final String TRACE_KEY = "crash_trace";
    private ActivityCrashHandlerBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCrashHandlerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        final var extra = getIntent().getExtras();
        if (extra == null) {
            finishAffinity();
            return;
        }

        final var report = extra.getString(TRACE_KEY, "Unable to get logs.");
        final var fragment = CrashReportFragment.newInstance(report);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(binding.getRoot().getId(), fragment, "crash_report_fragment")
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onBackPressed() {
        finishAffinity();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
