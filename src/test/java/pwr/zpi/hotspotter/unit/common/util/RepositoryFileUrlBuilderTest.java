package pwr.zpi.hotspotter.unit.common.util;

import org.junit.jupiter.api.Test;
import pwr.zpi.hotspotter.common.util.RepositoryFileUrlBuilder;

import static org.assertj.core.api.Assertions.assertThat;

class RepositoryFileUrlBuilderTest {

    private static final String GITHUB_REPO = "https://github.com/user/repo";
    private static final String GITLAB_REPO = "https://gitlab.com/user/repo";
    private static final String FILE_PATH = "src/main/java/Example.java";
    private static final String COMMIT_HASH = "abc123";

    @Test
    void buildFileUrl_withoutLineNumber_returnsCorrectUrl() {
        String result = RepositoryFileUrlBuilder.buildFileUrl(
                "github", GITHUB_REPO, FILE_PATH, COMMIT_HASH
        );

        assertThat(result)
                .isEqualTo("https://github.com/user/repo/blob/abc123/src/main/java/Example.java");
    }

    @Test
    void buildFileUrl_withSingleLineNumber_returnsCorrectUrl() {
        String result = RepositoryFileUrlBuilder.buildFileUrl(
                "github", GITHUB_REPO, FILE_PATH, COMMIT_HASH, 42
        );

        assertThat(result)
                .isEqualTo("https://github.com/user/repo/blob/abc123/src/main/java/Example.java#L42");
    }

    @Test
    void buildFileUrl_githubWithSingleLine_returnsUrlWithLineFragment() {
        String result = RepositoryFileUrlBuilder.buildFileUrl(
                "github", GITHUB_REPO, FILE_PATH, COMMIT_HASH, 42, 42
        );

        assertThat(result)
                .isEqualTo("https://github.com/user/repo/blob/abc123/src/main/java/Example.java#L42");
    }

    @Test
    void buildFileUrl_githubWithSingleLinePrimitive_returnsUrlWithLineFragment() {
        String result = RepositoryFileUrlBuilder.buildFileUrl(
                "github", GITHUB_REPO, FILE_PATH, COMMIT_HASH, 42, 42
        );

        assertThat(result)
                .isEqualTo("https://github.com/user/repo/blob/abc123/src/main/java/Example.java#L42");
    }

    @Test
    void buildFileUrl_gitlabWithSingleLine_returnsUrlWithLineFragment() {
        String result = RepositoryFileUrlBuilder.buildFileUrl(
                "gitlab", GITLAB_REPO, FILE_PATH, COMMIT_HASH, 42, 42
        );

        assertThat(result)
                .isEqualTo("https://gitlab.com/user/repo/-/blob/abc123/src/main/java/Example.java#L42");
    }

    @Test
    void buildFileUrl_githubWithLineRange_returnsUrlWithRangeFragment() {
        String result = RepositoryFileUrlBuilder.buildFileUrl(
                "github", GITHUB_REPO, FILE_PATH, COMMIT_HASH, 10, 20
        );

        assertThat(result)
                .isEqualTo("https://github.com/user/repo/blob/abc123/src/main/java/Example.java#L10-L20");
    }

    @Test
    void buildFileUrl_githubWithLineRangePrimitive_returnsUrlWithRangeFragment() {
        String result = RepositoryFileUrlBuilder.buildFileUrl(
                "github", GITHUB_REPO, FILE_PATH, COMMIT_HASH, 10, 20
        );

        assertThat(result)
                .isEqualTo("https://github.com/user/repo/blob/abc123/src/main/java/Example.java#L10-L20");
    }

    @Test
    void buildFileUrl_gitlabWithLineRange_returnsUrlWithCorrectFormat() {
        String result = RepositoryFileUrlBuilder.buildFileUrl(
                "gitlab", GITLAB_REPO, FILE_PATH, COMMIT_HASH, 10, 20
        );

        // Note: GitLab uses #L10-20 (no second 'L')
        assertThat(result)
                .isEqualTo("https://gitlab.com/user/repo/-/blob/abc123/src/main/java/Example.java#L10-20");
    }

    @Test
    void buildFileUrl_gitlabWithLineRangePrimitive_returnsUrlWithCorrectFormat() {
        String result = RepositoryFileUrlBuilder.buildFileUrl(
                "gitlab", GITLAB_REPO, FILE_PATH, COMMIT_HASH, 10, 20
        );

        // Note: GitLab uses #L10-20 (no second 'L')
        assertThat(result)
                .isEqualTo("https://gitlab.com/user/repo/-/blob/abc123/src/main/java/Example.java#L10-20");
    }

