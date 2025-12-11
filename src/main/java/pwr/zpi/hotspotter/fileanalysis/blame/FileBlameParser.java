package pwr.zpi.hotspotter.fileanalysis.blame;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.common.exception.AnalysisException;
import pwr.zpi.hotspotter.fileanalysis.blame.model.FileAuthorStatistics;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class FileBlameParser {

    public List<FileAuthorStatistics> parseBlameOutput(InputStream inputStream, long totalLines) {
        Map<String, Integer> authorsMap = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            String currentAuthor = null;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("author ")) {
                    currentAuthor = line.substring("author ".length()).trim();

                } else if (line.startsWith("\t")) {
                    if (currentAuthor != null) {
                        authorsMap.merge(currentAuthor, 1, Integer::sum);
                    }
                }
            }

        } catch (IOException e) {
            throw new AnalysisException("Failed to parse git blame output: " + e.getMessage());
        }

        return authorsMap.entrySet().stream()
                .map(entry -> FileAuthorStatistics.builder()
                        .authorName(entry.getKey())
                        .linesAuthored(entry.getValue())
                        .percentage(calculatePercentage(entry.getValue(), totalLines))
                        .build()
                )
                .sorted(Comparator.comparing(FileAuthorStatistics::getLinesAuthored).reversed())
                .toList();
    }

    private double calculatePercentage(int linesAuthored, long totalLines) {
        if (totalLines == 0) return 0.0;
        return Math.round((linesAuthored * 100.0 / totalLines) * 100.0) / 100.0;
    }

}
