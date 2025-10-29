package pwr.zpi.hotspotter.repositorymanagement.service.command;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class CommandExecutor {

    public CommandResult executeCommand(ProcessBuilder pb, int monitoringInterval) {
        try {
            Process process = pb.start();
            Thread monitoringThread = startProgressMonitoringThread(process, monitoringInterval);

            try {
                int exitCode = process.waitFor();
                monitoringThread.join();

                boolean success = exitCode == 0;
                return new CommandResult(success, exitCode);

            } finally {
                process.destroyForcibly();
            }

        } catch (Exception e) {
            log.error("Error executing command {}: {}", pb.command().toString(), e.getMessage(), e);
            return new CommandResult(false, -1);
        }
    }

    private Thread startProgressMonitoringThread(Process process, int monitoringInterval) {
        Thread thread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line, lastProgressLine = null;

                while (process.isAlive()) {
                    while (reader.ready() && (line = reader.readLine()) != null) {
                        if (isProgressInfoLine(line)) {
                            lastProgressLine = line;
                        }
                    }

                    if (lastProgressLine != null) {
                        log.info("Git process progress: {}", lastProgressLine.trim());
                        lastProgressLine = null;
                    }

                    boolean finished = process.waitFor(monitoringInterval, TimeUnit.SECONDS);
                    if (finished) break;
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.debug("Monitoring thread interrupted");
            } catch (Exception e) {
                log.error("Error monitoring git process: {}", e.getMessage());
            }
        }, "git-progress-monitoring-thread");

        thread.start();
        return thread;
    }

    private boolean isProgressInfoLine(String line) {
        return line.contains("%");
    }

    public record CommandResult(boolean success, int exitCode) { }

}
