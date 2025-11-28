package pwr.zpi.hotspotter.unit.repositoryanalysis.logprocessing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.LogParser;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.model.Commit;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.model.FileChange;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class LogParserTest {

    @InjectMocks
    private final LogParser parser = new LogParser();

    @Test
    void parsesCommitsFromInputStream() {
        String log = """
            [hash1] 2024-01-01
            Author One <author1@example.com>
            5 3 src/File1.java

            [hash2] 2024-01-02
            Author Two <author2@example.com>
            10 2 src/File2.java
            """;

        InputStream inputStream = new ByteArrayInputStream(log.getBytes(StandardCharsets.UTF_8));

        try (Stream<Commit> stream = parser.parseLogs(inputStream)) {
            List<Commit> commits = stream.toList();

            assertThat(commits).hasSize(2);
            assertThat(commits.get(0).hash()).isEqualTo("hash1");
            assertThat(commits.get(0).author()).isEqualTo("Author One");
            assertThat(commits.get(1).hash()).isEqualTo("hash2");
            assertThat(commits.get(1).author()).isEqualTo("Author Two");
        }
    }

    @Test
    void skipsEmptyCommitsWhenParsingFromInputStream() {
        String log = """
            [empty1] 2024-01-01
            Empty Commit <empty@example.com>

            [valid1] 2024-01-02
            Valid Author <valid@example.com>
            5 2 src/Main.java

            [empty2] 2024-01-03
            Another Empty <empty2@example.com>
            """;

        InputStream inputStream = new ByteArrayInputStream(log.getBytes(StandardCharsets.UTF_8));

        try (Stream<Commit> stream = parser.parseLogs(inputStream)) {
            List<Commit> commits = stream.toList();

            assertThat(commits).hasSize(1);
            assertThat(commits.get(0).hash()).isEqualTo("valid1");
        }
    }

    @Test
    void parsesFileChangesFromInputStream() {
        String log = """
            [abc] 2024-01-01
            Dev <dev@test.com>
            10 5 src/Main.java
            3 1 src/Utils.java
            """;

        InputStream inputStream = new ByteArrayInputStream(log.getBytes(StandardCharsets.UTF_8));

        try (Stream<Commit> stream = parser.parseLogs(inputStream)) {
            Commit commit = stream.findFirst().orElseThrow();

            assertThat(commit.changedFiles()).hasSize(2);
            assertThat(commit.changedFiles().get(0).filePath()).isEqualTo("src/Main.java");
            assertThat(commit.changedFiles().get(0).linesAdded()).isEqualTo(10);
            assertThat(commit.changedFiles().get(0).linesDeleted()).isEqualTo(5);
            assertThat(commit.changedFiles().get(1).filePath()).isEqualTo("src/Utils.java");
        }
    }

    @Test
    void handlesFullRenameInInputStream() {
        String log = """
            [rename] 2024-01-01
            Dev <dev@test.com>
            5 3 old/File.java => new/File.java
            """;

        InputStream inputStream = new ByteArrayInputStream(log.getBytes(StandardCharsets.UTF_8));

        try (Stream<Commit> stream = parser.parseLogs(inputStream)) {
            FileChange fileChange = stream.findFirst()
                    .orElseThrow()
                    .changedFiles()
                    .getFirst();

            assertThat(fileChange.oldPath()).isEqualTo("old/File.java");
            assertThat(fileChange.newPath()).isEqualTo("new/File.java");
        }
    }

    @Test
    void handlesPartialRenameInInputStream() {
        String log = """
            [rename] 2024-01-01
            Dev <dev@test.com>
            1 1 src/{old => new}/File.java
            """;

        InputStream inputStream = new ByteArrayInputStream(log.getBytes(StandardCharsets.UTF_8));

        try (Stream<Commit> stream = parser.parseLogs(inputStream)) {
            FileChange fileChange = stream.findFirst()
                    .orElseThrow()
                    .changedFiles()
                    .getFirst();

            assertThat(fileChange.oldPath()).isEqualTo("src/old/File.java");
            assertThat(fileChange.newPath()).isEqualTo("src/new/File.java");
        }
    }

    @Test
    void parsesDashAsZeroInInputStream() {
        String log = """
            [abcd] 2024-01-01
            John <j@x>
            - - README.md
            """;

        InputStream inputStream = new ByteArrayInputStream(log.getBytes(StandardCharsets.UTF_8));

        try (Stream<Commit> stream = parser.parseLogs(inputStream)) {
            FileChange fileChange = stream.findFirst()
                    .orElseThrow()
                    .changedFiles()
                    .getFirst();

            assertThat(fileChange.linesAdded()).isZero();
            assertThat(fileChange.linesDeleted()).isZero();
        }
    }

    @Test
    void skipsInvalidCommitBlocksInInputStream() {
        String log = """
            INVALID BLOCK WITHOUT HEADER
            1 1 A.java

            [abcd] 2024-01-01
            John <x>
            1 1 B.java
            """;

        InputStream inputStream = new ByteArrayInputStream(log.getBytes(StandardCharsets.UTF_8));

        try (Stream<Commit> stream = parser.parseLogs(inputStream)) {
            List<Commit> commits = stream.toList();

            assertThat(commits).hasSize(1);
            assertThat(commits.getFirst().hash()).isEqualTo("abcd");
        }
    }

    @Test
    void streamCanBeClosedProperly() {
        String log = """
            [test] 2024-01-01
            Test <test@test.com>
            1 1 test.java
            """;

        InputStream inputStream = new ByteArrayInputStream(log.getBytes(StandardCharsets.UTF_8));

        assertThatCode(() -> {
            try (Stream<Commit> stream = parser.parseLogs(inputStream)) {
                assertThat(stream.count()).isEqualTo(1);
            }
        }).doesNotThrowAnyException();
    }

    @Test
    void handlesEmptyInputStream() {
        InputStream inputStream = new ByteArrayInputStream(new byte[0]);

        try (Stream<Commit> stream = parser.parseLogs(inputStream)) {
            List<Commit> commits = stream.toList();
            assertThat(commits).isEmpty();
        }
    }

}
