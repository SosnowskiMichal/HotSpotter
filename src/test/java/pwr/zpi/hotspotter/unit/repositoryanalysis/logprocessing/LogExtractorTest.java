package pwr.zpi.hotspotter.unit.repositoryanalysis.logprocessing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import pwr.zpi.hotspotter.common.exception.LogProcessingException;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.CommitStream;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.LogExtractor;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.LogParser;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.model.Commit;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogExtractorTest {

    @Mock
    private LogParser logParser;

    @InjectMocks
    private LogExtractor logExtractor;

    private final Path repo = Path.of("/repo");

    @Test
    void extractAndParseCommits_shouldReturnCommitStream() throws Exception {
        Process process = mock(Process.class);
        InputStream inputStream = new ByteArrayInputStream("test".getBytes());
        when(process.getInputStream()).thenReturn(inputStream);

        Stream<Commit> commitStream = Stream.empty();
        when(logParser.parseLogs(any(InputStream.class))).thenReturn(commitStream);

        try (MockedConstruction<ProcessBuilder> pb = mockConstruction(
                ProcessBuilder.class,
                (builder, _) -> when(builder.start()).thenReturn(process))) {

            CommitStream result = logExtractor.extractAndParseCommits(
                    repo,
                    LocalDate.parse("2024-01-01"),
                    LocalDate.parse("2024-01-10")
            );

            assertThat(result).isNotNull();
            assertThat(result.getStream()).isEqualTo(commitStream);

            verify(logParser).parseLogs(any(InputStream.class));

            result.close();
        }
    }

    @Test
    void extractAndParseCommits_shouldHandleDateRange() throws Exception {
        Process process = mock(Process.class);
        InputStream inputStream = new ByteArrayInputStream("".getBytes());
        when(process.getInputStream()).thenReturn(inputStream);
        when(logParser.parseLogs(any(InputStream.class))).thenReturn(Stream.empty());

        try (MockedConstruction<ProcessBuilder> pb = mockConstruction(
                ProcessBuilder.class,
                (builder, context) -> {
                    when(builder.start()).thenReturn(process);
                })) {

            logExtractor.extractAndParseCommits(
                    repo,
                    LocalDate.parse("2024-01-01"),
                    LocalDate.parse("2024-12-31")
            ).close();

            assertThat(pb.constructed()).hasSize(1);
        }
    }

    @Test
    void extractAndParseCommits_shouldHandleNullDates() throws Exception {
        Process process = mock(Process.class);
        when(process.getInputStream()).thenReturn(new ByteArrayInputStream("".getBytes()));
        when(logParser.parseLogs(any(InputStream.class))).thenReturn(Stream.empty());

        try (MockedConstruction<ProcessBuilder> pb = mockConstruction(
                ProcessBuilder.class,
                (builder, _) -> when(builder.start()).thenReturn(process))) {

            logExtractor.extractAndParseCommits(repo, null, null).close();

            assertThat(pb.constructed()).isNotEmpty();
        }
    }

    @Test
    void extractAndParseCommits_shouldThrowExceptionWhenProcessStartFails() {
        try (MockedConstruction<ProcessBuilder> pb = mockConstruction(
                ProcessBuilder.class,
                (builder, _) -> when(builder.start()).thenThrow(new IOException("Process error")))) {

            assertThatThrownBy(() ->
                    logExtractor.extractAndParseCommits(repo, null, null))
                    .isInstanceOf(LogProcessingException.class)
                    .hasMessageContaining("Failed to start git log process");
        }
    }

    @Test
    void handleProcessCompletion_shouldValidateExitCode() throws Exception {
        Process process = mock(Process.class);
        when(process.isAlive()).thenReturn(false);
        when(process.exitValue()).thenReturn(0);
        when(process.getInputStream()).thenReturn(new ByteArrayInputStream("".getBytes()));

        Stream<Commit> mockStream = Stream.empty();
        when(logParser.parseLogs(any(InputStream.class))).thenReturn(mockStream);

        try (MockedConstruction<ProcessBuilder> pb = mockConstruction(
                ProcessBuilder.class,
                (builder, _) -> when(builder.start()).thenReturn(process))) {

            CommitStream commitStream = logExtractor.extractAndParseCommits(repo, null, null);

            assertThatCode(() -> commitStream.getStream().close())
                    .doesNotThrowAnyException();

            verify(process).exitValue();
            commitStream.close();
        }
    }

}
