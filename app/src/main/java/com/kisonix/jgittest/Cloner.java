package com.kisonix.jgittest;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.io.File;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ProgressMonitor;

public class Cloner {
    private static final String TAG = "Cloner";

    public interface CloneTaskListener {
        void onTaskStart(int totalTasks);

        void onTaskBegin(String title, int totalWork);

        void onTaskUpdate(int completed);

        void onTaskEnd(boolean isSuccess, String message);

        boolean isTaskCancelled();
    }

    public static Disposable cloneRepository(
            String repositoryURL, String destinationDirectory, CloneTaskListener listener) {
        return Observable.create(
                        emitter -> {
                            class CloningProgressMonitor implements ProgressMonitor {

                                @Override
                                public void start(int totalTasks) {
                                    listener.onTaskStart(totalTasks);
                                }

                                @Override
                                public void beginTask(String title, int totalWork) {
                                    listener.onTaskBegin(title, totalWork);
                                }

                                @Override
                                public void update(int completed) {
                                    emitter.onNext(completed);
                                }

                                @Override
                                public void endTask() {
                                    // FIXME: idk
                                }

                                @Override
                                public boolean isCancelled() {
                                    return emitter.isDisposed();
                                }
                            }

                            CloningProgressMonitor progressMonitor = new CloningProgressMonitor();

                            try {
                                CloneCommand cloneCommand =
                                        Git.cloneRepository()
                                                .setURI(repositoryURL)
                                                .setDirectory(new File(destinationDirectory))
                                                .setProgressMonitor(progressMonitor);

                                Git git = cloneCommand.call();
                                emitter.onComplete();
                            } catch (Exception e) {
                                emitter.onError(e);
                            }
                        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        progress -> {
                            if (listener != null) {
                                listener.onTaskUpdate((int) progress);
                            }
                        },
                        throwable -> {
                            if (listener != null) {
                                listener.onTaskEnd(false, throwable.getMessage());
                            }
                        },
                        () -> {
                            if (listener != null) {
                                listener.onTaskEnd(true, null);
                            }
                        });
    }
}
