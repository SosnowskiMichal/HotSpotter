package pwr.zpi.hotspotter.repositoryanalysis.queue;

import lombok.Getter;

import java.util.UUID;

@Getter
public class QueuedAnalysisTask {

    private final String taskId;
    private final Runnable analysisTask;

    public QueuedAnalysisTask(Runnable analysisTask) {
        this.taskId = UUID.randomUUID().toString();
        this.analysisTask = analysisTask;
    }

}
