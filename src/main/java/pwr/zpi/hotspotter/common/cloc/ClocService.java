package pwr.zpi.hotspotter.common.cloc;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import pwr.zpi.hotspotter.common.cloc.model.FileLinesData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class ClocService {

    @Async("repositoryAnalysisExecutor")
    public CompletableFuture<Map<String, FileLinesData>> analyzeDirectory(Path directoryPath) {
        log.info("Starting cloc analysis for directory: {}", directoryPath);

        if (!validateDirectory(directoryPath)) {
            return CompletableFuture.completedFuture(new HashMap<>());
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "bash", "-c",
                    "cloc --by-file --unix --csv --quiet --skip-uniqueness --timeout 60 ."
            );
            pb.directory(directoryPath.toFile());
            pb.redirectErrorStream(true);

            Process process = pb.start();

            Map<String, FileLinesData> fileLinesData = new HashMap<>();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
            );

            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                if (line.startsWith("SUM,")) {
                    while (reader.readLine() != null) { }
                    break;
                }

                String[] parts = line.split(",", 5);
                if (parts.length >= 5) {
                    try {
                        String language = parts[0].trim();
                        String filePath = normalizePath(parts[1].trim());
                        int blank = Integer.parseInt(parts[2].trim());
                        int comment = Integer.parseInt(parts[3].trim());
                        int code = Integer.parseInt(parts[4].trim());

                        FileLinesData data = new FileLinesData(language, code, comment, blank);
                        fileLinesData.put(filePath, data);

                    } catch (NumberFormatException _) { }
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.warn("Cloc process exited with code {} for {}", exitCode, directoryPath);
            }

            return CompletableFuture.completedFuture(fileLinesData);

        } catch (IOException | InterruptedException e) {
            log.error("Failed to run cloc process for directory {}: {}", directoryPath, e.getMessage(), e);
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            return CompletableFuture.completedFuture(new HashMap<>());
        }
    }

    private boolean validateDirectory(Path directoryPath) {
        if (directoryPath == null) {
            log.warn("Directory path is null");
            return false;
        }
        if (!Files.exists(directoryPath)) {
            log.warn("Directory does not exist: {}", directoryPath);
            return false;
        }
        if (!Files.isDirectory(directoryPath)) {
            log.warn("Path is not a directory: {}", directoryPath);
            return false;
        }
        if (!Files.isReadable(directoryPath)) {
            log.warn("Directory is not readable: {}", directoryPath);
            return false;
        }
        return true;
    }

    private String normalizePath(String path) {
        return path.replace("./", "").replace("\\", "/");
    }

}
