package pwr.zpi.hotspotter.repositorymanagement.service.operation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.lib.ProgressMonitor;

@Slf4j
@RequiredArgsConstructor
public class ProcessProgressMonitor implements ProgressMonitor {

    private String currentTask;
    private int totalWork;
    private int completed;
    private int lastLoggedPercentage = -1;
    private final int logIntervalPercentage;

    @Override
    public void start(int totalTasks) {
        log.debug("Starting process with {} tasks", totalTasks);
    }

    @Override
    public void beginTask(String title, int totalWork) {
        this.currentTask = title;
        this.totalWork = totalWork;
        this.completed = 0;
        this.lastLoggedPercentage = -1;

        if (totalWork > 0) {
            log.debug("Process started: {} (Total work: {})", title, totalWork);
        }
    }

    @Override
    public void update(int completed) {
        this.completed += completed;
        logProgressIfNeeded();
    }

    @Override
    public void endTask() {
        if (totalWork > 0) {
            log.debug("Process finished: {} (Total work: {} (100%))", currentTask, totalWork);
        }

        this.currentTask = null;
        this.totalWork = 0;
        this.completed = 0;
        this.lastLoggedPercentage = -1;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public void showDuration(boolean b) { }

    private void logProgressIfNeeded() {
        if (totalWork <= 0) return;

        int currentPercentage = (int) ((completed * 100.0) / totalWork);
        if (currentPercentage >= lastLoggedPercentage + logIntervalPercentage) {
            log.info("Git operation progress: {} ({}%)", currentTask, currentPercentage);
            lastLoggedPercentage = currentPercentage;
        }
    }

}
