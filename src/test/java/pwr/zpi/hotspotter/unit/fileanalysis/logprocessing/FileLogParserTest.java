package pwr.zpi.hotspotter.unit.fileanalysis.logprocessing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import pwr.zpi.hotspotter.fileanalysis.logprocessing.FileLogParser;
import pwr.zpi.hotspotter.fileanalysis.logprocessing.model.FileCommit;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class FileLogParserTest {

    @InjectMocks
    private final FileLogParser parser = new FileLogParser();

    @Test
    void parsesFileCommitsFromInputStream() {
        String log = """
            [abc123] 2024-01-15 John Doe <john@example.com>
            5 3 src/main/java/File1.java

            [def456] 2024-01-16 Jane Smith <jane@example.com>
            10 2 src/main/java/File2.java
            """;

        List<FileCommit> commits = parser.parseFileLogs(toInputStream(log));

        assertThat(commits).hasSize(2);
        assertThat(commits.getFirst().hash()).isEqualTo("abc123");
        assertThat(commits.getFirst().date()).isEqualTo(LocalDate.of(2024, 1, 15));
        assertThat(commits.getFirst().author()).isEqualTo("John Doe");
        assertThat(commits.getFirst().email()).isEqualTo("john@example.com");
        assertThat(commits.getFirst().path()).isEqualTo("src/main/java/File1.java");
        assertThat(commits.getFirst().linesAdded()).isEqualTo(5);
        assertThat(commits.getFirst().linesDeleted()).isEqualTo(3);

        assertThat(commits.get(1).hash()).isEqualTo("def456");
        assertThat(commits.get(1).date()).isEqualTo(LocalDate.of(2024, 1, 16));
        assertThat(commits.get(1).path()).isEqualTo("src/main/java/File2.java");
        assertThat(commits.get(1).linesAdded()).isEqualTo(10);
        assertThat(commits.get(1).linesDeleted()).isEqualTo(2);
    }

    @Test
    void handlesBinaryFileCommit() {
        String log = """
            [bin123] 2024-01-15 Dev <dev@test.com>
            - - binary-file.png
            """;

        List<FileCommit> commits = parser.parseFileLogs(toInputStream(log));

        assertThat(commits).hasSize(1);
        assertThat(commits.getFirst().hash()).isEqualTo("bin123");
        assertThat(commits.getFirst().path()).isEqualTo("binary-file.png");
        assertThat(commits.getFirst().linesAdded()).isEqualTo(0);
        assertThat(commits.getFirst().linesDeleted()).isEqualTo(0);
    }

    @Test
    void handlesEmptyInputStream() {
        List<FileCommit> commits = parser.parseFileLogs(toInputStream(""));
        assertThat(commits).isEmpty();
    }

    @Test
    void skipsInvalidBlocks() {
        String log = """
            INVALID LINE

            [valid] 2024-01-15 Dev <dev@test.com>
            1 0 src/test/File.java
            """;

        List<FileCommit> commits = parser.parseFileLogs(toInputStream(log));
        assertThat(commits).hasSize(1);
        assertThat(commits.getFirst().hash()).isEqualTo("valid");
        assertThat(commits.getFirst().path()).isEqualTo("src/test/File.java");
    }

    @Test
    void handlesSingularInsertionDeletion() {
        String log = """
            [single] 2024-01-15 Dev <dev@test.com>
            1 1 src/File.java
            """;

        List<FileCommit> commits = parser.parseFileLogs(toInputStream(log));
        assertThat(commits).hasSize(1);
        assertThat(commits.getFirst().path()).isEqualTo("src/File.java");
        assertThat(commits.getFirst().linesAdded()).isEqualTo(1);
        assertThat(commits.getFirst().linesDeleted()).isEqualTo(1);
    }

    @Test
    void handlesOnlyInsertions() {
        String log = """
            [new] 2024-01-15 Dev <dev@test.com>
            100 0 src/NewFile.java
            """;

        List<FileCommit> commits = parser.parseFileLogs(toInputStream(log));
        assertThat(commits).hasSize(1);
        assertThat(commits.getFirst().path()).isEqualTo("src/NewFile.java");
        assertThat(commits.getFirst().linesAdded()).isEqualTo(100);
        assertThat(commits.getFirst().linesDeleted()).isEqualTo(0);
    }

    @Test
    void handlesOnlyDeletions() {
        String log = """
            [del] 2024-01-15 Dev <dev@test.com>
            0 50 src/DeletedFile.java
            """;

        List<FileCommit> commits = parser.parseFileLogs(toInputStream(log));
        assertThat(commits).hasSize(1);
        assertThat(commits.getFirst().path()).isEqualTo("src/DeletedFile.java");
        assertThat(commits.getFirst().linesAdded()).isEqualTo(0);
        assertThat(commits.getFirst().linesDeleted()).isEqualTo(50);
    }

    @Test
    void parsesLocalDateCorrectly() {
        String log = """
            [date] 2024-12-31 Dev <dev@test.com>
            1 0 src/File.java
            """;

        List<FileCommit> commits = parser.parseFileLogs(toInputStream(log));
        assertThat(commits).hasSize(1);
        assertThat(commits.getFirst().date()).isEqualTo(LocalDate.of(2024, 12, 31));
    }

    @Test
    void trimsAuthorName() {
        String log = """
            [trim] 2024-01-15 Author With Spaces   <author@test.com>
            5 0 src/File.java
            """;

        List<FileCommit> commits = parser.parseFileLogs(toInputStream(log));
        assertThat(commits).hasSize(1);
        assertThat(commits.getFirst().author()).isEqualTo("Author With Spaces");
    }

    @Test
    void handlesMultipleCommitsWithVariedStats() {
        String log = """
            [commit1] 2024-01-01 Alice <alice@test.com>
            10 0 src/File1.java

            [commit2] 2024-01-02 Bob <bob@test.com>
            0 5 src/File2.java

            [commit3] 2024-01-03 Charlie <charlie@test.com>
            - - binary-file.bin

            [commit4] 2024-01-04 Diana <diana@test.com>
            3 7 src/File4.java
            """;

        List<FileCommit> commits = parser.parseFileLogs(toInputStream(log));
        assertThat(commits).hasSize(4);

        // Commit 1: only insertions
        assertThat(commits.getFirst().linesAdded()).isEqualTo(10);
        assertThat(commits.getFirst().linesDeleted()).isEqualTo(0);
        assertThat(commits.getFirst().path()).isEqualTo("src/File1.java");

        // Commit 2: only deletions
        assertThat(commits.get(1).linesAdded()).isEqualTo(0);
        assertThat(commits.get(1).linesDeleted()).isEqualTo(5);
        assertThat(commits.get(1).path()).isEqualTo("src/File2.java");

        // Commit 3: binary/no stats
        assertThat(commits.get(2).linesAdded()).isEqualTo(0);
        assertThat(commits.get(2).linesDeleted()).isEqualTo(0);
        assertThat(commits.get(2).path()).isEqualTo("binary-file.bin");

        // Commit 4: both insertions and deletions
        assertThat(commits.get(3).linesAdded()).isEqualTo(3);
        assertThat(commits.get(3).linesDeleted()).isEqualTo(7);
        assertThat(commits.get(3).path()).isEqualTo("src/File4.java");
    }

    @Test
    void handlesCommitsWithBlankLinesBetween() {
        String log = """
            [hash1] 2024-01-01 Author <a@test.com>
            1 0 src/File1.java



            [hash2] 2024-01-02 Author <a@test.com>
            2 0 src/File2.java
            """;

        List<FileCommit> commits = parser.parseFileLogs(toInputStream(log));
        assertThat(commits).hasSize(2);
        assertThat(commits.getFirst().hash()).isEqualTo("hash1");
        assertThat(commits.get(1).hash()).isEqualTo("hash2");
    }

    @Test
    void handlesCommitWithoutStatsLine() {
        String log = """
            [nostats] 2024-01-15 Dev <dev@test.com>
            """;

        List<FileCommit> commits = parser.parseFileLogs(toInputStream(log));
        assertThat(commits).hasSize(1);
        assertThat(commits.getFirst().hash()).isEqualTo("nostats");
        assertThat(commits.getFirst().linesAdded()).isEqualTo(0);
        assertThat(commits.getFirst().linesDeleted()).isEqualTo(0);
    }

    @Test
    void parsesPluralInsertionsAndDeletions() {
        String log = """
            [plural] 2024-01-15 Dev <dev@test.com>
            42 13 src/LargeFile.java
            """;

        List<FileCommit> commits = parser.parseFileLogs(toInputStream(log));
        assertThat(commits).hasSize(1);
        assertThat(commits.getFirst().path()).isEqualTo("src/LargeFile.java");
        assertThat(commits.getFirst().linesAdded()).isEqualTo(42);
        assertThat(commits.getFirst().linesDeleted()).isEqualTo(13);
    }

    @Test
    void handlesEmailWithSpecialCharacters() {
        String log = """
            [email] 2024-01-15 User Name <user.name+tag@example.co.uk>
            1 0 src/File.java
            """;

        List<FileCommit> commits = parser.parseFileLogs(toInputStream(log));
        assertThat(commits).hasSize(1);
        assertThat(commits.getFirst().email()).isEqualTo("user.name+tag@example.co.uk");
    }

    @Test
    void handlesFullRename() {
        String log = """
            [abc123] 2024-01-15 John Doe <john@example.com>
            5 3 old/path/File.java => new/path/File.java
            """;

        List<FileCommit> commits = parser.parseFileLogs(toInputStream(log));

        assertThat(commits).hasSize(1);
        assertThat(commits.getFirst().path()).isEqualTo("new/path/File.java");
        assertThat(commits.getFirst().linesAdded()).isEqualTo(5);
        assertThat(commits.getFirst().linesDeleted()).isEqualTo(3);
    }

    @Test
    void handlesPartialRenameFilename() {
        String log = """
            [def456] 2024-01-16 Jane Smith <jane@example.com>
            10 2 src/main/{OldName.java => NewName.java}
            """;

        List<FileCommit> commits = parser.parseFileLogs(toInputStream(log));

        assertThat(commits).hasSize(1);
        assertThat(commits.getFirst().path()).isEqualTo("src/main/NewName.java");
        assertThat(commits.getFirst().linesAdded()).isEqualTo(10);
        assertThat(commits.getFirst().linesDeleted()).isEqualTo(2);
    }

    @Test
    void handlesPartialRenameDirectory() {
        String log = """
            [ghi789] 2024-01-17 Dev User <dev@example.com>
            7 4 src/{old/package => new/package}/File.java
            """;

        List<FileCommit> commits = parser.parseFileLogs(toInputStream(log));

        assertThat(commits).hasSize(1);
        assertThat(commits.getFirst().path()).isEqualTo("src/new/package/File.java");
        assertThat(commits.getFirst().linesAdded()).isEqualTo(7);
        assertThat(commits.getFirst().linesDeleted()).isEqualTo(4);
    }

    @Test
    void handlesMultiplePartialRenames() {
        String log = """
            [jkl012] 2024-01-18 Dev User <dev@example.com>
            3 1 {old => new}/src/{pkg1 => pkg2}/File.java
            """;

        List<FileCommit> commits = parser.parseFileLogs(toInputStream(log));

        assertThat(commits).hasSize(1);
        assertThat(commits.getFirst().path()).isEqualTo("new/src/pkg2/File.java");
        assertThat(commits.getFirst().linesAdded()).isEqualTo(3);
        assertThat(commits.getFirst().linesDeleted()).isEqualTo(1);
    }

    @Test
    void handlesNonRenamedPathWithNumstat() {
        String log = """
            [mno345] 2024-01-19 Dev User <dev@example.com>
            8 5 src/main/java/File.java
            """;

        List<FileCommit> commits = parser.parseFileLogs(toInputStream(log));

        assertThat(commits).hasSize(1);
        assertThat(commits.getFirst().path()).isEqualTo("src/main/java/File.java");
        assertThat(commits.getFirst().linesAdded()).isEqualTo(8);
        assertThat(commits.getFirst().linesDeleted()).isEqualTo(5);
    }

    @Test
    void normalizesPathWithMultipleSlashes() {
        String log = """
            [pqr678] 2024-01-20 Dev User <dev@example.com>
            2 1 old//path => new///path//File.java
            """;

        List<FileCommit> commits = parser.parseFileLogs(toInputStream(log));

        assertThat(commits).hasSize(1);
        assertThat(commits.getFirst().path()).isEqualTo("new/path/File.java");
        assertThat(commits.getFirst().linesAdded()).isEqualTo(2);
        assertThat(commits.getFirst().linesDeleted()).isEqualTo(1);
    }

    @Test
    void handlesNullPathGracefully() {
        String log = """
            [stu901] 2024-01-21 Dev User <dev@example.com>
            """;

        List<FileCommit> commits = parser.parseFileLogs(toInputStream(log));

        assertThat(commits).hasSize(1);
        assertThat(commits.getFirst().path()).isNull();
    }

    @Test
    void handlesRealWorldPartialRename() {
        String log = """
            [8c8218b] 2025-11-27 Michał Sosnowski <sosnowskimichal.028@gmail.com>
            2 2 src/main/java/pwr/zpi/hotspotter/analysisqueue/{QueuedAnalysisTask.java => AnalysisQueueTask.java}
            """;

        List<FileCommit> commits = parser.parseFileLogs(toInputStream(log));

        assertThat(commits).hasSize(1);
        assertThat(commits.getFirst().path())
                .isEqualTo("src/main/java/pwr/zpi/hotspotter/analysisqueue/AnalysisQueueTask.java");
        assertThat(commits.getFirst().linesAdded()).isEqualTo(2);
        assertThat(commits.getFirst().linesDeleted()).isEqualTo(2);
    }

    private InputStream toInputStream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

}
