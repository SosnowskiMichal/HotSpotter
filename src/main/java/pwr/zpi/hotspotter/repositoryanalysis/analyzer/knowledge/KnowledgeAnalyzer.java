package pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge.model.AuthorContribution;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge.model.FileKnowledge;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge.repository.FileKnowledgeRepository;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.authors.model.AuthorStatistics;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.authors.repository.AuthorStatisticsRepository;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.model.Commit;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.model.FileChange;
import pwr.zpi.hotspotter.repositoryanalysis.util.AnalysisUtils;

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
                context.updateFilePath(fileChange.oldPath(), fileChange.newPath());
                filePath = fileChange.newPath();
            }

            context.recordContribution(filePath, author, linesAdded);
        }
    }

    public void finishAnalysis(KnowledgeAnalyzerContext context) {
        if (context == null) return;

        Set<String> existingFiles = AnalysisUtils.getExistingFileNames(context.getRepositoryPath());

        List<FileKnowledge> knowledgeData = context.getFileContributions().entrySet().stream()
                .filter(entry -> existingFiles.contains(entry.getKey()))
                .map(entry -> calculateFileKnowledge(
                        context.getAnalysisId(),
                        entry.getKey(),
                        entry.getValue()
                ))
                .toList();

        try {
            AnalysisUtils.saveDataInBatches(fileKnowledgeRepository, knowledgeData);
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

            int activeContributors = (int) contributions.stream()
                    .filter(contribution -> authorActivityMap.getOrDefault(contribution.getName(), false))
                    .count();
            fileKnowledge.setActiveContributors(activeContributors);

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

        AuthorContribution leadAuthorContribution = determineLeadAuthorContribution(contributions);
        String leadAuthorName = leadAuthorContribution != null ? leadAuthorContribution.getName() : null;
        Double leadAuthorPercentage = leadAuthorContribution != null ? leadAuthorContribution.getContributionPercentage() : null;

        return FileKnowledge.builder()
                .analysisId(analysisId)
                .filePath(filePath)
                .linesAdded(linesAdded)
                .commits(commits)
                .authorContributions(contributions)
                .leadAuthor(leadAuthorName)
                .leadAuthorKnowledgePercentage(leadAuthorPercentage)
                .contributors(contributions.size())
                .build();
    }

    private AuthorContribution determineLeadAuthorContribution(List<AuthorContribution> contributions) {
        double maxPercentage = contributions.stream()
                .mapToDouble(AuthorContribution::getContributionPercentage)
                .max()
                .orElse(0.0);

        return maxPercentage < 1.0 ? null : contributions.stream()
                .filter(c -> c.getContributionPercentage() == maxPercentage)
                .max(Comparator.comparingInt(AuthorContribution::getCommits))
                .orElse(null);
    }

}
