package pwr.zpi.hotspotter.repositoryanalysis.logprocessing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.common.exception.LogProcessingException;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.model.Commit;

import java.io.IOException;
import java.io.InputStream;
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

    private static final String GIT_LOG_FORMAT = "[%h] %cd%n%an <%ae>";
    private static final int PROCESS_COMPLETION_TIMEOUT_SECONDS = 30;

    private final LogParser logParser;

    public CommitStream extractAndParseCommits(Path repositoryPath, LocalDate startDate, LocalDate endDate) {
        String startDateStr = getDateString(startDate);
        String endDatePlusOneDayStr = getDatePlusOneDayString(endDate);

        try {
            ProcessBuilder pb = createStreamingProcessBuilder(repositoryPath, startDateStr, endDatePlusOneDayStr);
            Process process = pb.start();

            InputStream inputStream = process.getInputStream();

            Stream<Commit> commits = logParser.parseLogs(inputStream)
                    .onClose(() -> handleProcessCompletion(process));

            return new CommitStream(process, commits);

        } catch (IOException e) {
            throw new LogProcessingException("Failed to start git log process: " + e.getMessage());
        }
    }

    private ProcessBuilder createStreamingProcessBuilder(Path repositoryPath, String sinceDateStr, String untilDateStr) {
        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(repositoryPath.toFile());

        List<String> command = Stream.of(
                    "git", "log",
                    "--pretty=format:" + GIT_LOG_FORMAT,
                    "--date=short",
                    "--numstat",
                    "--reverse",
                    sinceDateStr != null ? "--since=" + sinceDateStr : null,
                    untilDateStr != null ? "--until=" + untilDateStr : null
                ).filter(Objects::nonNull)
                .toList();

        pb.command(command);
        pb.redirectErrorStream(true);

        return pb;
    }

    private void handleProcessCompletion(Process process) {
        try {
            if (process.isAlive()) {
                boolean finished = process.waitFor(PROCESS_COMPLETION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (!finished) {
                    process.destroyForcibly();
                    throw new LogProcessingException("Git log process did not complete in time");
                }
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new LogProcessingException("Git log process failed with exit code: " + exitCode);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LogProcessingException("Interrupted while waiting for git log process");
        }
    }

    private String getDateString(LocalDate date) {
        return (date != null) ? date.format(DateTimeFormatter.ISO_LOCAL_DATE) : null;
    }

    private String getDatePlusOneDayString(LocalDate date) {
        return (date != null) ? date.plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE) : null;
    }

}
