package pwr.zpi.hotspotter.repositoryanalysis.analyzer.coupling;

import lombok.Getter;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class CouplingAnalyzerContext {

    // TODO: Temporary constants, replace with user preferences
    private static final int MAX_FILES_PER_COMMIT = 25;
    private static final int AUTHOR_COUPLING_ANALYSIS_PERIOD_MONTHS = 12;

    private final String analysisId;
    private final Path repositoryPath;
    private final LocalDate referenceDate;
    private final LocalDate authorCouplingAnalysisStartDate;

    private final Map<String, Integer> fileCommits = new HashMap<>();
    private final Map<String, Map<String, Integer>> fileCouplings = new HashMap<>();

    private final Map<String, Map<String, Integer>> authorFileChanges = new HashMap<>();

    public CouplingAnalyzerContext(String analysisId, Path repositoryPath, LocalDate referenceDate) {
        this.analysisId = analysisId;
        this.repositoryPath = repositoryPath;
        this.referenceDate = referenceDate != null ? referenceDate : LocalDate.now();
        this.authorCouplingAnalysisStartDate = this.referenceDate.minusMonths(AUTHOR_COUPLING_ANALYSIS_PERIOD_MONTHS);
    }

    public void recordCoupling(String author, LocalDate date, List<String> changedFiles) {
        if (changedFiles == null || changedFiles.isEmpty()) return;
        if (changedFiles.size() > MAX_FILES_PER_COMMIT) return;

        for (String file : changedFiles) {
            fileCommits.merge(file, 1, Integer::sum);
        }

        for (int i = 0; i < changedFiles.size(); i++) {
            String filePath1 = changedFiles.get(i);
            for (int j = i + 1; j < changedFiles.size(); j++) {
                String filePath2 = changedFiles.get(j);

                recordCouplingPair(filePath1, filePath2);
                recordCouplingPair(filePath2, filePath1);
            }
        }

        if (author != null && !author.isBlank() && date != null) {
            if (!date.isBefore(authorCouplingAnalysisStartDate)) {
                Map<String, Integer> fileChanges = authorFileChanges.computeIfAbsent(author, _ -> new HashMap<>());
                for (String file : changedFiles) {
                    fileChanges.merge(file, 1, Integer::sum);
                }
            }
        }
    }

    public void updateFilePath(String oldPath, String newPath) {
        if (newPath == null || newPath.isBlank()) {
            fileCommits.remove(oldPath);
            fileCouplings.remove(oldPath);

            for (Map<String, Integer> couplings : fileCouplings.values()) {
                couplings.remove(oldPath);
            }
            return;
        }

        if (fileCommits.containsKey(oldPath)) {
            int commits = fileCommits.remove(oldPath);
            fileCommits.put(newPath, commits);
        }

        if (fileCouplings.containsKey(oldPath)) {
            Map<String, Integer> couplings = fileCouplings.remove(oldPath);
            fileCouplings.put(newPath, couplings);
        }

        for (Map<String, Integer> couplings : fileCouplings.values()) {
            if (couplings.containsKey(oldPath)) {
                int commits = couplings.remove(oldPath);
                couplings.put(newPath, commits);
            }
        }

        for (Map<String, Integer> fileChanges : authorFileChanges.values()) {
            if (fileChanges.containsKey(oldPath)) {
                int changes = fileChanges.remove(oldPath);
                fileChanges.put(newPath, changes);
            }
        }
    }

    private void recordCouplingPair(String filePath1, String filePath2) {
        fileCouplings
                .computeIfAbsent(filePath1, _ -> new HashMap<>())
                .merge(filePath2, 1, Integer::sum);
    }

}
