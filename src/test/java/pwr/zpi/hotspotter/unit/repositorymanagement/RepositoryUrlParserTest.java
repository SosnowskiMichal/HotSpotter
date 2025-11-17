package pwr.zpi.hotspotter.unit.repositorymanagement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import pwr.zpi.hotspotter.repositorymanagement.exception.InvalidRepositoryUrlException;
import pwr.zpi.hotspotter.repositorymanagement.parser.RepositoryUrlParser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class RepositoryUrlParserTest {

    private final RepositoryUrlParser repositoryUrlParser = new RepositoryUrlParser();

    @Test
    void parseShouldExtractRepositoryDataForValidGitHubUrl() {
        String repositoryUrl = "https://github.com/user/repo";

        RepositoryUrlParser.RepositoryData result = repositoryUrlParser.parse(repositoryUrl);

        assertEquals("github", result.platform());
        assertEquals("user", result.owner());
        assertEquals("repo", result.name());
        assertEquals("https://github.com/user/repo", result.repositoryUrl());
    }

    @Test
    void parseShouldExtractRepositoryDataForValidGitLabUrl() {
        String repositoryUrl = "https://gitlab.com/user/repo";

        RepositoryUrlParser.RepositoryData result = repositoryUrlParser.parse(repositoryUrl);

        assertEquals("gitlab", result.platform());
        assertEquals("user", result.owner());
        assertEquals("repo", result.name());
        assertEquals("https://gitlab.com/user/repo", result.repositoryUrl());
    }

    @Test
    void parseShouldNormalizeUrlWithoutHttps() {
        String repositoryUrl = "github.com/user/repo";

        RepositoryUrlParser.RepositoryData result = repositoryUrlParser.parse(repositoryUrl);

        assertEquals("https://github.com/user/repo", result.repositoryUrl());
    }

    @Test
    void parseShouldNormalizeUrlWithTrailingGitExtension() {
        String repositoryUrl = "https://github.com/user/repo.git";

        RepositoryUrlParser.RepositoryData result = repositoryUrlParser.parse(repositoryUrl);

        assertEquals("https://github.com/user/repo", result.repositoryUrl());
    }

    @Test
    void parseShouldThrowExceptionForInvalidUrl() {
        String repositoryUrl = "https://invalid.com/user/repo";

        assertThrows(InvalidRepositoryUrlException.class, () -> repositoryUrlParser.parse(repositoryUrl));
    }

    @Test
    void parseShouldThrowExceptionForMalformedUrl() {
        String repositoryUrl = "not-a-valid-url";

        assertThrows(InvalidRepositoryUrlException.class, () -> repositoryUrlParser.parse(repositoryUrl));
    }
}

