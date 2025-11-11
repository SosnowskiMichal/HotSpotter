package pwr.zpi.hotspotter.repositorymanagement.parser;

import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.repositorymanagement.exception.InvalidRepositoryUrlException;

import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RepositoryUrlParser {

    private static final Pattern REPOSITORY_URL_PATTERN = Pattern.compile(
            "^(?:https://)?(?<platform>git(?:hub|lab))\\.com/"
            + "(?<owner>[^/]+)/(?<name>(?!\\.git$)[^/]+?)(?:\\.git)*$"
    );

    public RepositoryData parse(String repositoryUrl) throws IllegalArgumentException {
        Matcher matcher = REPOSITORY_URL_PATTERN.matcher(repositoryUrl);
        if (!matcher.matches()) {
            throw new InvalidRepositoryUrlException("Invalid repository URL: " + repositoryUrl
                    + ", only GitHub and GitLab HTTPS URLs are supported");
        }

        String platform = matcher.group("platform");
        String owner = matcher.group("owner");
        String name = matcher.group("name");
        repositoryUrl = normalizeRepositoryUrl(repositoryUrl);

        return new RepositoryData(repositoryUrl, platform, owner, name);
    }

    private String normalizeRepositoryUrl(String repositoryUrl) {
        if (!repositoryUrl.startsWith("http")) {
            repositoryUrl = "https://" + repositoryUrl;
        }
        if (repositoryUrl.endsWith(".git")) {
            repositoryUrl = repositoryUrl.substring(0, repositoryUrl.length() - 4);
        }
        return repositoryUrl;
    }

    public record RepositoryData(String repositoryUrl, String platform, String owner, String name) {
        public String getPath() {
            return Path.of(platform, owner, name).toString();
        }
    }

}
