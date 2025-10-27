package pwr.zpi.hotspotter.repositoryanalysis.logparser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.repositoryanalysis.logparser.config.LogExtractorConfig;

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

    private final LogExtractorConfig logExtractorConfig;

    public LogExtractionResult extractLogs(Path repositoryPath, LocalDate afterDate, LocalDate beforeDate) {
        String afterDateStr = getDateString(afterDate);
        String beforeDateStr = getDateString(beforeDate);

        String logFileName = getLogFileName(afterDateStr, beforeDateStr);
        Path logFilePath = repositoryPath.resolve(logExtractorConfig.getLogDirectoryName()).resolve(logFileName);

        if (Files.exists(logFilePath)) {
            return LogExtractionResult.success(logFilePath);
        }

        Path logDirPath = repositoryPath.resolve(logExtractorConfig.getLogDirectoryName());
        if (!createLogDirectory(logDirPath)) {
            return LogExtractionResult.failure("Failed to create log directory.");
        }

        boolean success = executeLogCommand(repositoryPath, logFilePath, afterDateStr, beforeDateStr);
        return success ?
                LogExtractionResult.success(logFilePath) :
                LogExtractionResult.failure("Error executing git log command.");
    }

    private String getDateString(LocalDate date) {
        return (date != null) ? date.format(DateTimeFormatter.ISO_LOCAL_DATE) : null;
    }

    private String getLogFileName(String afterDateStr, String beforeDateStr) {
        return "log--" + (afterDateStr != null ? afterDateStr : "_") + "--"
                + (beforeDateStr != null ? beforeDateStr : "_") + ".log";
    }

    private boolean createLogDirectory(Path logDirPath) {
        try {
            FileUtils.forceMkdir(logDirPath.toFile());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean executeLogCommand(
            Path repositoryPath,
            Path logFilePath,
            String afterDateStr,
            String beforeDateStr) {

        try {
            ProcessBuilder pb = createProcessBuilder(repositoryPath, logFilePath, afterDateStr, beforeDateStr);
            Process process = pb.start();
            Thread monitoringThread = startLogFileSizeMonitoringThread(process, logFilePath);

            monitoringThread.join();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                Files.deleteIfExists(logFilePath);
                log.error("Git log command failed with exit code: {}", exitCode);
                return false;
            }
            return true;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Log extraction interrupted for {}: {}", repositoryPath, e.getMessage());
            return false;

        } catch (IOException e) {
            log.error("Error executing git log command for {}: {}", repositoryPath, e.getMessage());
            return false;
        }
    }

    private ProcessBuilder createProcessBuilder(
            Path repositoryPath,
            Path logFilePath,
            String afterDateStr,
            String beforeDateStr) {

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

    public record LogExtractionResult(boolean success, String message, Path logFilePath) {
        public static LogExtractionResult success(Path logFilePath) {
            return new LogExtractionResult(true, null, logFilePath);
        }

        public static LogExtractionResult failure(String message) {
            return new LogExtractionResult(false, message, null);
        }
    }

}
