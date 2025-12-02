package pwr.zpi.hotspotter.fileanalysis.complexity.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import pwr.zpi.hotspotter.fileanalysis.complexity.component.HeuristicStrategy;
import pwr.zpi.hotspotter.fileanalysis.complexity.component.LizardStrategy;
import pwr.zpi.hotspotter.fileanalysis.complexity.model.FileComplexityReport;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileComplexityService {

    private final LizardStrategy lizardStrategy;
    private final HeuristicStrategy heuristicStrategy;

    @Async("fileComplexityExecutor")
    public CompletableFuture<Map<String, FileComplexityReport>> analyze(Path path, String extension) {
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
}