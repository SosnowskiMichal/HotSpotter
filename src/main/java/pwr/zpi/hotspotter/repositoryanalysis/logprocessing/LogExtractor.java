package pwr.zpi.hotspotter.repositoryanalysis.logprocessing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.config.LogExtractorConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogExtractor {

    private static final String GIT_LOG_FORMAT = "[%h] %ad%n%an <%ae>";
    private static final int LOG_PROCESS_MONITORING_INTERVAL = 30;
    private static final int LOG_PROCESS_TIMEOUT_MINUTES = 15;

    private final LogExtractorConfig logExtractorConfig;

    public LogExtractionResult extractLogs(Path repositoryPath, String analysisId, LocalDate startDate, LocalDate endDate) {
        Path logFilePath = repositoryPath.resolve(logExtractorConfig.getLogDirectoryName()).resolve(analysisId + ".log");

        if (Files.exists(logFilePath)) {
            return LogExtractionResult.success("Log file already exists.", logFilePath);
        }

        Path logDirPath = repositoryPath.resolve(logExtractorConfig.getLogDirectoryName());
        if (!createLogDirectory(logDirPath)) {
            return LogExtractionResult.failure("Failed to create log directory.");
        }

        String startDateStr = getDateString(startDate);
        String endDatePlusOneDayStr = getDatePlusOneDayString(endDate);

        boolean success = executeLogCommand(repositoryPath, logFilePath, startDateStr, endDatePlusOneDayStr);
        return success ?
                LogExtractionResult.success("Log extraction completed successfully.", logFilePath) :
                LogExtractionResult.failure("Error executing git log command.");
    }

    public void deleteLogFile(Path logFilePath) {
        try {
            Files.deleteIfExists(logFilePath);
            log.info("Deleted log file: {}", logFilePath);
        } catch (IOException e) {
            log.error("Error deleting log file {}: {}", logFilePath, e.getMessage());
        }
    }

    private String getDateString(LocalDate date) {
        return (date != null) ? date.format(DateTimeFormatter.ISO_LOCAL_DATE) : null;
    }

    private String getDatePlusOneDayString(LocalDate date) {
        return (date != null) ? date.plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE) : null;
    }

    private boolean createLogDirectory(Path logDirPath) {
        try {
            FileUtils.forceMkdir(logDirPath.toFile());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean executeLogCommand(Path repositoryPath, Path logFilePath, String afterDateStr, String beforeDateStr) {
        Process process = null;
        Thread monitoringThread = null;

        try {
            ProcessBuilder pb = createProcessBuilder(repositoryPath, logFilePath, afterDateStr, beforeDateStr);
            process = pb.start();
            monitoringThread = startLogFileSizeMonitoringThread(process, logFilePath);

            boolean finished = process.waitFor(LOG_PROCESS_TIMEOUT_MINUTES, TimeUnit.MINUTES);
            if (!finished) {
                log.error("Log extraction timed out after {} minutes", LOG_PROCESS_TIMEOUT_MINUTES);
                process.destroyForcibly();
                monitoringThread.interrupt();
                Files.deleteIfExists(logFilePath);
                return false;
            }

            monitoringThread.interrupt();
            monitoringThread.join(5000);

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                Files.deleteIfExists(logFilePath);
                log.error("Git log command failed with exit code: {}", exitCode);
                return false;
            }
            return true;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Log extraction interrupted for {}: {}", repositoryPath, e.getMessage());
            cleanupResources(process, monitoringThread, logFilePath);
            return false;

        } catch (IOException e) {
            log.error("Error executing git log command for {}: {}", repositoryPath, e.getMessage());
            cleanupResources(process, monitoringThread, logFilePath);
            return false;
        }
    }

    private ProcessBuilder createProcessBuilder(Path repositoryPath, Path logFilePath, String afterDateStr, String beforeDateStr) {
        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(repositoryPath.toFile());

        List<String> command = Stream.of(
                    "git", "log",
                    "--pretty=format:" + GIT_LOG_FORMAT,
                    "--date=short",
                    "--numstat",
                    "--reverse",
                    afterDateStr != null ? "--after=" + afterDateStr : null,
                    beforeDateStr != null ? "--before=" + beforeDateStr : null
            ).filter(Objects::nonNull)
            .toList();

        pb.command(command);
        pb.redirectErrorStream(true);
        pb.redirectOutput(logFilePath.toFile());
        return pb;
    }

    private Thread startLogFileSizeMonitoringThread(Process process, Path logFilePath) {
        Thread thread = new Thread(() -> {
            try {
                while (process.isAlive()) {
                    long fileSize = Files.size(logFilePath);
                    log.info("Log file size: {} bytes.", fileSize);

                    boolean finished = process.waitFor(LOG_PROCESS_MONITORING_INTERVAL, TimeUnit.SECONDS);
                    if (finished) break;
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Monitoring thread interrupted");
            } catch (IOException e) {
                log.error("Error monitoring log file size: {}", e.getMessage());
            }
        }, "git-log-file-size-monitoring-thread");

        thread.start();
        return thread;
    }

    private void cleanupResources(Process process, Thread monitoringThread, Path logFilePath) {
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
        }

        if (monitoringThread != null && monitoringThread.isAlive()) {
            monitoringThread.interrupt();
        }

        deleteLogFile(logFilePath);
    }

    public record LogExtractionResult(boolean success, String message, Path logFilePath) {
        public static LogExtractionResult success(String message, Path logFilePath) {
            return new LogExtractionResult(true, message, logFilePath);
        }

        public static LogExtractionResult failure(String message) {
            return new LogExtractionResult(false, message, null);
        }
    }

}
