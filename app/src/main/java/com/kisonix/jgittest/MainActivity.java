package com.kisonix.jgittest;

import android.app.ProgressDialog;
import android.util.Log;
import android.webkit.URLUtil;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import com.blankj.utilcode.util.ThreadUtils;
import com.kisonix.jgittest.databinding.ActivityMainBinding;
import com.kisonix.jgittest.gitmanager.RepositoryCloner;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Callable;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private ProgressDialog prog;
    private final String REPO_URL = "https://github.com/eric-the-dev/Test.git";
    private Git git = null;
    private StringBuilder output = new StringBuilder();

    private File dataDir;
    private String username = "eric-the-dev";
    private String password = "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";
    private String SuccessMessage = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        prog = new ProgressDialog(MainActivity.this);
        prog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        prog.setIndeterminate(true);
        prog.setCancelable(false);
        binding.outputTextView.setTextIsSelectable(true);
        dataDir = ContextCompat.getExternalFilesDirs(getApplicationContext(), null)[0];
        binding.cloneButton.setOnClickListener(
                v -> {
                    var inputUrl = binding.repoInput.getText().toString();
                    var repoUrl =
                            (inputUrl != null && URLUtil.isValidUrl(inputUrl))
                                    ? inputUrl
                                    : REPO_URL;
                    gitCloneOrPull(repoUrl);
                });
        binding.pullButton.setOnClickListener(
                v -> {
                    var inputUrl = binding.repoInput.getText().toString();
                    var repoUrl =
                            (inputUrl != null && URLUtil.isValidUrl(inputUrl))
                                    ? inputUrl
                                    : REPO_URL;
                    gitCloneOrPull(repoUrl);
                });
        binding.addButton.setOnClickListener(
                v -> {
                    var inputUrl = binding.repoInput.getText().toString();
                    var repoUrl =
                            (inputUrl != null && URLUtil.isValidUrl(inputUrl))
                                    ? inputUrl
                                    : REPO_URL;
                    try {
                        gitAdd(getGitDirectoryFromUrl(inputUrl), null);
                    } catch (URISyntaxException err) {
                        showToast(err.getMessage());
                    }
                });
        binding.commitButton.setOnClickListener(
                v -> {
                    var inputUrl = binding.repoInput.getText().toString();
                    var repoUrl =
                            (inputUrl != null && URLUtil.isValidUrl(inputUrl))
                                    ? inputUrl
                                    : REPO_URL;
                    try {
                        gitCommit(getGitDirectoryFromUrl(inputUrl), null, true);
                    } catch (URISyntaxException err) {
                        showToast(err.getMessage());
                    }
                });
        binding.pushButton.setOnClickListener(
                v -> {
                    var inputUrl = binding.repoInput.getText().toString();
                    var repoUrl =
                            (inputUrl != null && URLUtil.isValidUrl(inputUrl))
                                    ? inputUrl
                                    : REPO_URL;
                    try {
                        gitPush(getGitDirectoryFromUrl(inputUrl), "main");
                    } catch (URISyntaxException err) {
                        showToast(err.getMessage());
                    }
                });
    }

    private void gitCloneOrPull(String repoUrl) {
        try {
            var gitDir = getGitDirectoryFromUrl(repoUrl);
            if (!gitDir.exists()
                    || (gitDir.exists() && gitDir.isDirectory() && gitDir.list().length == 0)) {
                gitCloneRepository(repoUrl, gitDir);
            } else {
                if (isGitRepository(gitDir)) {
                    gitPullRepository(gitDir);
                } else {
                    showToast("Destination directory is not empty. Handle this case accordingly.");
                }
            }
        } catch (URISyntaxException err) {
            showToast(err.getMessage());
        }
    }

    private File getGitDirectoryFromUrl(String repoUrl) throws URISyntaxException {
        var uri = new URIish(repoUrl);
        var repoName = uri.getHumanishName();
        return new File(dataDir, repoName);
    }

    private boolean isGitRepository(File directory) {
        File gitDir = new File(directory, ".git");
        return gitDir.exists() && gitDir.isDirectory();
    }

    private void gitAdd(File directory, @Nullable String filePath) {
        executeGitOperationAsync(
                () -> {
                    Git git = Git.open(directory);
                    Status status = git.status().call();
                    if (status.isClean()) {
                        showToast("No changes to add.");
                        SuccessMessage = "No changes to add.";
                        return git;
                    }
                    AddCommand addCommand = git.add();

                    if (filePath != null && !filePath.isEmpty()) {
                        addCommand.addFilepattern(filePath);
                    } else {
                        addCommand.addFilepattern(".");
                    }

                    addCommand.call();
                    SuccessMessage = "Changes added to the staging area.";
                    return git;
                },
                SuccessMessage,
                "Error during 'git add'.");
    }

    private void gitCommit(File directory, @Nullable String message, boolean includeAll) {
        executeGitOperationAsync(
                () -> {
                    Git git = Git.open(directory);
                    Status status = git.status().call();
                    if (status.isClean()) {
                        showToast("No changes to commit.");
                        SuccessMessage = "No changes to commit.";
                        return git;
                    }
                    CommitCommand commitCommand = git.commit();

                    if (includeAll) {
                        commitCommand.setAll(true);
                    }
                    if (message == null || message.isEmpty()) {
                        // If message is null or empty, generate a default message
                        DateTimeFormatter formatter =
                                DateTimeFormatter.ofPattern("yyyy-MM-d h:m:s a");
                        String defaultMessage =
                                "Update data at " + LocalDateTime.now().format(formatter);
                        commitCommand.setMessage(defaultMessage);
                    } else {
                        commitCommand.setMessage(message);
                    }

                    commitCommand.call();
                    SuccessMessage = "Changes committed successfully.";
                    return git;
                },
                SuccessMessage,
                "Error during 'git commit'.");
    }

    private void gitPush(File directory, String branchName) {
        executeGitOperationAsync(
                () -> {
                    Git git = Git.open(directory);
                    CredentialsProvider credentialsProvider =
                            new UsernamePasswordCredentialsProvider(username, password);
                    PushCommand pushCommand = git.push();
                    pushCommand
                            .setRemote("origin")
                            .setRefSpecs(new RefSpec(branchName))
                            .setCredentialsProvider(credentialsProvider)
                            .call();
                    return git;
                },
                "Changes pushed successfully to the 'origin' repository.",
                "Error during 'git push'.");
    }

    private void gitPullRepository(File directory) {
        prog.setTitle("Pulling last commits from repository...");
        executeGitOperationAsync(
                () -> {
                    Git git = Git.open(directory);
                    var pullResult =
                            git.pull()
                                    .setRemoteBranchName("main")
                                    .setProgressMonitor(new SimpleProgressMonitor())
                                    .call();

                    if (!pullResult.isSuccessful()) {
                        throw new Exception("Pull failed. Handle this case accordingly.");
                    }
                    return git;
                },
                directory.getAbsolutePath(),
                "Error during 'git pull'.");
    }

    private void gitCloneRepository(String repositoryURL, File directory) {
        if (repositoryURL.isEmpty()
                || repositoryURL.isBlank()
                || !URLUtil.isValidUrl(repositoryURL)) {
            showToast("Invalid URL");
            return;
        }
        
        prog.setTitle("Cloning repository...");

        executeGitOperationAsync(
                () -> {
                    String url = repositoryURL.trim();
                    if (!url.endsWith(".git")) {
                        url += ".git";
                    }
                    git =
                            Git.cloneRepository()
                                    .setURI(url)
                                    .setDirectory(directory)
                                    .setProgressMonitor(new SimpleProgressMonitor())
                                    .call();
                    return git;
                },
                directory.getAbsolutePath(),
                "Error during repository cloning.");
    }

    private void executeGitOperationAsync(
            Callable<Git> gitOperation, String successMessage, String errorMessage) {

        var task =
                TaskExecutor.executeAsyncProvideError(
                        () -> {
                            Git git = null;
                            try {
                                git = gitOperation.call();
                            } catch (Exception e) {
                                throw new Exception(e);
                            }
                            return git;
                        });

        prog.show();
        task.whenComplete(
                (result, error) -> {
                    ThreadUtils.runOnUiThread(
                            () -> {
                                prog.dismiss();
                                if (result != null && error == null) {
                                    result.close();
                                    showToast(successMessage);
                                    SuccessMessage = "";
                                    binding.outputTextView.setText(output.toString());
                                } else {
                                    handleGitOperationException(error, errorMessage);
                                }
                            });
                });
    }

    private void handleGitOperationException(Throwable error, String errorMessage) {
        showToast(errorMessage);
        binding.outputTextView.setText(error.getMessage());
    }

    public class SimpleProgressMonitor implements ProgressMonitor {
        public boolean cancelled = false;

        public void cancel() {
            cancelled = true;
        }

        @Override
        public void start(int totalTask) {
            output.append("SimpleProgressMonitor has started!" + "\n");
        }

        @Override
        public void beginTask(String title, int totalWork) {
            // output.append("CloneProgressMonitor-beginTask : " + title + "|" + totalWork + "\n");
            ThreadUtils.runOnUiThread(
                    () -> {
                        prog.setMessage(title);
                    });
        }

        @Override
        public void update(int completed) {
            // output.append("CloneProgressMonitor-update : " + completed + "\n");
        }

        @Override
        public void endTask() {
            output.append("SimpleProgressMonitor have been ended!" + "\n");
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.binding = null;
    }

    private void showToast(String msg) {
        ThreadUtils.runOnUiThread(
                () -> {
                    if (msg != null && !msg.isEmpty()) {
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
