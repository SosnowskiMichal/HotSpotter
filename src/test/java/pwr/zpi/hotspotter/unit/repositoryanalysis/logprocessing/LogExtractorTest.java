package pwr.zpi.hotspotter.unit.repositoryanalysis.logprocessing;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import pwr.zpi.hotspotter.common.exception.LogProcessingException;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.LogExtractor;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.config.LogExtractorConfig;
import pwr.zpi.hotspotter.repositorymanagement.config.RepositoryManagementConfig;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogExtractorTest {

    @Mock
    private RepositoryManagementConfig repositoryManagementConfig;

    @Mock
    private LogExtractorConfig logExtractorConfig;

    @InjectMocks
    private LogExtractor logExtractor;

    private final Path repo = Path.of("/repo");
    private final Path baseDir = Path.of("/base");
    private final Path logDir = baseDir.resolve("logs");
    private final Path logFile = logDir.resolve("123.log");

    @Test
    void extractLogs_shouldCreateDirectoryAndReturnLogFilePath() throws Exception {
        when(repositoryManagementConfig.getBaseDirectory()).thenReturn(baseDir.toString());
        when(logExtractorConfig.getLogDirectoryName()).thenReturn("logs");
        when(logExtractorConfig.getProcessTimeoutMinutes()).thenReturn(1);

        try (MockedStatic<FileUtils> utils = mockStatic(FileUtils.class)) {
            utils.when(() -> FileUtils.forceMkdir(logDir.toFile())).thenAnswer(_ -> null);

            try (MockedStatic<Files> _ = mockStatic(Files.class)) {

                Process process = mock(Process.class);
                when(process.waitFor(1, TimeUnit.MINUTES)).thenReturn(true);
                when(process.exitValue()).thenReturn(0);

                MockedConstruction<ProcessBuilder> pb = mockConstruction(
                        ProcessBuilder.class,
                        (builder, _) -> when(builder.start()).thenReturn(process)
                );

                Path result = logExtractor.extractLogs(repo, "123",
                        LocalDate.parse("2024-01-01"),
                        LocalDate.parse("2024-01-10"));

                assertThat(result).isEqualTo(logFile);

                utils.verify(() -> FileUtils.forceMkdir(logDir.toFile()));
                pb.close();
            }
        }
    }

    @Test
    void extractLogs_shouldThrowExceptionWhenDirectoryCannotBeCreated() {
        when(repositoryManagementConfig.getBaseDirectory()).thenReturn(baseDir.toString());
        when(logExtractorConfig.getLogDirectoryName()).thenReturn("logs");

        try (MockedStatic<FileUtils> utils = mockStatic(FileUtils.class)) {
            utils.when(() -> FileUtils.forceMkdir(any(File.class)))
                    .thenThrow(new IOException("mkdir error"));

            assertThatThrownBy(() ->
                    logExtractor.extractLogs(repo, "123",
                            LocalDate.now(), LocalDate.now())
            ).isInstanceOf(LogProcessingException.class)
                    .hasMessageContaining("Failed to create log directory");
        }
    }

    @Test
    void extractLogs_shouldTimeoutAndRemoveLogFile() throws Exception {
        when(repositoryManagementConfig.getBaseDirectory()).thenReturn(baseDir.toString());
        when(logExtractorConfig.getLogDirectoryName()).thenReturn("logs");
        when(logExtractorConfig.getProcessTimeoutMinutes()).thenReturn(1);

        try (MockedStatic<FileUtils> utils = mockStatic(FileUtils.class);
             MockedStatic<Files> filesMock = mockStatic(Files.class)) {

            utils.when(() -> FileUtils.forceMkdir(any())).thenAnswer(_ -> null);

            Process process = mock(Process.class);
            when(process.waitFor(1, TimeUnit.MINUTES)).thenReturn(false);

            MockedConstruction<ProcessBuilder> pb = mockConstruction(
                    ProcessBuilder.class,
                    (builder, _) -> when(builder.start()).thenReturn(process)
            );

            filesMock.when(() -> Files.deleteIfExists(logFile)).thenReturn(true);

            assertThatThrownBy(() ->
                    logExtractor.extractLogs(repo, "123",
                            LocalDate.now(), LocalDate.now())
            ).isInstanceOf(LogProcessingException.class)
                    .hasMessageContaining("timed out");

            pb.close();
        }
    }

    @Test
    void extractLogs_shouldFailWhenExitCodeNonZero() throws Exception {
        when(repositoryManagementConfig.getBaseDirectory()).thenReturn(baseDir.toString());
        when(logExtractorConfig.getLogDirectoryName()).thenReturn("logs");
        when(logExtractorConfig.getProcessTimeoutMinutes()).thenReturn(1);

        try (MockedStatic<FileUtils> utils = mockStatic(FileUtils.class);
             MockedStatic<Files> filesMock = mockStatic(Files.class)) {

            utils.when(() -> FileUtils.forceMkdir(any())).thenAnswer(_ -> null);

            Process process = mock(Process.class);
            when(process.waitFor(1, TimeUnit.MINUTES)).thenReturn(true);
            when(process.exitValue()).thenReturn(99);

            MockedConstruction<ProcessBuilder> pb = mockConstruction(
                    ProcessBuilder.class,
                    (builder, _) -> when(builder.start()).thenReturn(process)
            );

            filesMock.when(() -> Files.deleteIfExists(any())).thenReturn(true);

            assertThatThrownBy(() ->
                    logExtractor.extractLogs(repo, "123", LocalDate.now(), LocalDate.now()))
                    .isInstanceOf(LogProcessingException.class)
                    .hasMessageContaining("exit code: 99");

            pb.close();
        }
    }

    @Test
    void deleteLogFile_shouldNotThrow() {
        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            filesMock.when(() -> Files.deleteIfExists(logFile)).thenReturn(true);

            assertThatCode(() -> logExtractor.deleteLogFile(logFile))
                    .doesNotThrowAnyException();

            filesMock.verify(() -> Files.deleteIfExists(logFile));
        }
    }
}
