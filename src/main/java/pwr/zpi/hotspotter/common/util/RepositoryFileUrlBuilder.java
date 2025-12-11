package pwr.zpi.hotspotter.common.util;

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
        return buildFileUrl(platform, repositoryUrl, filePath, commitHash, null, null);
    }

    public static String buildFileUrl(
            String platform,
            String repositoryUrl,
            String filePath,
            String commitHash,
            int lineNumber
    ) {
        return buildFileUrl(platform, repositoryUrl, filePath, commitHash, lineNumber, null);
    }

    public static String buildFileUrl(
            String platform,
            String repositoryUrl,
            String filePath,
            String commitHash,
            int startLine,
            int endLine
    ) {
        return buildFileUrl(platform, repositoryUrl, filePath, commitHash, Integer.valueOf(startLine), Integer.valueOf(endLine));
    }

    private static String buildFileUrl(
            String platform,
            String repositoryUrl,
            String filePath,
            String commitHash,
            Integer startLine,
            Integer endLine
    ) {
        if (!isValidInput(platform, repositoryUrl, filePath, commitHash)) return null;

        String normalizedPath = normalizeFilePath(filePath);
        String lineFragment = buildLineFragment(startLine, endLine, platform);

        if (isPlatform(platform, "github")) {
            return buildGitHubUrl(repositoryUrl, commitHash, normalizedPath, lineFragment);
        } else if (isPlatform(platform, "gitlab")) {
            return buildGitLabUrl(repositoryUrl, commitHash, normalizedPath, lineFragment);
        }

        return null;
    }

    private static String buildGitHubUrl(String repositoryUrl, String commitHash, String filePath, String lineFragment) {
        return repositoryUrl + String.format(GITHUB_PATH_FORMAT, commitHash, filePath) + lineFragment;
    }

    private static String buildGitLabUrl(String repositoryUrl, String commitHash, String filePath, String lineFragment) {
        return repositoryUrl + String.format(GITLAB_PATH_FORMAT, commitHash, filePath) + lineFragment;
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

    private static String buildLineFragment(Integer startLine, Integer endLine, String platform) {
        if (startLine == null || startLine <= 0) {
            return "";
        }

        int start = startLine;
        int end = (endLine == null || endLine <= 0) ? startLine : endLine;

        if (end < start) {
            int temp = start;
            start = end;
            end = temp;
        }

        if (isPlatform(platform, "github")) {
            return formatGitHubLineFragment(start, end);
        } else if (isPlatform(platform, "gitlab")) {
            return formatGitLabLineFragment(start, end);
        }

        return "";
    }

    private static String formatGitHubLineFragment(int startLine, int endLine) {
        if (startLine == endLine) {
            return "#L" + startLine;
        }
        return "#L" + startLine + "-L" + endLine;
    }

    private static String formatGitLabLineFragment(int startLine, int endLine) {
        if (startLine == endLine) {
            return "#L" + startLine;
        }
        return "#L" + startLine + "-" + endLine;
    }

}
