package pwr.zpi.hotspotter.fileanalysis.analyzer.knowledge;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.fileanalysis.analyzer.knowledge.model.AuthorContribution;
import pwr.zpi.hotspotter.fileanalysis.analyzer.knowledge.model.FileKnowledge;
import pwr.zpi.hotspotter.fileanalysis.analyzer.knowledge.repository.FileKnowledgeRepository;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.authors.model.AuthorStatistics;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.authors.repository.AuthorStatisticsRepository;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.model.Commit;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.model.FileChange;
import pwr.zpi.hotspotter.repositoryanalysis.util.AnalysisUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeAnalyzer {

    private final FileKnowledgeRepository fileKnowledgeRepository;
    private final AuthorStatisticsRepository authorStatisticsRepository;

    public KnowledgeAnalyzerContext startAnalysis(String analysisId, Path repositoryPath) {
        log.debug("Starting knowledge analysis for ID {}", analysisId);
        return new KnowledgeAnalyzerContext(analysisId, repositoryPath);
    }

    public void processCommit(Commit commit, KnowledgeAnalyzerContext context) {
        if (commit == null || context == null) return;

        String author = commit.author();

        for (FileChange fileChange : commit.changedFiles()) {
            String filePath = fileChange.filePath();
            int linesAdded = fileChange.linesAdded();

            if (fileChange.isRenamed()) {
                updateFilePath(fileChange.oldPath(), fileChange.newPath(), context);
                filePath = fileChange.newPath();
            }

            context.recordContribution(filePath, author, linesAdded);
        }
    }

    public void finishAnalysis(KnowledgeAnalyzerContext context) {
        if (context == null || context.getRepositoryPath() == null) return;

        try {
            Set<String> existingFiles = getExistingFiles(context.getRepositoryPath());
            List<FileKnowledge> knowledgeData = new ArrayList<>();

            for (Map.Entry<String, Map<String, AuthorContribution>> entry : context.getFileContributions().entrySet()) {
                String filePath = entry.getKey();
                if (!existingFiles.contains(filePath)) continue;
                Map<String, AuthorContribution> authorContributions = entry.getValue();

                FileKnowledge fileKnowledge = calculateFileKnowledge(
                        context.getAnalysisId(),
                        filePath,
                        authorContributions
                );

                knowledgeData.add(fileKnowledge);

                if (knowledgeData.size() >= AnalysisUtils.DEFAULT_SAVE_BATCH_SIZE) {
                    AnalysisUtils.saveDataInBatches(fileKnowledgeRepository, knowledgeData);
                    knowledgeData.clear();
                }
            }

            if (!knowledgeData.isEmpty()) {
                AnalysisUtils.saveDataInBatches(fileKnowledgeRepository, knowledgeData);
                log.debug("Saved final batch of {} file knowledge data records", knowledgeData.size());
            }

        } catch (Exception e) {
            log.error("Error saving knowledge analysis data for ID: {}: {}", context.getAnalysisId(), e.getMessage(), e);
        }
    }

    public void enrichAnalysisData(KnowledgeAnalyzerContext context) {
        if (context == null) return;

        List<FileKnowledge> fileKnowledgeData = fileKnowledgeRepository.findAllByAnalysisId(context.getAnalysisId());
        List<AuthorStatistics> authorStatistics = authorStatisticsRepository.findAllByAnalysisId(context.getAnalysisId());

        Map<String, Boolean> authorActivityMap = authorStatistics.stream()
                .collect(Collectors.toMap(
                        AuthorStatistics::getName,
                        AuthorStatistics::getIsActive
                ));

        for (FileKnowledge fileKnowledge : fileKnowledgeData) {
            int totalLinesAdded = fileKnowledge.getLinesAdded();

            if (totalLinesAdded == 0) {
                fileKnowledge.setKnowledgeLoss(0.0);
                continue;
            }

            List<AuthorContribution> contributions = fileKnowledge.getAuthorContributions();
            if (contributions == null || contributions.isEmpty()) {
                fileKnowledge.setKnowledgeLoss(0.0);
                continue;
            }

            int linesAddedByInactiveAuthors = contributions.stream()
                    .filter(contribution -> !authorActivityMap.get(contribution.getName()))
                    .mapToInt(AuthorContribution::getLinesAdded)
                    .sum();

            double knowledgeLoss = linesAddedByInactiveAuthors * 100.0 / totalLinesAdded;
            fileKnowledge.setKnowledgeLoss(knowledgeLoss);
        }

        try {
            AnalysisUtils.saveDataInBatches(fileKnowledgeRepository, fileKnowledgeData);
        } catch (Exception e) {
            log.error("Error saving enriched knowledge analysis data for ID {}: {}", context.getAnalysisId(), e.getMessage(), e);
        }
    }

    private void updateFilePath(String oldPath, String newPath, KnowledgeAnalyzerContext context) {
        context.updateFilePath(oldPath, newPath);
    }

    private Set<String> getExistingFiles(Path repositoryPath) {
        Set<String> existingFiles = new HashSet<>();

        try {
            ProcessBuilder pb = new ProcessBuilder("git", "ls-files");
            pb.directory(repositoryPath.toFile());
            pb.redirectErrorStream(true);

            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.isBlank()) {
                        existingFiles.add(line.trim());
                    }
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("git ls-files command failed with exit code: {}", exitCode);
            }

        } catch (IOException | InterruptedException e) {
            log.error("Error retrieving existing files from repository at {}: {}", repositoryPath, e.getMessage(), e);
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            return Set.of();
        }

        return existingFiles;
    }

    private FileKnowledge calculateFileKnowledge(String analysisId, String filePath,
                                                 Map<String, AuthorContribution> authorContributions) {

        int linesAdded = authorContributions.values().stream()
                .mapToInt(AuthorContribution::getLinesAdded)
                .sum();

        int commits = authorContributions.values().stream()
                .mapToInt(AuthorContribution::getCommits)
                .sum();

        List<AuthorContribution> contributions = authorContributions.values().stream()
                .peek(contribution -> {
                    double contributionPercentage = linesAdded > 0
                            ? contribution.getLinesAdded() * 100.0 / linesAdded
                            : 0.0;
                    contribution.setContributionPercentage(contributionPercentage);
                })
                .sorted(Comparator.comparingDouble(AuthorContribution::getContributionPercentage).reversed())
                .toList();

        String leadAuthor = determineLeadAuthor(contributions);

        return FileKnowledge.builder()
                .analysisId(analysisId)
                .filePath(filePath)
                .linesAdded(linesAdded)
                .commits(commits)
                .authorContributions(contributions)
                .leadAuthor(leadAuthor)
                .contributors(contributions.size())
                .build();
    }

    private String determineLeadAuthor(List<AuthorContribution> contributions) {
        double maxPercentage = contributions.stream()
                .mapToDouble(AuthorContribution::getContributionPercentage)
                .max()
                .orElse(0.0);

        return maxPercentage < 1.0 ? null : contributions.stream()
                .filter(c -> c.getContributionPercentage() == maxPercentage)
                .max(Comparator.comparingInt(AuthorContribution::getCommits))
                .map(AuthorContribution::getName)
                .orElse(null);
    }

}
