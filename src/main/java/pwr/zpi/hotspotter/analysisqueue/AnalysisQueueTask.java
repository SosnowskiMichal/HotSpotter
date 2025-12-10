package pwr.zpi.hotspotter.analysisqueue;

import lombok.Getter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Getter
public class AnalysisQueueTask {

    private final String taskId;
    private final String repositoryUrl;
    private final LocalDate endDate;
    private final SseEmitter emitter;
    private final Runnable analysisTask;

    public AnalysisQueueTask(String repositoryUrl, LocalDate endDate, SseEmitter emitter, Runnable analysisTask) {
        this.taskId = UUID.randomUUID().toString();
        this.repositoryUrl = repositoryUrl;
        this.endDate = endDate != null ? endDate : LocalDate.now();
        this.emitter = emitter;
        this.analysisTask = analysisTask;
    }

    public AnalysisQueueTask(String repositoryUrl, LocalDate endDate, Runnable analysisTask) {
        this.taskId = UUID.randomUUID().toString();
        this.repositoryUrl = repositoryUrl;
        this.endDate = endDate != null ? endDate : LocalDate.now();
        this.emitter = null;
        this.analysisTask = analysisTask;
    }

}
