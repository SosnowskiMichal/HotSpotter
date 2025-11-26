package pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge.model.AuthorContribution;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge.model.FileKnowledge;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge.model.KnowledgeRisk;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge.repository.FileKnowledgeRepository;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.authors.model.AuthorStatistics;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.authors.repository.AuthorStatisticsRepository;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.model.Commit;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.model.FileChange;
import pwr.zpi.hotspotter.repositoryanalysis.filter.AnalysisFileFilter;
import pwr.zpi.hotspotter.repositoryanalysis.util.AnalysisUtils;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeAnalyzer {

    private static final double ABANDONED_THRESHOLD = 75.0;
    private static final double SINGLE_OWNER_THRESHOLD = 75.0;
    private static final int DIFFUSED_AUTHOR_THRESHOLD = 6;

    private final FileKnowledgeRepository fileKnowledgeRepository;
    private final AuthorStatisticsRepository authorStatisticsRepository;
    private final AnalysisFileFilter analysisFileFilter;

    public KnowledgeAnalyzerContext startAnalysis(String analysisId, Path repositoryPath) {
        log.debug("Starting knowledge analysis for ID: {}", analysisId);
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

        log.debug("Finishing knowledge analysis for ID: {}", context.getAnalysisId());

        Set<String> existingFiles = AnalysisUtils.getFilteredExistingFileNames(context.getRepositoryPath(),
                analysisFileFilter);

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
            List<AuthorContribution> contributions = fileKnowledge.getAuthorContributions();
            if (contributions == null || contributions.isEmpty()) {
                fileKnowledge.setKnowledgeLoss(0.0);
                fileKnowledge.setKnowledgeRisk(KnowledgeRisk.UNKNOWN);
                continue;
            }

            int activeContributors = (int) contributions.stream()
                    .filter(contribution -> authorActivityMap.getOrDefault(contribution.getName(), false))
                    .count();
            fileKnowledge.setActiveAuthors(activeContributors);

            int totalLinesAdded = fileKnowledge.getLinesAdded();
            if (totalLinesAdded > 0) {
                int linesAddedByInactiveAuthors = contributions.stream()
                        .filter(contribution -> !authorActivityMap.get(contribution.getName()))
                        .mapToInt(AuthorContribution::getLinesAdded)
                        .sum();

                double knowledgeLoss = Math.round(linesAddedByInactiveAuthors * 10000.0 / totalLinesAdded) / 100.0;
                fileKnowledge.setKnowledgeLoss(knowledgeLoss);

            } else {
                int totalCommits = fileKnowledge.getCommits();
                int commitsByInactiveAuthors = contributions.stream()
                        .filter(contribution -> !authorActivityMap.get(contribution.getName()))
                        .mapToInt(AuthorContribution::getCommits)
                        .sum();

                double knowledgeLoss = Math.round(commitsByInactiveAuthors * 10000.0 / totalCommits) / 100.0;
                fileKnowledge.setKnowledgeLoss(knowledgeLoss);

                fileKnowledge.setLinesAdded(null);
                contributions.forEach(contribution -> {
                    Integer lines = contribution.getLinesAdded();
                    if (lines != null && lines == 0) {
                        contribution.setLinesAdded(null);
                    }
                });
            }

            KnowledgeRisk risk = calculateKnowledgeRisk(fileKnowledge);
            fileKnowledge.setKnowledgeRisk(risk);
        }

        try {
            AnalysisUtils.saveDataInBatches(fileKnowledgeRepository, fileKnowledgeData);
            log.debug("Saved {} knowledge analysis data records for ID: {}", fileKnowledgeData.size(), context.getAnalysisId());
        } catch (Exception e) {
            log.error("Error saving enriched knowledge analysis data for ID: {}: {}", context.getAnalysisId(), e.getMessage(), e);
        }
    }

    private FileKnowledge calculateFileKnowledge(
            String analysisId,
            String filePath,
            Map<String, AuthorContribution> authorContributions
    ) {
        int linesAdded = authorContributions.values().stream()
                .mapToInt(AuthorContribution::getLinesAdded)
                .sum();

        int commits = authorContributions.values().stream()
                .mapToInt(AuthorContribution::getCommits)
                .sum();

        List<AuthorContribution> contributions = authorContributions.values().stream()
                .peek(contribution -> {
                    double contributionPercentage = linesAdded > 0
                            ? Math.round(contribution.getLinesAdded() * 10000.0 / linesAdded) / 100.0
                            : Math.round(contribution.getCommits() * 10000.0 / commits) / 100.0;
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
                .fileName(getFileName(filePath))
                .linesAdded(linesAdded)
                .commits(commits)
                .authorContributions(contributions)
                .leadAuthor(leadAuthorName)
                .leadAuthorKnowledgePercentage(leadAuthorPercentage)
                .authors(contributions.size())
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

    private String getFileName(String filePath) {
        String[] parts = filePath.replace("\\", "/").split("/");
        return parts[parts.length - 1];
    }

    private KnowledgeRisk calculateKnowledgeRisk(FileKnowledge fileKnowledge) {
        Integer authors = fileKnowledge.getAuthors();
        Integer activeAuthors = fileKnowledge.getActiveAuthors();
        Double leadAuthorPercentage = fileKnowledge.getLeadAuthorKnowledgePercentage();
        Double knowledgeLoss = fileKnowledge.getKnowledgeLoss();

        if (activeAuthors != null && activeAuthors == 0) {
            return KnowledgeRisk.ABANDONED;
        }
        if (knowledgeLoss != null && knowledgeLoss >= ABANDONED_THRESHOLD) {
            return KnowledgeRisk.ABANDONED;
        }

        if (leadAuthorPercentage != null && leadAuthorPercentage >= SINGLE_OWNER_THRESHOLD) {
            return KnowledgeRisk.SINGLE_OWNER;
        }

        if (authors != null && authors >= DIFFUSED_AUTHOR_THRESHOLD) {
            return KnowledgeRisk.DIFFUSED;
        }
        if (activeAuthors != null && activeAuthors >= DIFFUSED_AUTHOR_THRESHOLD) {
            return KnowledgeRisk.DIFFUSED;
        }

        if (authors == null || authors == 0) {
            return KnowledgeRisk.UNKNOWN;
        }
        if (activeAuthors == null) {
            return KnowledgeRisk.UNKNOWN;
        }
        if (leadAuthorPercentage == null || leadAuthorPercentage < 1.0) {
            return KnowledgeRisk.UNKNOWN;
        }

        return KnowledgeRisk.BALANCED;
    }

}
