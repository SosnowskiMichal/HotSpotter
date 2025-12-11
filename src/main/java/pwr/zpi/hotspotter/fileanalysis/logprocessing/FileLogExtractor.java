package pwr.zpi.hotspotter.fileanalysis.logprocessing;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pwr.zpi.hotspotter.common.exception.LogProcessingException;
import pwr.zpi.hotspotter.fileanalysis.logprocessing.model.FileCommit;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class FileLogExtractor {

    private static final String FILE_GIT_LOG_FORMAT = "[%h] %cd %an <%ae>";

    private final FileLogParser fileLogParser;

    public List<FileCommit> extractAndParseFileCommits(
            Path repositoryPath,
            String filePath,
            LocalDate startDate,
            LocalDate endDate
    ) {
        LocalDate endDatePlusOneDay = endDate != null ? endDate.plusDays(1) : null;

        try {
            ProcessBuilder pb = createProcessBuilder(repositoryPath, filePath, startDate, endDatePlusOneDay);
            Process process = pb.start();

            InputStream inputStream = process.getInputStream();
            List<FileCommit> result = fileLogParser.parseFileLogs(inputStream);

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new LogProcessingException("Git log process failed with exit code: " + exitCode);
            }

            Collections.reverse(result);
            return result;

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new LogProcessingException("Failed to extract commits for file " + filePath + ": " + e.getMessage());
        }
    }

    private ProcessBuilder createProcessBuilder(
            Path repositoryPath,
            String filePath,
            LocalDate sinceDate,
            LocalDate untilDate
    ) {
        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(repositoryPath.toFile());

        List<String> command = Stream.of(
                    "git", "log",
                    "--pretty=format:" + FILE_GIT_LOG_FORMAT,
                    "--numstat",
                    "--date=short",
                    "--follow",
                    sinceDate != null ? "--since=" + sinceDate.format(DateTimeFormatter.ISO_LOCAL_DATE) : null,
                    untilDate != null ? "--until=" + untilDate.format(DateTimeFormatter.ISO_LOCAL_DATE) : null,
                    "--", filePath
                )
                .filter(Objects::nonNull)
                .toList();

        pb.command(command);
        pb.redirectErrorStream(true);

        return pb;
    }

}
