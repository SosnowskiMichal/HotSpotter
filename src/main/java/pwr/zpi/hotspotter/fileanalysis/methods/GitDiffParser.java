package pwr.zpi.hotspotter.fileanalysis.methods;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.fileanalysis.methods.model.LineRange;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class GitDiffParser {

    private static final Pattern HUNK_HEADER_PATTERN = Pattern.compile(
            "@@ -(?<oldStart>\\d+)(?:,(?<oldCount>\\d+))? \\+(?<newStart>\\d+)(?:,(?<newCount>\\d+))? @@"
    );

    public List<LineRange> parseChangedLineRanges(InputStream inputStream) {
        List<LineRange> lineRanges = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;

            while ((line = reader.readLine()) != null) {
                Matcher matcher = HUNK_HEADER_PATTERN.matcher(line);

                if (matcher.find()) {
                    int newStart = Integer.parseInt(matcher.group("newStart"));
                    String newCountStr = matcher.group("newCount");

                    int newCount = (newCountStr != null) ? Integer.parseInt(newCountStr) : 1;

                    if (newCount > 0) {
                        int endLine = newStart + newCount - 1;
                        lineRanges.add(new LineRange(newStart, endLine));
                    }
                }
            }

        } catch (IOException e) {
            log.error("Failed to parse git diff output: {}", e.getMessage());
        }

        return lineRanges;
    }

}
