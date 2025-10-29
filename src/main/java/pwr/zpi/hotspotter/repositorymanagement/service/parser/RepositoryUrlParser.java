package pwr.zpi.hotspotter.repositorymanagement.service.parser;

import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RepositoryUrlParser {

    private static final Pattern REPOSITORY_URL_PATTERN = Pattern.compile(
            "^(?:https://(?<platformHttps>git(?:hub|lab))\\.com/|(?<platformSsh>git@git(?:hub|lab))\\.com:)"
            + "(?<owner>[^/]+)/(?<name>(?!\\.git$)[^/]+?)(?:\\.git)*$"
    );

    public RepositoryData parse(String repositoryUrl) {
        Matcher matcher = REPOSITORY_URL_PATTERN.matcher(repositoryUrl);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid repository URL: " + repositoryUrl
                    + ", only GitHub and GitLab URLs are supported");
        }

        String platform = Objects.requireNonNullElse(matcher.group("platformHttps"), matcher.group("platformSsh"));
        String owner = matcher.group("owner");
        String name = matcher.group("name");
        return new RepositoryData(repositoryUrl, platform, owner, name);
    }

    public record RepositoryData(String repositoryUrl, String platform, String owner, String name) {
        public String getPath() {
            return Path.of(platform, owner, name).toString();
        }
    }

}
