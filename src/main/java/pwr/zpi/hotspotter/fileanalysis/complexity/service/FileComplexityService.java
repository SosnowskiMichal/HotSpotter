package pwr.zpi.hotspotter.fileanalysis.complexity.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import pwr.zpi.hotspotter.common.utils.FileUtils;
import pwr.zpi.hotspotter.fileanalysis.complexity.component.HeuristicStrategy;
import pwr.zpi.hotspotter.fileanalysis.complexity.component.LizardStrategy;
import pwr.zpi.hotspotter.fileanalysis.complexity.model.FileComplexityReport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileComplexityService {

    private final LizardStrategy lizardStrategy;
    private final HeuristicStrategy heuristicStrategy;

    @Async("fileComplexityExecutor")
    public CompletableFuture<Map<String, FileComplexityReport>> analyze(Path path) {
        Optional<Path> firstPathOpt = findFirstFile(path);

        if (firstPathOpt.isEmpty()) {
            log.warn("Directory is empty or contains no files: {}", path);
            return CompletableFuture.completedFuture(Collections.emptyMap());
        }

        Path firstPath = firstPathOpt.get();
        String extension = FileUtils.getExtension(firstPath);
        log.info("First file found: {}. Extension: {}", firstPath, extension);

        Map<String, FileComplexityReport> result;

        if (lizardStrategy.isSupported(extension)) {
            log.info("Extension supported by Lizard. Running Lizard Strategy for folder: {}", path);
            try {
                result = lizardStrategy.analyze(path);
            } catch (RuntimeException e) {
                log.warn("Lizard batch analysis failed. Fallback to Heuristic.", e);
                result = heuristicStrategy.analyze(path);
            }
        } else {
            log.info("Extension NOT supported by Lizard. Running Heuristic Strategy for folder: {}", path);
            result = heuristicStrategy.analyze(path);
        }

        return CompletableFuture.completedFuture(result);
    }

    private Optional<Path> findFirstFile(Path path) {
        try (Stream<Path> stream = Files.walk(path)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> !p.getFileName().toString().startsWith("."))
                    .findFirst();
        } catch (IOException e) {
            log.error("Error walking directory: {}", path, e);
            return Optional.empty();
        }
    }
}