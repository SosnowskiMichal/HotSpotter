package pwr.zpi.hotspotter.fileanalysis.complexity.component;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.fileanalysis.complexity.model.FileComplexityReport;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Slf4j
@Component
public class HeuristicStrategy {

    private static final Pattern C_STYLE_PATTERN = Pattern.compile("//.*|/\\*[\\s\\S]*?\\*/");
    private static final Pattern HASH_STYLE_PATTERN = Pattern.compile("#.*");

    private static final Pattern STRING_PATTERN = Pattern.compile("\"(?:[^\"\\\\]|\\\\.)*\"|'(?:[^'\\\\]|\\\\.)*'");

    private static final Set<String> HASH_STYLE_EXTENSIONS = Set.of(
            "py", "rb", "sh", "yaml", "yml", "r", "pl", "bash"
    );

    private static final Set<String> UNIVERSAL_KEYWORDS = Set.of(
            "if", "else", "elif", "for", "foreach", "while", "case",
            "catch", "except", "try", "&&", "||", "and", "or", "?"
    );

    public Map<String, FileComplexityReport> analyze(Path path) {
        Map<String, FileComplexityReport> reportMap = new HashMap<>();

        try (Stream<Path> paths = Files.walk(path)) {
            paths.filter(Files::isRegularFile)
                    .forEach(filePath -> processFile(filePath, reportMap));
        } catch (IOException e) {
            throw new RuntimeException("Heuristic batch analysis failed", e);
        }

        return reportMap;
    }

    private void processFile(Path path, Map<String, FileComplexityReport> reportMap) {
        try {
            String extension = FilenameUtils.getExtension(path.toString());
            String content = readFileContent(path);
            content = removeStrings(content);
            content = removeComments(content, extension);

            int complexity = countKeywords(content) + 1;
            String key = FilenameUtils.getBaseName(path.toString());
            FileComplexityReport report = reportMap.computeIfAbsent(key, _ -> new FileComplexityReport());
            report.updateCcnStats(complexity);

        } catch (IOException e) {
            log.error("Error while analyzing file {}", path, e);
        }
    }

    private String removeStrings(String content) {
        return STRING_PATTERN.matcher(content).replaceAll(" ");
    }

    private String removeComments(String content, String extension) {
        Pattern pattern = HASH_STYLE_EXTENSIONS.contains(extension) ? HASH_STYLE_PATTERN : C_STYLE_PATTERN;
        Matcher matcher = pattern.matcher(content);
        return matcher.replaceAll(" ");
    }

    private int countKeywords(String content) {
        int count = 0;
        String[] tokens = content.split("[^a-zA-Z0-9_&|?]+");
        for (String token : tokens) {
            if (UNIVERSAL_KEYWORDS.contains(token)) {
                count++;
            }
        }
        return count;
    }

    private String readFileContent(Path path) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(path.toFile()))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }
}