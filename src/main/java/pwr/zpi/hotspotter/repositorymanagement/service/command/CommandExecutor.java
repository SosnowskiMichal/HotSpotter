package pwr.zpi.hotspotter.repositorymanagement.service.command;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class CommandExecutor {

    private final Thread gitShutdownHook = new Thread(() -> {
        log.info("Git shutdown hook triggered, terminating all git processes");
        ProcessHandle.allProcesses()
                .filter(ph -> ph.info().command().isPresent()
                        && ph.info().command().get().toLowerCase().contains("git"))
                .forEach(ProcessHandle::destroyForcibly);
    });

    public CommandResult executeCommand(ProcessBuilder pb, int monitoringInterval) {
        try {
            Process process = pb.start();
            startProgressMonitoringThread(process, monitoringInterval);
            Runtime.getRuntime().addShutdownHook(gitShutdownHook);

            try {
                int exitCode = process.waitFor();
                boolean success = exitCode == 0;
                return new CommandResult(success, exitCode);

            } finally {
                process.destroyForcibly();
                Runtime.getRuntime().removeShutdownHook(gitShutdownHook);
            }

        } catch (Exception e) {
            log.error("Error executing command {}: {}", pb.command().toString(), e.getMessage(), e);
            return new CommandResult(false, -1);
        }
    }

    private void startProgressMonitoringThread(Process process, int monitoringInterval) {
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
    }

    private boolean isProgressInfoLine(String line) {
        return line.contains("%");
    }

    public record CommandResult(boolean success, int exitCode) { }

}
