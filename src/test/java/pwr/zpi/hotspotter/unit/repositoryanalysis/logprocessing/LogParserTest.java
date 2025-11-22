package pwr.zpi.hotspotter.unit.repositoryanalysis.logprocessing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import pwr.zpi.hotspotter.repositoryanalysis.exception.LogProcessingException;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.LogParser;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.model.Commit;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.model.FileChange;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class LogParserTest {

    @InjectMocks
    private final LogParser parser = new LogParser();

    private Path createTempLog(@TempDir Path tempDir, String content) throws IOException {
        Path file = tempDir.resolve("log.txt");
        Files.writeString(file, content);
        return file;
    }

    @Test
    void parsesSingleCommit(@TempDir Path tempDir) throws IOException {
        String log = """
            [abcd1234] 2024-01-01
            John Doe <john@example.com>
            10 2 src/Main.java
            """;

        Path file = createTempLog(tempDir, log);
        Stream<Commit> stream = parser.parseLogs(file);

        List<Commit> commits = stream.toList();
        assertThat(commits).hasSize(1);

        Commit c = commits.getFirst();

        assertThat(c.hash()).isEqualTo("abcd1234");
        assertThat(c.date()).isEqualTo("2024-01-01");
        assertThat(c.author()).isEqualTo("John Doe");
        assertThat(c.email()).isEqualTo("john@example.com");

        assertThat(c.changedFiles()).hasSize(1);
        FileChange fc = c.changedFiles().getFirst();
        assertThat(fc.filePath()).isEqualTo("src/Main.java");
        assertThat(fc.linesAdded()).isEqualTo(10);
        assertThat(fc.linesDeleted()).isEqualTo(2);
    }

    @Test
    void parsesMultipleCommits(@TempDir Path tempDir) throws IOException {
        String log = """
            [111] 2024-01-01
            A <a@a.com>
            1 1 A.java

            [222] 2024-01-02
            B <b@b.com>
            2 3 B.java
            """;

        Path file = createTempLog(tempDir, log);

        List<Commit> commits = parser.parseLogs(file).toList();

        assertThat(commits).hasSize(2);
        assertThat(commits.get(0).hash()).isEqualTo("111");
        assertThat(commits.get(1).hash()).isEqualTo("222");
    }

    @Test
    void parsesEmptyFileChangeBlock(@TempDir Path tempDir) throws IOException {
        String log = """
            [abcd] 2024-01-01
            John <j@x>
            
            """;

        Path file = createTempLog(tempDir, log);

        Commit c = parser.parseLogs(file).findFirst().orElseThrow();
        assertThat(c.changedFiles()).isEmpty();
    }

    @Test
    void parsesDashAsZeroInFileChanges(@TempDir Path tempDir) throws IOException {
        String log = """
            [abcd] 2024-01-01
            John <j@x>
            - - README.md
            """;

        Path file = createTempLog(tempDir, log);

        Commit c = parser.parseLogs(file).findFirst().orElseThrow();
        FileChange fc = c.changedFiles().getFirst();

        assertThat(fc.linesAdded()).isZero();
        assertThat(fc.linesDeleted()).isZero();
    }

    @Test
    void detectsFullRename(@TempDir Path tempDir) throws IOException {
        String log = """
            [abcd] 2024-01-01
            John <j@x>
            5 3 old/path/File.java => new/path/File.java
            """;

        Path file = createTempLog(tempDir, log);

        FileChange fc = parser.parseLogs(file)
                .findFirst()
                .orElseThrow()
                .changedFiles()
                .getFirst();

        assertThat(fc.oldPath()).isEqualTo("old/path/File.java");
        assertThat(fc.newPath()).isEqualTo("new/path/File.java");
    }

    @Test
    void detectsPartialRename(@TempDir Path tempDir) throws IOException {
        String log = """
            [abcd] 2024-01-01
            John <j@x>
            1 1 src/{old => new}/File.java
            """;

        Path file = createTempLog(tempDir, log);

        FileChange fc = parser.parseLogs(file)
                .findFirst()
                .orElseThrow()
                .changedFiles()
                .getFirst();

        assertThat(fc.oldPath()).isEqualTo("src/old/File.java");
        assertThat(fc.newPath()).isEqualTo("src/new/File.java");
    }

    @Test
    void iteratorStopsCorrectlyAtEndOfFile(@TempDir Path tempDir) throws IOException {
        String log = """
            [hash] 2024-01-01
            A <a>
            1 1 A.java
            """;

        Path file = createTempLog(tempDir, log);

        List<Commit> commits = parser.parseLogs(file).toList();
        assertThat(commits).hasSize(1);
    }

    @Test
    void throwsExceptionWhenFileCannotBeRead(@TempDir Path tempDir) {
        Path nonExisting = tempDir.resolve("missing.txt");

        assertThatThrownBy(() -> parser.parseLogs(nonExisting))
                .isInstanceOf(LogProcessingException.class)
                .hasMessageContaining("Failed to initialize commit stream");
    }

    @Test
    void skipsInvalidCommitBlocks(@TempDir Path tempDir) throws IOException {
        String log = """
            INVALID BLOCK WITHOUT HEADER
            1 1 A.java

            [abcd] 2024-01-01
            John <x>
            1 1 B.java
            """;

        Path file = createTempLog(tempDir, log);

        List<Commit> commits = parser.parseLogs(file).toList();

        assertThat(commits).hasSize(1);
        assertThat(commits.getFirst().hash()).isEqualTo("abcd");
    }
}
