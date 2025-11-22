package pwr.zpi.hotspotter.repositoryanalysis.queue;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.repositoryanalysis.exception.AnalysisException;
import pwr.zpi.hotspotter.repositoryanalysis.service.AsyncRepositoryAnalysisService;
import pwr.zpi.hotspotter.repositoryanalysis.sse.AnalysisSseStatus;
import pwr.zpi.hotspotter.repositoryanalysis.sse.RepositoryAnalysisSsePublisher;
import pwr.zpi.hotspotter.repositorymanagement.model.RepositoryInfo;
import pwr.zpi.hotspotter.repositorymanagement.service.RepositoryManagementService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Component
public class RepositoryAnalysisQueue {

    private final ExecutorService executorService;
    private final RepositoryAnalysisSsePublisher ssePublisher;
    private final RepositoryManagementService repositoryManagementService;

    private final ConcurrentHashMap<String, BlockingQueue<QueuedAnalysisTask>> repositoryQueues;
    private final ConcurrentHashMap<String, AtomicBoolean> repositoryProcessingFlags;
    private final ConcurrentHashMap<String, LocalDate> currentProcessingDates;
    private final ConcurrentHashMap<String, AtomicInteger> runningTasksCount;
    private final ConcurrentHashMap<String, Phaser> dateCompletionPhasers;
    private final ConcurrentHashMap<String, RepositoryInfo> repositoryInfoCache;

    public RepositoryAnalysisQueue(
            @Qualifier("analysisQueueExecutor") Executor analysisQueueExecutor,
            RepositoryAnalysisSsePublisher ssePublisher,
            RepositoryManagementService repositoryManagementService
    ) {
        this.executorService = ((ThreadPoolTaskExecutor) analysisQueueExecutor).getThreadPoolExecutor();
        this.ssePublisher = ssePublisher;
        this.repositoryManagementService = repositoryManagementService;

        this.repositoryQueues = new ConcurrentHashMap<>();
        this.repositoryProcessingFlags = new ConcurrentHashMap<>();
        this.currentProcessingDates = new ConcurrentHashMap<>();
        this.runningTasksCount = new ConcurrentHashMap<>();
        this.dateCompletionPhasers = new ConcurrentHashMap<>();
        this.repositoryInfoCache = new ConcurrentHashMap<>();
    }

