package pwr.zpi.hotspotter.repositorymanagement.operation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.repositoryanalysis.exception.AnalysisException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.time.LocalDate;

@Slf4j
@Component
public class RepositoryStateManager {

    public void restoreRepositoryToDate(Path repositoryPath, LocalDate endDate) {
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

    public void restoreRepositoryToLatest(Path repositoryPath) {
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

    private int executeGitCheckout(Path repositoryPath, LocalDate beforeDate) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                "bash", "-c",
                "git checkout $(git rev-list -1 --before=\"" + beforeDate + "\" HEAD)"
        );
        pb.directory(repositoryPath.toFile());
        pb.redirectErrorStream(true);

        return executeProcess(pb);
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

}
