package com.kisonix.jgittest.gitmanager;

import android.app.ProgressDialog;
import android.content.Context;
import android.util.Log;
import android.webkit.URLUtil;
import com.blankj.utilcode.util.ThreadUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.kisonix.jgittest.TaskExecutor;
import java.io.File;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ProgressMonitor;

public class RepositoryCloner {
    private Context context;
    private File directory;
    private CloneTaskListener listener;
    private ProgressDialog prog;

    public RepositoryCloner(Context context, File directory, CloneTaskListener listener) {
        this.context = context;
        this.directory = directory;
        this.listener = listener;
    }

    private Git git = null;

    public void cloneRepository(String repositoryURL) {
        if (repositoryURL.isEmpty()
                || repositoryURL.isBlank()
                || !URLUtil.isValidUrl(repositoryURL)) {
            listener.onTaskFailed("Invaild url");
            return;
        }
        prog = new ProgressDialog(context);
        prog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        prog.setIndeterminate(true);
        prog.setTitle("Cloning repository...");
        prog.setCancelable(false);
        var output = new File(directory, extractRepositoryNameFromURL(repositoryURL));
        var monitor = new CloneProgressMonitor(prog);

        var task =
                TaskExecutor.executeAsyncProvideError(
                        () -> {
                            String url = repositoryURL.trim();
                            if (!url.endsWith(".git")) {
                                url += ".git";
                            }
                            git =
                                    Git.cloneRepository()
                                            .setURI(url)
                                            .setDirectory(output)
                                            .setProgressMonitor(monitor)
                                            .call();
                            return git;
                        });
        prog.setCancelable(false);
        prog.show();
        task.whenComplete(
                (result, error) -> {
                    ThreadUtils.runOnUiThread(
                            () -> {
                                prog.dismiss();
                                if (result != null && error == null) {
                                    result.close();
                                    listener.onTaskCompleted(output);
                                    return;
                                }
                                listener.onTaskFailed(Log.getStackTraceString(error));
                            });
                });
    }

    private String extractRepositoryNameFromURL(String url) {
        String repositoryName = "";
        int lastSlashIndex = url.lastIndexOf("/");

        if (lastSlashIndex >= 0 && lastSlashIndex < url.length() - 1) {
            repositoryName = url.substring(lastSlashIndex + 1);

            if (repositoryName.endsWith(".git")) {
                repositoryName = repositoryName.substring(0, repositoryName.length() - 4);
            }
        }

        return repositoryName;
    }

    public class CloneProgressMonitor implements ProgressMonitor {
        private ProgressDialog progressDialog;
        public boolean cancelled = false;

        public CloneProgressMonitor(ProgressDialog progressDialog) {
            this.progressDialog = progressDialog;
        }

        public void cancel() {
            cancelled = true;
        }

        @Override
        public void start(int totalTask) {
            // progressDialog.setMax(totalTask);
        }

        @Override
        public void beginTask(String title, int totalWork) {
            ThreadUtils.runOnUiThread(
                    () -> {
                        progressDialog.setMessage(title);
                    });
        }

        @Override
        public void update(int completed) {}

        @Override
        public void endTask() {}

        @Override
        public boolean isCancelled() {
            return cancelled;
        }
    }

    public interface CloneTaskListener {
        void onTaskCompleted(File output);

        void onTaskFailed(String message);
    }
}
