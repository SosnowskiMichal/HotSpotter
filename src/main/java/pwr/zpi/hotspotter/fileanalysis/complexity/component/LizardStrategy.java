package pwr.zpi.hotspotter.fileanalysis.complexity.component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.fileanalysis.complexity.model.FileComplexityReport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class LizardStrategy {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "c", "cpp", "cc", "h", "hpp", "cxx", "m", "mm", "java", "kt", "kts",
            "cs", "js", "jsx", "ts", "tsx", "py", "rb", "php", "scala", "swift",
            "lua", "rs", "go", "sol", "gd"
    );

    private static final Pattern LIZARD_LINE_PATTERN = Pattern.compile(
            "^\\d+," +
            "(?<ccn>\\d+)," +
            "\\d+," +
            "(?<params>\\d+)," +
            "\\d+," +
            "\"[^\"]*\"," +
            "\"(?<filePath>[^\"]*)\"," +
            "\"(?<functionName>[^\"]*)\"," +
            "\"[^\"]*\"," +
            "(?<startLine>\\d+)," +
            "(?<endLine>\\d+)$"
    );

    public boolean isSupported(String extension) {
        return SUPPORTED_EXTENSIONS.contains(extension.toLowerCase());
    }

    public Map<String, FileComplexityReport> analyze(Path path) {
        Map<String, FileComplexityReport> reportMap = new HashMap<>();

        try {
            ProcessBuilder pb = new ProcessBuilder("lizard", "--csv", ".");
            pb.directory(path.toFile());
            pb.redirectErrorStream(true);

            Process process = pb.start();
            InputStream inputStream = process.getInputStream();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (isDataLine(line)) {
                        processLine(line, reportMap);
                    }
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Lizard process exited with code: " + exitCode);
            }

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new RuntimeException("Lizard analysis failed", e);
        }

        return reportMap;
    }

    private boolean isDataLine(String line) {
        return line != null && !line.trim().isEmpty() && Character.isDigit(line.trim().charAt(0));
    }

    private void processLine(String line, Map<String, FileComplexityReport> reportMap) {
        Matcher matcher = LIZARD_LINE_PATTERN.matcher(line);

        if (matcher.matches()) {
            try {
                int ccn = Integer.parseInt(matcher.group("ccn"));
                int params = Integer.parseInt(matcher.group("params"));
                String fullFilePath = matcher.group("filePath");
                String functionName = matcher.group("functionName");
                int startLine = Integer.parseInt(matcher.group("startLine"));
                int endLine = Integer.parseInt(matcher.group("endLine"));

                String key = FilenameUtils.getBaseName(fullFilePath);
                reportMap.computeIfAbsent(key, _ -> new FileComplexityReport())
                        .addMethodStats(functionName, ccn, params, startLine, endLine);

            } catch (Exception _) { }
        }
    }

}