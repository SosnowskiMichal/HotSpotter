package pwr.zpi.hotspotter.fileanalysis.logprocessing;

import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.common.exception.LogProcessingException;
import pwr.zpi.hotspotter.fileanalysis.logprocessing.model.FileCommit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class FileLogParser {

    private static final Pattern FILE_LOG_PATTERN = Pattern.compile(
            "\\[(?<hash>[^]]+)]\\s" +
            "(?<date>\\d{4}-\\d{2}-\\d{2})\\s" +
            "(?<author>[^<]+?)\\s<(?<email>[^>]+)>\\s*\\n" +
            "(?:(?<added>\\d+|-)\\s+(?<removed>\\d+|-)\\s+(?<path>[^\\n]+))?"
    );
    private static final Pattern FULL_RENAME_PATTERN = Pattern.compile(
            "^(?<old>[^{}]*?)\\s=>\\s(?<current>[^{}]*?)$"
    );
    private static final Pattern PARTIAL_RENAME_PATTERN = Pattern.compile(
            "\\{(?<old>[^{}]*?)\\s=>\\s(?<current>[^{}]*?)}"
    );

    public List<FileCommit> parseFileLogs(InputStream inputStream) {
        List<FileCommit> commits = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder content = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }

            Matcher matcher = FILE_LOG_PATTERN.matcher(content.toString());
            while (matcher.find()) {
                String hash = matcher.group("hash");
                String dateStr = matcher.group("date");
                String author = matcher.group("author").trim();
                String email = matcher.group("email");
                String rawPath = matcher.group("path");
                String path = extractNewPath(rawPath);

                LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);

                Integer linesAdded = parseInteger(matcher.group("added"));
                Integer linesDeleted = parseInteger(matcher.group("removed"));

                commits.add(new FileCommit(hash, date, author, email, path, linesAdded, linesDeleted));
            }

        } catch (IOException e) {
            throw new LogProcessingException("Failed to parse file logs: " + e.getMessage());
        }

        return commits;
    }

    private Integer parseInteger(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String extractNewPath(String rawPath) {
        if (rawPath == null) {
            return null;
        }

        Matcher fullMatcher = FULL_RENAME_PATTERN.matcher(rawPath);
        if (fullMatcher.matches()) {
            return normalizePath(fullMatcher.group("current"));
        }

        Matcher partialMatcher = PARTIAL_RENAME_PATTERN.matcher(rawPath);
        if (!partialMatcher.find()) {
            return normalizePath(rawPath);
        }

        StringBuilder newPath = new StringBuilder();
        partialMatcher.reset();
        int lastEnd = 0;

        while (partialMatcher.find()) {
            newPath.append(rawPath, lastEnd, partialMatcher.start());
            String newPart = partialMatcher.group("current");
            newPath.append(newPart != null ? newPart : "");
            lastEnd = partialMatcher.end();
        }
        newPath.append(rawPath.substring(lastEnd));

        return normalizePath(newPath.toString());
    }

    private String normalizePath(String path) {
        return path.replaceAll("/{2,}", "/").trim();
    }

}
