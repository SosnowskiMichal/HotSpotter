package pwr.zpi.hotspotter.unit.fileanalysis.complexity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pwr.zpi.hotspotter.fileanalysis.complexity.component.HeuristicStrategy;
import pwr.zpi.hotspotter.fileanalysis.complexity.component.LizardStrategy;
import pwr.zpi.hotspotter.fileanalysis.complexity.model.FileComplexityReport;
import pwr.zpi.hotspotter.fileanalysis.complexity.service.FileComplexityService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileComplexityServiceTest {

    @Mock
    private LizardStrategy lizardStrategy;
    @Mock
    private HeuristicStrategy heuristicStrategy;

    @InjectMocks
    private FileComplexityService fileComplexityService;

    @Test
    void analyze_ShouldUseLizard_WhenFirstFileIsSupported(@TempDir Path tempDir) throws IOException, ExecutionException, InterruptedException {
        Path file = tempDir.resolve("Service.java");
        Files.createFile(file);
        Map<String, FileComplexityReport> expectedMap = Map.of("Service", new FileComplexityReport());

        when(lizardStrategy.isSupported("java")).thenReturn(true);
        when(lizardStrategy.analyze(tempDir)).thenReturn(expectedMap);

        CompletableFuture<Map<String, FileComplexityReport>> future = fileComplexityService.analyze(tempDir, "java");
        Map<String, FileComplexityReport> result = future.get();

        assertEquals(expectedMap, result);
        verify(lizardStrategy).analyze(tempDir);
        verify(heuristicStrategy, never()).analyze(any());
    }

    @Test
    void analyze_ShouldUseHeuristic_WhenFirstFileIsNotSupported(@TempDir Path tempDir) throws IOException, ExecutionException, InterruptedException {
        Path file = tempDir.resolve("Notes.txt");
        Files.createFile(file);

        Map<String, FileComplexityReport> expectedMap = Map.of("Notes", new FileComplexityReport());

        when(lizardStrategy.isSupported("txt")).thenReturn(false);
        when(heuristicStrategy.analyze(tempDir)).thenReturn(expectedMap);

        CompletableFuture<Map<String, FileComplexityReport>> future = fileComplexityService.analyze(tempDir, "txt");
        Map<String, FileComplexityReport> result = future.get();

        assertEquals(expectedMap, result);
        verify(lizardStrategy, never()).analyze(any());
        verify(heuristicStrategy).analyze(tempDir);
    }

    @Test
    void analyze_ShouldFallbackToHeuristic_WhenLizardFails(@TempDir Path tempDir) throws IOException, ExecutionException, InterruptedException {
        Path file = tempDir.resolve("Broken.java");
        Files.createFile(file);

        Map<String, FileComplexityReport> fallbackMap = Map.of("Broken", new FileComplexityReport());

        when(lizardStrategy.isSupported("java")).thenReturn(true);
        when(lizardStrategy.analyze(tempDir)).thenThrow(new RuntimeException("Lizard process crashed"));
        when(heuristicStrategy.analyze(tempDir)).thenReturn(fallbackMap);

        CompletableFuture<Map<String, FileComplexityReport>> future = fileComplexityService.analyze(tempDir, "java");
        Map<String, FileComplexityReport> result = future.get();

        assertEquals(fallbackMap, result);
        verify(lizardStrategy).analyze(tempDir);
        verify(heuristicStrategy).analyze(tempDir);
    }

    @Test
    void analyze_ShouldSkipHiddenFiles_AndFindNextValidFile(@TempDir Path tempDir) throws IOException, ExecutionException, InterruptedException {
        Files.createFile(tempDir.resolve(".gitkeep"));
        Files.createFile(tempDir.resolve("RealCode.py"));

        Map<String, FileComplexityReport> expectedMap = Map.of("RealCode", new FileComplexityReport());

        when(lizardStrategy.isSupported("py")).thenReturn(true);
        when(lizardStrategy.analyze(tempDir)).thenReturn(expectedMap);

        CompletableFuture<Map<String, FileComplexityReport>> future = fileComplexityService.analyze(tempDir, "py");
        Map<String, FileComplexityReport> result = future.get();

        assertEquals(expectedMap, result);
        verify(lizardStrategy).isSupported("py");
    }

    @Test
    void analyze_ShouldHandleNestedFiles(@TempDir Path tempDir) throws IOException, ExecutionException, InterruptedException {
        Path src = Files.createDirectory(tempDir.resolve("src"));
        Files.createFile(src.resolve("App.java"));

        when(lizardStrategy.isSupported("java")).thenReturn(true);
        when(lizardStrategy.analyze(tempDir)).thenReturn(Collections.emptyMap());

        fileComplexityService.analyze(tempDir, "java").get();

        verify(lizardStrategy).isSupported("java");
    }
}