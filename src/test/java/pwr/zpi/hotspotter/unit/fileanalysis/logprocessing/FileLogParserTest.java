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
             1 file changed, 5 insertions(+), 3 deletions(-)

            [def456] 2024-01-16 Jane Smith <jane@example.com>
             1 file changed, 10 insertions(+), 2 deletions(-)
            """;

        List<FileCommit> commits = parser.parseFileLogs(toInputStream(log));

        assertThat(commits).hasSize(2);
        assertThat(commits.getFirst().hash()).isEqualTo("abc123");
        assertThat(commits.getFirst().date()).isEqualTo(LocalDate.of(2024, 1, 15));
        assertThat(commits.getFirst().author()).isEqualTo("John Doe");
        assertThat(commits.getFirst().email()).isEqualTo("john@example.com");
        assertThat(commits.getFirst().linesAdded()).isEqualTo(5);
        assertThat(commits.getFirst().linesDeleted()).isEqualTo(3);

        assertThat(commits.get(1).hash()).isEqualTo("def456");
        assertThat(commits.get(1).date()).isEqualTo(LocalDate.of(2024, 1, 16));
        assertThat(commits.get(1).linesAdded()).isEqualTo(10);
        assertThat(commits.get(1).linesDeleted()).isEqualTo(2);
    }

    @Test
    void handlesBinaryFileCommit() {
        String log = """
            [bin123] 2024-01-15 Dev <dev@test.com>
             1 file changed
            """;

        List<FileCommit> commits = parser.parseFileLogs(toInputStream(log));

        assertThat(commits).hasSize(1);
        assertThat(commits.getFirst().hash()).isEqualTo("bin123");
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
             1 file changed, 1 insertion(+)
            """;

        List<FileCommit> commits = parser.parseFileLogs(toInputStream(log));
        assertThat(commits).hasSize(1);
        assertThat(commits.getFirst().hash()).isEqualTo("valid");
    }

    @Test
    void handlesSingularInsertionDeletion() {
        String log = """
            [single] 2024-01-15 Dev <dev@test.com>
             1 file changed, 1 insertion(+), 1 deletion(-)
            """;

        List<FileCommit> commits = parser.parseFileLogs(toInputStream(log));
        assertThat(commits).hasSize(1);
        assertThat(commits.getFirst().linesAdded()).isEqualTo(1);
        assertThat(commits.getFirst().linesDeleted()).isEqualTo(1);
    }

    @Test
    void handlesOnlyInsertions() {
        String log = """
            [new] 2024-01-15 Dev <dev@test.com>
             1 file changed, 100 insertions(+)
            """;

        List<FileCommit> commits = parser.parseFileLogs(toInputStream(log));
        assertThat(commits).hasSize(1);
        assertThat(commits.getFirst().linesAdded()).isEqualTo(100);
        assertThat(commits.getFirst().linesDeleted()).isEqualTo(0);
    }

    @Test
    void handlesOnlyDeletions() {
        String log = """
            [del] 2024-01-15 Dev <dev@test.com>
             1 file changed, 50 deletions(-)
            """;

        List<FileCommit> commits = parser.parseFileLogs(toInputStream(log));
        assertThat(commits).hasSize(1);
        assertThat(commits.getFirst().linesAdded()).isEqualTo(0);
        assertThat(commits.getFirst().linesDeleted()).isEqualTo(50);
    }

    @Test
    void parsesLocalDateCorrectly() {
        String log = """
            [date] 2024-12-31 Dev <dev@test.com>
             1 file changed
            """;

        List<FileCommit> commits = parser.parseFileLogs(toInputStream(log));
        assertThat(commits).hasSize(1);
        assertThat(commits.getFirst().date()).isEqualTo(LocalDate.of(2024, 12, 31));
    }

    @Test
    void trimsAuthorName() {
        String log = """
            [trim] 2024-01-15 Author With Spaces   <author@test.com>
             1 file changed, 5 insertions(+)
            """;

        List<FileCommit> commits = parser.parseFileLogs(toInputStream(log));
        assertThat(commits).hasSize(1);
        assertThat(commits.getFirst().author()).isEqualTo("Author With Spaces");
    }

    @Test
    void handlesMultipleCommitsWithVariedStats() {
        String log = """
            [commit1] 2024-01-01 Alice <alice@test.com>
             1 file changed, 10 insertions(+)

            [commit2] 2024-01-02 Bob <bob@test.com>
             1 file changed, 5 deletions(-)

            [commit3] 2024-01-03 Charlie <charlie@test.com>
             1 file changed

            [commit4] 2024-01-04 Diana <diana@test.com>
             1 file changed, 3 insertions(+), 7 deletions(-)
            """;

        List<FileCommit> commits = parser.parseFileLogs(toInputStream(log));
        assertThat(commits).hasSize(4);

        // Commit 1: only insertions
        assertThat(commits.getFirst().linesAdded()).isEqualTo(10);
        assertThat(commits.getFirst().linesDeleted()).isEqualTo(0);

        // Commit 2: only deletions
        assertThat(commits.get(1).linesAdded()).isEqualTo(0);
        assertThat(commits.get(1).linesDeleted()).isEqualTo(5);

        // Commit 3: binary/no stats
        assertThat(commits.get(2).linesAdded()).isEqualTo(0);
        assertThat(commits.get(2).linesDeleted()).isEqualTo(0);

        // Commit 4: both insertions and deletions
        assertThat(commits.get(3).linesAdded()).isEqualTo(3);
        assertThat(commits.get(3).linesDeleted()).isEqualTo(7);
    }

    @Test
    void handlesCommitsWithBlankLinesBetween() {
        String log = """
            [hash1] 2024-01-01 Author <a@test.com>
             1 file changed, 1 insertion(+)



            [hash2] 2024-01-02 Author <a@test.com>
             1 file changed, 2 insertions(+)
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
             1 file changed, 42 insertions(+), 13 deletions(-)
            """;

        List<FileCommit> commits = parser.parseFileLogs(toInputStream(log));
        assertThat(commits).hasSize(1);
        assertThat(commits.getFirst().linesAdded()).isEqualTo(42);
        assertThat(commits.getFirst().linesDeleted()).isEqualTo(13);
    }

    @Test
    void handlesEmailWithSpecialCharacters() {
        String log = """
            [email] 2024-01-15 User Name <user.name+tag@example.co.uk>
             1 file changed, 1 insertion(+)
            """;

        List<FileCommit> commits = parser.parseFileLogs(toInputStream(log));
        assertThat(commits).hasSize(1);
        assertThat(commits.getFirst().email()).isEqualTo("user.name+tag@example.co.uk");
    }

    private InputStream toInputStream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

}
