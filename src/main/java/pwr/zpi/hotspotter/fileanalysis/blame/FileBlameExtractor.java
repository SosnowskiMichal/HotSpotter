package pwr.zpi.hotspotter.fileanalysis.blame;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.common.exception.AnalysisException;
import pwr.zpi.hotspotter.fileanalysis.blame.model.FileAuthorStatistics;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileBlameExtractor {

    private final FileBlameParser fileBlameParser;

    public List<FileAuthorStatistics> extractCurrentAuthors(Path filePath) {
        log.info("Extracting current authors statistics for file: {}", filePath);

        if (!Files.exists(filePath)) {
            throw new AnalysisException("File does not exist: " + filePath);
        }

        try {
            long totalLines;
            try (Stream<String> lines = Files.lines(filePath)) {
                totalLines = lines.count();
                if (totalLines == 0) {
                    log.warn("File {} is empty, returning empty current authors list", filePath);
                    return List.of();
                }
            }

            ProcessBuilder pb = createProcessBuilder(filePath);
            Process process = pb.start();

            InputStream inputStream = process.getInputStream();
            List<FileAuthorStatistics> result = fileBlameParser.parseBlameOutput(inputStream, totalLines);

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new AnalysisException("Git blame process failed for file: " + filePath);
            }

            log.info("Successfully extracted {} current authors for file: {}", result.size(), filePath);
            return result;

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new AnalysisException("Failed to extract current authors for file " + filePath + ": " + e.getMessage());
        }
    }

    private ProcessBuilder createProcessBuilder(Path filePath) {
        ProcessBuilder pb = new ProcessBuilder(
                "git", "blame", "--line-porcelain", filePath.getFileName().toString()
        );
        pb.directory(filePath.getParent().toFile());
        pb.redirectErrorStream(true);

        return pb;
    }

}