    public void submitAnalysis(
            String repositoryUrl,
            LocalDate startDate,
            LocalDate endDate,
            SseEmitter emitter,
            AsyncRepositoryAnalysisService asyncRepositoryAnalysisService
    ) {
        Runnable analysisTask = () -> {
            RepositoryInfo repositoryInfo = repositoryInfoCache.get(repositoryUrl);
            if (repositoryInfo != null) {
                asyncRepositoryAnalysisService.runRepositoryAnalysis(repositoryInfo, startDate, endDate, emitter);
            } else {
                log.error("RepositoryInfo not found in cache for repository: {}", repositoryUrl);
                throw new AnalysisException("Repository information not available");
            }
        };

        QueuedAnalysisTask task = new QueuedAnalysisTask(endDate, emitter, analysisTask);
        log.info("Submitting analysis task {} for repository: {} with endDate: {}",
                task.getTaskId(), repositoryUrl, endDate);

        BlockingQueue<QueuedAnalysisTask> queue = repositoryQueues.computeIfAbsent(
                repositoryUrl, _ -> new LinkedBlockingQueue<>()
        );

        boolean shouldRunImmediately = false;
        synchronized (repositoryUrl.intern()) {
            LocalDate processingDate = currentProcessingDates.get(repositoryUrl);
            Phaser phaser = dateCompletionPhasers.get(repositoryUrl);

            if (processingDate != null && processingDate.equals(task.getEndDate()) &&
                queue.isEmpty() &&
                phaser != null && !phaser.isTerminated()
            ) {
                phaser.register();
                shouldRunImmediately = true;

                AtomicInteger runningCount = runningTasksCount.get(repositoryUrl);
                if (runningCount != null) {
                    runningCount.incrementAndGet();
                }
            }
        }

        if (shouldRunImmediately) {
            log.info("Processing task {} immediately with current batch for repository: {} with endDate: {}",
                    task.getTaskId(), repositoryUrl, task.getEndDate());
            ssePublisher.sendProgress(emitter, AnalysisSseStatus.QUEUED);
            executeTask(repositoryUrl, task);

        } else {
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
                QueuedAnalysisTask firstTask = queue.peek();
                if (firstTask == null) {
                    log.debug("Queue empty for repository: {}", repositoryUrl);
                    break;
                }

                LocalDate batchDate = firstTask.getEndDate();
                List<QueuedAnalysisTask> batch = new ArrayList<>();

                synchronized (repositoryUrl.intern()) {
                    currentProcessingDates.put(repositoryUrl, batchDate);

                    while (true) {
                        QueuedAnalysisTask task = queue.peek();
                        if (task == null || !task.getEndDate().equals(batchDate)) {
                            break;
                        }
                        batch.add(queue.poll());
                    }

                    if (batch.isEmpty()) {
                        currentProcessingDates.remove(repositoryUrl);
                        continue;
                    }

                    log.info("Preparing repository for batch of {} tasks for repository: {} with endDate: {}",
                            batch.size(), repositoryUrl, batchDate);

                    RepositoryInfo repositoryInfo;
                    try {
                        SseEmitter firstEmitter = batch.getFirst().getEmitter();
                        repositoryInfo = repositoryManagementService.cloneOrUpdateRepository(repositoryUrl, firstEmitter);
                        repositoryInfoCache.put(repositoryUrl, repositoryInfo);

                    } catch (Exception e) {
                        log.error("Failed to prepare repository: {}", repositoryUrl, e);
                        failEntireBatch(batch, "Failed to clone/update repository: " + e.getMessage());
                        currentProcessingDates.remove(repositoryUrl);
                        continue;
                    }

                    Path repositoryPath = Path.of(repositoryInfo.getLocalPath());
                    boolean needsRestoration = !batchDate.equals(LocalDate.now());
                    if (needsRestoration) {
                        try {
                            restoreRepositoryToDate(repositoryPath, batchDate);
                        } catch (Exception e) {
                            log.error("Failed to restore repository {} to date {}: {}", repositoryUrl, batchDate, e.getMessage());
                            failEntireBatch(batch, "Failed to restore repository to date " + batchDate + ": " + e.getMessage());
                            currentProcessingDates.remove(repositoryUrl);
                            continue;
                        }
                    }

                    AtomicInteger runningCount = new AtomicInteger(batch.size());
                    runningTasksCount.put(repositoryUrl, runningCount);

                    Phaser phaser = new Phaser();
                    phaser.bulkRegister(batch.size());
                    dateCompletionPhasers.put(repositoryUrl, phaser);

                    log.info("Executing batch of {} tasks for repository: {} with endDate: {}",
                            batch.size(), repositoryUrl, batchDate);

                    for (QueuedAnalysisTask task : batch) {
                        executeTask(repositoryUrl, task);
                    }
                }

                try {
                    Phaser phaser = dateCompletionPhasers.get(repositoryUrl);
                    if (phaser != null) {
                        log.debug("Waiting for batch completion for repository: {} with endDate: {}", repositoryUrl, batchDate);
                        phaser.awaitAdvance(0);
                        log.info("Batch completed for repository: {} with endDate: {}", repositoryUrl, batchDate);
                    }

                } catch (Exception e) {
                    log.error("Error while waiting for batch completion for repository: {}", repositoryUrl, e);
                    break;
                }

                boolean needsRestoration = !batchDate.equals(LocalDate.now());
                if (needsRestoration) {
                    RepositoryInfo repositoryInfo = repositoryInfoCache.get(repositoryUrl);
                    if (repositoryInfo != null) {
                        Path repositoryPath = Path.of(repositoryInfo.getLocalPath());
                        try {
                            restoreRepositoryToLatest(repositoryPath);
                        } catch (Exception e) {
                            log.error("Failed to restore repository {} to latest state: {}", repositoryUrl, e.getMessage());
                        }
                    }
                }

                synchronized (repositoryUrl.intern()) {
                    currentProcessingDates.remove(repositoryUrl);
                    runningTasksCount.remove(repositoryUrl);
                    dateCompletionPhasers.remove(repositoryUrl);
                }
            }

        } finally {
            repositoryProcessingFlags.get(repositoryUrl).set(false);
            repositoryInfoCache.remove(repositoryUrl);

            if (queue.isEmpty()) {
                log.info("Cleaning up empty queue for repository: {}", repositoryUrl);
                repositoryQueues.remove(repositoryUrl);
                repositoryProcessingFlags.remove(repositoryUrl);
            }
        }
    }

    private void executeTask(String repositoryUrl, QueuedAnalysisTask task) {
        executorService.submit(() -> {
            log.debug("Executing task {} for repository: {}", task.getTaskId(), repositoryUrl);
            try {
                task.getAnalysisTask().run();
                log.debug("Task {} completed successfully for repository: {}", task.getTaskId(), repositoryUrl);

            } catch (Exception e) {
                log.error("Task {} failed for repository: {}", task.getTaskId(), repositoryUrl, e);

            } finally {
                AtomicInteger runningCount = runningTasksCount.get(repositoryUrl);
                Phaser phaser = dateCompletionPhasers.get(repositoryUrl);

                if (runningCount != null) {
                    int remaining = runningCount.decrementAndGet();
                    log.debug("Task {} finished, {} tasks remaining for current batch for repository: {}",
                            task.getTaskId(), remaining, repositoryUrl);
                }

                if (phaser != null && !phaser.isTerminated()) {
                    try {
                        phaser.arrive();
                    } catch (IllegalStateException e) {
                        log.warn("Phaser already terminated when task {} completed", task.getTaskId());
                    }
                }
            }
        });
    }

    private void failEntireBatch(List<QueuedAnalysisTask> batch, String errorMessage) {
        log.error("Failing entire batch of {} tasks: {}", batch.size(), errorMessage);
        for (QueuedAnalysisTask task : batch) {
            try {
                ssePublisher.sendError(task.getEmitter(), errorMessage);
                task.getEmitter().complete();
            } catch (Exception e) {
                log.warn("Failed to send error to emitter for task {}: {}", task.getTaskId(), e.getMessage());
            }
        }
    }

    private void restoreRepositoryToDate(Path repositoryPath, LocalDate endDate) {
        log.debug("Restoring repository {} to {}", repositoryPath, endDate);
        LocalDate beforeDate = endDate.plusDays(1);

        try {
            int exitCode = executeGitCheckout(repositoryPath, beforeDate);
            if (exitCode != 0) {
                throw new AnalysisException("Failed to restore repository to date " + endDate);
            }
            log.debug("Repository {} restored to {}", repositoryPath, endDate);

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new AnalysisException("Failed to restore repository to date " + endDate);
        }
    }

    private int executeGitCheckout(Path repositoryPath, LocalDate beforeDate) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                "bash", "-c",
                "git checkout $(git rev-list -1 --before=\"" + beforeDate + "\" HEAD)"
        );
        pb.directory(repositoryPath.toFile());
        pb.redirectErrorStream(true);

        return executeProcess(pb);
    }

    private void restoreRepositoryToLatest(Path repositoryPath) {
        log.debug("Restoring repository {} to latest", repositoryPath);

        try {
            int exitCode = executeGitCheckoutLatest(repositoryPath);
            if (exitCode != 0) {
                log.error("Failed to restore repository {} to the latest state", repositoryPath);
            } else {
                log.debug("Repository {} restored to the latest state", repositoryPath);
            }

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            log.error("Failed to restore repository {} to the latest state", repositoryPath, e);
        }
    }

    private int executeGitCheckoutLatest(Path repositoryPath) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                "bash", "-c",
                "git branch | grep -q '^* (HEAD detached' && git checkout $(git branch | grep -v '^*' | tr -d ' ')"
        );
        pb.directory(repositoryPath.toFile());
        pb.redirectErrorStream(true);

        return executeProcess(pb);
    }

    private int executeProcess(ProcessBuilder pb) throws IOException, InterruptedException {
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            while (reader.readLine() != null) {}
        }

        return process.waitFor();
    }

    public boolean isRepositoryInUse(String repositoryUrl) {
        boolean hasQueue = repositoryQueues.containsKey(repositoryUrl);
        boolean isProcessing = repositoryProcessingFlags.containsKey(repositoryUrl) &&
                repositoryProcessingFlags.get(repositoryUrl).get();
        boolean hasRunningTasks = runningTasksCount.containsKey(repositoryUrl) &&
                runningTasksCount.get(repositoryUrl).get() > 0;

        return hasQueue || isProcessing || hasRunningTasks;
    }

}
