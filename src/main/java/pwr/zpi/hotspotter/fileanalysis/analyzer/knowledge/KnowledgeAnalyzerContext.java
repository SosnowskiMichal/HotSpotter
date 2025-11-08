package pwr.zpi.hotspotter.fileanalysis.analyzer.knowledge;

import lombok.Getter;
import pwr.zpi.hotspotter.fileanalysis.analyzer.knowledge.model.AuthorContribution;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Getter
public class KnowledgeAnalyzerContext {

    private final String analysisId;
    private final Path repositoryPath;
    private final Map<String, Map<String, AuthorContribution>> fileContributions;

    public KnowledgeAnalyzerContext(String analysisId, Path repositoryPath) {
        this.analysisId = analysisId;
        this.repositoryPath = repositoryPath;
        this.fileContributions = new HashMap<>();
    }

    public void recordContribution(String filePath, String name, int linesAdded) {
        fileContributions
                .computeIfAbsent(filePath, _ -> new HashMap<>())
                .compute(name, (_, contribution) -> {
                    if (contribution == null) {
                        contribution = new AuthorContribution(name);
                    }
                    contribution.increaseLinesAdded(linesAdded);
                    contribution.incrementCommits();
                    return contribution;
                });
    }

    public void updateFilePath(String oldPath, String newPath) {
        if (newPath == null || newPath.isBlank()) {
            fileContributions.remove(oldPath);
            return;
        }

        Map<String, AuthorContribution> contributions = fileContributions.remove(oldPath);
        if (contributions != null) {
            fileContributions.put(newPath, contributions);
        }
    }

}
