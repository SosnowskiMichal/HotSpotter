package pwr.zpi.hotspotter.repositoryanalysis.queue;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.repositoryanalysis.sse.AnalysisSseStatus;
import pwr.zpi.hotspotter.repositoryanalysis.sse.RepositoryAnalysisSsePublisher;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Component
public class RepositoryAnalysisQueue {

    private final ExecutorService executorService;
    private final RepositoryAnalysisSsePublisher ssePublisher;

    private final ConcurrentHashMap<String, BlockingQueue<QueuedAnalysisTask>> repositoryQueues;
    private final ConcurrentHashMap<String, AtomicBoolean> repositoryProcessingFlags;

    public RepositoryAnalysisQueue(
            @Qualifier("analysisQueueExecutor") Executor analysisQueueExecutor,
            RepositoryAnalysisSsePublisher ssePublisher
    ) {
        this.executorService = ((ThreadPoolTaskExecutor) analysisQueueExecutor).getThreadPoolExecutor();
        this.ssePublisher = ssePublisher;

        this.repositoryQueues = new ConcurrentHashMap<>();
        this.repositoryProcessingFlags = new ConcurrentHashMap<>();
    }

    public void submitAnalysis(String repositoryUrl, Runnable analysisTask, SseEmitter emitter) {
        QueuedAnalysisTask task = new QueuedAnalysisTask(analysisTask);
        log.info("Submitting analysis task {} for repository: {}", task.getTaskId(), repositoryUrl);

        BlockingQueue<QueuedAnalysisTask> queue = repositoryQueues.computeIfAbsent(
                repositoryUrl, _ -> new LinkedBlockingQueue<>()
        );
        queue.offer(task);

        log.debug("Task {} queued for repository: {}", task.getTaskId(), repositoryUrl);
        ssePublisher.sendProgress(emitter, AnalysisSseStatus.QUEUED);

        AtomicBoolean isProcessing = repositoryProcessingFlags.computeIfAbsent(
                repositoryUrl, _ -> new AtomicBoolean(false)
        );

        if (isProcessing.compareAndSet(false, true)) {
            log.debug("Starting queue processing for repository: {}", repositoryUrl);
            executorService.submit(() -> processQueue(repositoryUrl));
        } else {
            log.debug("Queue processing already running for repository: {}", repositoryUrl);
        }
    }

    private void processQueue(String repositoryUrl) {
        log.info("Queue processing started for repository: {}", repositoryUrl);

        BlockingQueue<QueuedAnalysisTask> queue = repositoryQueues.get(repositoryUrl);
        if (queue == null) {
            log.warn("No queue found for repository: {}", repositoryUrl);
            repositoryProcessingFlags.get(repositoryUrl).set(false);
            return;
        }

        try {
            while (true) {
                QueuedAnalysisTask task = queue.poll();
                if (task == null) {
                    log.debug("Queue empty for repository: {}", repositoryUrl);
                    break;
                }

                log.debug("Processing task {} for repository: {}", task.getTaskId(), repositoryUrl);
                try {
                    task.getAnalysisTask().run();
                    log.info("Task {} completed successfully for repository: {}", task.getTaskId(), repositoryUrl);
                } catch (Exception e) {
                    log.error("Task {} failed for repository: {}", task.getTaskId(), repositoryUrl, e);
                }
            }

        } finally {
            repositoryProcessingFlags.get(repositoryUrl).set(false);

            if (queue.isEmpty()) {
                log.info("Cleaning up empty queue for repository: {}", repositoryUrl);
                repositoryQueues.remove(repositoryUrl);
                repositoryProcessingFlags.remove(repositoryUrl);
            }
        }
    }

    public boolean isRepositoryInUse(String repositoryUrl) {
        boolean hasQueue = repositoryQueues.containsKey(repositoryUrl);
        boolean isProcessing = repositoryProcessingFlags.containsKey(repositoryUrl) &&
                repositoryProcessingFlags.get(repositoryUrl).get();

        return hasQueue || isProcessing;
    }

}
