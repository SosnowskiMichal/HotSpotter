package pwr.zpi.hotspotter.repositoryanalysis.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RepositoryFileUrlBuilder {

    private static final String GITHUB_PATH_FORMAT = "/blob/%s/%s";
    private static final String GITLAB_PATH_FORMAT = "/-/blob/%s/%s";

    public static String buildFileUrl(
            String platform,
            String repositoryUrl,
            String filePath,
            String commitHash
    ) {
        if (!isValidInput(platform, repositoryUrl, filePath, commitHash)) {
            return null;
        }

        String normalizedPath = normalizeFilePath(filePath);

        if (isPlatform(platform, "github")) {
            return buildGitHubUrl(repositoryUrl, commitHash, normalizedPath);
        } else if (isPlatform(platform, "gitlab")) {
            return buildGitLabUrl(repositoryUrl, commitHash, normalizedPath);
        }

        return null;
    }

    private static String buildGitHubUrl(String repositoryUrl, String commitHash, String filePath) {
        return repositoryUrl + String.format(GITHUB_PATH_FORMAT, commitHash, filePath);
    }

    private static String buildGitLabUrl(String repositoryUrl, String commitHash, String filePath) {
        return repositoryUrl + String.format(GITLAB_PATH_FORMAT, commitHash, filePath);
    }

    private static String normalizeFilePath(String filePath) {
        if (filePath == null) return "";
        return filePath.replace("\\", "/").replaceFirst("^/+", "");
    }

    private static boolean isPlatform(String platform, String expected) {
        if (platform == null) return false;
        return platform.equalsIgnoreCase(expected);
    }

    private static boolean isValidInput(
            String platform,
            String repositoryUrl,
            String filePath,
            String commitHash
    ) {
        return platform != null && !platform.isBlank()
                && repositoryUrl != null && !repositoryUrl.isBlank()
                && filePath != null && !filePath.isBlank()
                && commitHash != null && !commitHash.isBlank();
    }

}
