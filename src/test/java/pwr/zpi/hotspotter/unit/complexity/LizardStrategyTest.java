package pwr.zpi.hotspotter.unit.complexity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pwr.zpi.hotspotter.fileanalysis.complexity.component.LizardRunner;
import pwr.zpi.hotspotter.fileanalysis.complexity.component.LizardStrategy;
import pwr.zpi.hotspotter.fileanalysis.complexity.model.FileComplexityReport;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LizardStrategyTest {

    @Mock
    private LizardRunner lizardRunner;

    @InjectMocks
    private LizardStrategy lizardStrategy;

    @Test
    void isSupported_ShouldReturnTrueForSupportedExtensions() {
        assertTrue(lizardStrategy.isSupported("java"));
        assertTrue(lizardStrategy.isSupported("py"));
        assertTrue(lizardStrategy.isSupported("CPP"));
        assertTrue(lizardStrategy.isSupported("tsx"));
    }

    @Test
    void isSupported_ShouldReturnFalseForUnsupportedExtensions() {
        assertFalse(lizardStrategy.isSupported("txt"));
        assertFalse(lizardStrategy.isSupported("md"));
        assertFalse(lizardStrategy.isSupported(""));
    }

    @Test
    void analyze_ShouldParseValidCsvOutput() throws IOException {
        Path mockPath = Path.of("dummy/path");
        List<String> mockOutput = Arrays.asList(
                "NLOC, CCN, token, PARAM, length, location, file, function, long_name, start, end",
                "10,   5,   50,    2,     15,     loc1,     src/Service.java, processData, longName, 10,   25"
        );

        when(lizardRunner.runLizard(any())).thenReturn(mockOutput);

        Map<String, FileComplexityReport> result = lizardStrategy.analyze(mockPath);

        assertNotNull(result);
        assertTrue(result.containsKey("Service"));

        FileComplexityReport report = result.get("Service");
        assertEquals(5, report.getTotalCCN());
        assertEquals(1, report.getMethodsCount());
        assertEquals(5, report.getMaxCCN());

        assertEquals("processData", report.getMethods().getFirst().getName());
    }

    @Test
    void analyze_ShouldAggregateMultipleFilesWithSameName() throws IOException {
        Path mockPath = Path.of("project");
        List<String> mockOutput = Arrays.asList(
                "10, 3, 50, 0, 10, loc, src/main/Service.java, init, long, 1, 10",
                "20, 4, 80, 1, 20, loc, src/test/Service.java, testInit, long, 1, 20"
        );

        when(lizardRunner.runLizard(any())).thenReturn(mockOutput);

        Map<String, FileComplexityReport> result = lizardStrategy.analyze(mockPath);

        assertEquals(1, result.size());
        assertTrue(result.containsKey("Service"));

        FileComplexityReport report = result.get("Service");
        assertEquals(7, report.getTotalCCN());
        assertEquals(2, report.getMethodsCount());
    }

    @Test
    void analyze_ShouldHandleGarbageAndShortLines() throws IOException {
        List<String> mockOutput = Arrays.asList(
                "WARNING: Some warning from lizard",
                "   ",
                "10, 5, 50"
        );
        when(lizardRunner.runLizard(any())).thenReturn(mockOutput);

        Map<String, FileComplexityReport> result = lizardStrategy.analyze(Path.of("."));

        assertTrue(result.isEmpty(), "Result map should be empty for garbage input");
    }

    @Test
    void analyze_ShouldHandleParsingErrorsGracefully() throws IOException {
        List<String> mockOutput = List.of(
                "10, NOT_A_NUMBER, 50, 2, 10, loc, src/File.java, func, long, 1, 10"
        );
        when(lizardRunner.runLizard(any())).thenReturn(mockOutput);

        Map<String, FileComplexityReport> result = lizardStrategy.analyze(Path.of("."));

        assertTrue(result.containsKey("File"));
        assertEquals(0, result.get("File").getTotalCCN());
    }

    @Test
    void analyze_ShouldHandleOptionalColumnsMissing() throws IOException {
        List<String> mockOutput = List.of(
                "10, 5, 50, 1, 10, loc, src/Short.java, myFunc"
        );
        when(lizardRunner.runLizard(any())).thenReturn(mockOutput);

        Map<String, FileComplexityReport> result = lizardStrategy.analyze(Path.of("."));

        assertTrue(result.containsKey("Short"));
        FileComplexityReport report = result.get("Short");
        assertEquals(5, report.getTotalCCN());
    }

    @Test
    void analyze_ShouldThrowRuntimeException_WhenIoExceptionOccurs() throws IOException {
        when(lizardRunner.runLizard(any())).thenThrow(new IOException("System error"));

        assertThrows(RuntimeException.class, () -> lizardStrategy.analyze(Path.of(".")));
    }
}