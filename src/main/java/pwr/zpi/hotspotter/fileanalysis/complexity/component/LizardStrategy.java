package pwr.zpi.hotspotter.fileanalysis.complexity.component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.common.utils.FileUtils;
import pwr.zpi.hotspotter.fileanalysis.complexity.model.FileComplexityReport;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class LizardStrategy {

    private final LizardRunner lizardRunner;

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "c", "cpp", "cc", "h", "hpp", "cxx", "m", "mm", "java", "kt", "kts",
            "cs", "js", "jsx", "ts", "tsx", "py", "rb", "php", "scala", "swift",
            "lua", "rs", "go", "sol", "gd"
    );

    private static final int MIN_PARTS_LENGTH = 8;
    private static final int CCN_INDEX = 1;
    private static final int PARAM_INDEX = 3;
    private static final int FILE_PATH_INDEX = 6;
    private static final int FUNCTION_NAME_INDEX = 7;
    private static final int START_LINE_INDEX = 9;
    private static final int END_LINE_INDEX = 10;

    public boolean isSupported(String extension) {
        return SUPPORTED_EXTENSIONS.contains(extension.toLowerCase());
    }

    public Map<String, FileComplexityReport> analyze(Path path) {
        Map<String, FileComplexityReport> reportMap = new HashMap<>();

        try {
            List<String> lizardOutput = lizardRunner.runLizard(path);

            for (String line : lizardOutput) {
                if (!isDataLine(line)) continue;

                String[] parts = line.split(",");
                if (parts.length >= MIN_PARTS_LENGTH) {
                    try {
                        int ccn = parseSafeInt(parts[CCN_INDEX]);
                        int params = parseSafeInt(parts[PARAM_INDEX]);
                        String fullFilePath = parts[FILE_PATH_INDEX].trim();
                        String functionName = parts[FUNCTION_NAME_INDEX].trim();

                        int startLine = (parts.length >= START_LINE_INDEX) ? parseSafeInt(parts[START_LINE_INDEX]) : 0;
                        int endLine = (parts.length >= END_LINE_INDEX) ? parseSafeInt(parts[END_LINE_INDEX]) : 0;

                        String key = FileUtils.getFileNameWithoutExtension(Path.of(fullFilePath));
                        reportMap.computeIfAbsent(key, _ -> new FileComplexityReport())
                                .addFunctionStats(functionName, ccn, params, startLine, endLine);

                    } catch (Exception e) {
                        log.warn("Parsing error for line: [{}]. Error: {}", line, e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Lizard analysis failed", e);
        }

        return reportMap;
    }

    private boolean isDataLine(String line) {
        return line != null && !line.trim().isEmpty() && Character.isDigit(line.trim().charAt(0));
    }

    private int parseSafeInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}