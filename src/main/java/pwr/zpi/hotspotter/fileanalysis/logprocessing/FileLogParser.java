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
            "(?:\\s*1 files? changed(?:, (?<added>\\d+) insertions?\\(\\+\\))?(?:, (?<removed>\\d+) deletions?\\(-\\))?)?"
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

                LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);

                Integer linesAdded = parseInteger(matcher.group("added"));
                Integer linesDeleted = parseInteger(matcher.group("removed"));

                commits.add(new FileCommit(hash, date, author, email, linesAdded, linesDeleted));
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

}