    @Test
    void buildFileUrl_withEqualStartAndEnd_returnsSingleLineFragment() {
        String result = RepositoryFileUrlBuilder.buildFileUrl(
                "github", GITHUB_REPO, FILE_PATH, COMMIT_HASH, 15, 15
        );

        assertThat(result)
                .isEqualTo("https://github.com/user/repo/blob/abc123/src/main/java/Example.java#L15");
    }

    @Test
    void buildFileUrl_withBackwardsRange_swapsValues() {
        String result = RepositoryFileUrlBuilder.buildFileUrl(
                "github", GITHUB_REPO, FILE_PATH, COMMIT_HASH, 20, 10
        );

        assertThat(result)
                .isEqualTo("https://github.com/user/repo/blob/abc123/src/main/java/Example.java#L10-L20");
    }

    @Test
    void buildFileUrl_gitlabWithBackwardsRange_swapsValues() {
        String result = RepositoryFileUrlBuilder.buildFileUrl(
                "gitlab", GITLAB_REPO, FILE_PATH, COMMIT_HASH, 20, 10
        );

        assertThat(result)
                .isEqualTo("https://gitlab.com/user/repo/-/blob/abc123/src/main/java/Example.java#L10-20");
    }

    @Test
    void buildFileUrl_withOnlyStartLine_returnsSingleLineFragment() {
        String result = RepositoryFileUrlBuilder.buildFileUrl(
                "github", GITHUB_REPO, FILE_PATH, COMMIT_HASH, 42
        );

        assertThat(result)
                .isEqualTo("https://github.com/user/repo/blob/abc123/src/main/java/Example.java#L42");
    }

    @Test
    void buildFileUrl_withInvalidStartLine_returnsUrlWithoutFragment() {
        String result = RepositoryFileUrlBuilder.buildFileUrl(
                "github", GITHUB_REPO, FILE_PATH, COMMIT_HASH, 0, 10
        );

        assertThat(result)
                .isEqualTo("https://github.com/user/repo/blob/abc123/src/main/java/Example.java");
    }

    @Test
    void buildFileUrl_withNegativeStartLine_returnsUrlWithoutFragment() {
        String result = RepositoryFileUrlBuilder.buildFileUrl(
                "github", GITHUB_REPO, FILE_PATH, COMMIT_HASH, -5, 10
        );

        assertThat(result)
                .isEqualTo("https://github.com/user/repo/blob/abc123/src/main/java/Example.java");
    }

    @Test
    void buildFileUrl_withInvalidEndLine_treatAsSingleLine() {
        String result = RepositoryFileUrlBuilder.buildFileUrl(
                "github", GITHUB_REPO, FILE_PATH, COMMIT_HASH, 42, 0
        );

        assertThat(result)
                .isEqualTo("https://github.com/user/repo/blob/abc123/src/main/java/Example.java#L42");
    }

    @Test
    void buildFileUrl_withNegativeEndLine_treatAsSingleLine() {
        String result = RepositoryFileUrlBuilder.buildFileUrl(
                "github", GITHUB_REPO, FILE_PATH, COMMIT_HASH, 42, -3
        );

        assertThat(result)
                .isEqualTo("https://github.com/user/repo/blob/abc123/src/main/java/Example.java#L42");
    }

    @Test
    void buildFileUrl_withUnsupportedPlatform_returnsNull() {
        String result = RepositoryFileUrlBuilder.buildFileUrl(
                "bitbucket", "https://bitbucket.org/user/repo", FILE_PATH, COMMIT_HASH, 10, 20
        );

        assertThat(result).isNull();
    }

    @Test
    void buildFileUrl_withNullPlatform_returnsNull() {
        String result = RepositoryFileUrlBuilder.buildFileUrl(
                null, GITHUB_REPO, FILE_PATH, COMMIT_HASH, 10, 20
        );

        assertThat(result).isNull();
    }

    @Test
    void buildFileUrl_withCaseInsensitivePlatform_github_returnsCorrectUrl() {
        String result = RepositoryFileUrlBuilder.buildFileUrl(
                "GITHUB", GITHUB_REPO, FILE_PATH, COMMIT_HASH, 42, 42
        );

        assertThat(result)
                .isEqualTo("https://github.com/user/repo/blob/abc123/src/main/java/Example.java#L42");
    }

    @Test
    void buildFileUrl_withCaseInsensitivePlatform_gitlab_returnsCorrectUrl() {
        String result = RepositoryFileUrlBuilder.buildFileUrl(
                "GitLab", GITLAB_REPO, FILE_PATH, COMMIT_HASH, 42, 42
        );

        assertThat(result)
                .isEqualTo("https://gitlab.com/user/repo/-/blob/abc123/src/main/java/Example.java#L42");
    }

    @Test
    void buildFileUrl_withNullRepositoryUrl_returnsNull() {
        String result = RepositoryFileUrlBuilder.buildFileUrl(
                "github", null, FILE_PATH, COMMIT_HASH, 10, 20
        );

        assertThat(result).isNull();
    }

    @Test
    void buildFileUrl_withBlankRepositoryUrl_returnsNull() {
        String result = RepositoryFileUrlBuilder.buildFileUrl(
                "github", "   ", FILE_PATH, COMMIT_HASH, 10, 20
        );

        assertThat(result).isNull();
    }

    @Test
    void buildFileUrl_withNullFilePath_returnsNull() {
        String result = RepositoryFileUrlBuilder.buildFileUrl(
                "github", GITHUB_REPO, null, COMMIT_HASH, 10, 20
        );

        assertThat(result).isNull();
    }

    @Test
    void buildFileUrl_withBlankFilePath_returnsNull() {
        String result = RepositoryFileUrlBuilder.buildFileUrl(
                "github", GITHUB_REPO, "   ", COMMIT_HASH, 10, 20
        );

        assertThat(result).isNull();
    }

    @Test
    void buildFileUrl_withNullCommitHash_returnsNull() {
        String result = RepositoryFileUrlBuilder.buildFileUrl(
                "github", GITHUB_REPO, FILE_PATH, null, 10, 20
        );

        assertThat(result).isNull();
    }

    @Test
    void buildFileUrl_withBlankCommitHash_returnsNull() {
        String result = RepositoryFileUrlBuilder.buildFileUrl(
                "github", GITHUB_REPO, FILE_PATH, "   ", 10, 20
        );

        assertThat(result).isNull();
    }

    @Test
    void buildFileUrl_withWindowsPath_normalizesToUnixPath() {
        String result = RepositoryFileUrlBuilder.buildFileUrl(
                "github", GITHUB_REPO, "src\\main\\java\\Example.java", COMMIT_HASH, 42, 42
        );

        assertThat(result)
                .isEqualTo("https://github.com/user/repo/blob/abc123/src/main/java/Example.java#L42");
    }

    @Test
    void buildFileUrl_withLeadingSlash_removesSlash() {
        String result = RepositoryFileUrlBuilder.buildFileUrl(
                "github", GITHUB_REPO, "/src/main/java/Example.java", COMMIT_HASH, 42, 42
        );

        assertThat(result)
                .isEqualTo("https://github.com/user/repo/blob/abc123/src/main/java/Example.java#L42");
    }

    @Test
    void buildFileUrl_withMultipleLeadingSlashes_removesSlashes() {
        String result = RepositoryFileUrlBuilder.buildFileUrl(
                "github", GITHUB_REPO, "///src/main/java/Example.java", COMMIT_HASH, 42, 42
        );

        assertThat(result)
                .isEqualTo("https://github.com/user/repo/blob/abc123/src/main/java/Example.java#L42");
    }

    @Test
    void buildFileUrl_githubWithoutLineNumbers_returnsUrlWithoutFragment() {
        String result = RepositoryFileUrlBuilder.buildFileUrl(
                "github", GITHUB_REPO, FILE_PATH, COMMIT_HASH
        );

        assertThat(result)
                .isEqualTo("https://github.com/user/repo/blob/abc123/src/main/java/Example.java");
    }

    @Test
    void buildFileUrl_gitlabWithoutLineNumbers_returnsUrlWithoutFragment() {
        String result = RepositoryFileUrlBuilder.buildFileUrl(
                "gitlab", GITLAB_REPO, FILE_PATH, COMMIT_HASH
        );

        assertThat(result)
                .isEqualTo("https://gitlab.com/user/repo/-/blob/abc123/src/main/java/Example.java");
    }

}
