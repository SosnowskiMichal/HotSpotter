package pwr.zpi.hotspotter.fileanalysis.analyzer.ownership;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.fileanalysis.analyzer.ownership.model.AuthorContribution;
import pwr.zpi.hotspotter.fileanalysis.analyzer.ownership.model.FileOwnership;
import pwr.zpi.hotspotter.fileanalysis.analyzer.ownership.repository.FileOwnershipRepository;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.model.Commit;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.model.FileChange;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class OwnershipAnalyzer {

    private static final int BATCH_SIZE = 500;

    private final FileOwnershipRepository fileOwnershipRepository;

    public OwnershipAnalyzerContext startAnalysis(String analysisId, Path repositoryPath) {
        log.debug("Starting ownership analysis for ID {}", analysisId);
        return new OwnershipAnalyzerContext(analysisId, repositoryPath);
    }

    public void processCommit(Commit commit, OwnershipAnalyzerContext context) {
        if (commit == null || context == null) return;

        String author = commit.author();
        String email = commit.email();

        for (FileChange fileChange : commit.changedFiles()) {
            String filePath = fileChange.filePath();
            int linesAdded = fileChange.linesAdded();

            if (fileChange.isRenamed()) {
                updateFilePath(fileChange.oldPath(), fileChange.newPath(), context);
                filePath = fileChange.newPath();
            }

            context.recordContribution(filePath, author, email, linesAdded);
        }
    }

    public void finishAnalysis(OwnershipAnalyzerContext context) {
        if (context == null || context.getRepositoryPath() == null) return;

        try {
            Set<String> existingFiles = getExistingFiles(context.getRepositoryPath());
            List<FileOwnership> ownershipData = new ArrayList<>();

            for (Map.Entry<String, Map<String, AuthorContribution>> fileEntry
                    : context.getFileContributions().entrySet()) {

                String filePath = fileEntry.getKey();

                if (!existingFiles.contains(filePath)) continue;

                Map<String, AuthorContribution> authorContributions = fileEntry.getValue();

                FileOwnership fileOwnership = calculateFileOwnership(
                        context.getAnalysisId(),
                        filePath,
                        authorContributions
                );

                ownershipData.add(fileOwnership);

                if (ownershipData.size() >= BATCH_SIZE) {
                    fileOwnershipRepository.saveAll(ownershipData);
                    log.debug("Saved batch of {} file ownership records.", ownershipData.size());
                    ownershipData.clear();
                }
            }

            if (!ownershipData.isEmpty()) {
                fileOwnershipRepository.saveAll(ownershipData);
                log.debug("Saved final batch of {} file ownership records.", ownershipData.size());
            }

        } catch (Exception e) {
            log.error("Error finalizing ownership analysis for ID: {} : {}", context.getAnalysisId(), e.getMessage(), e);
        }
    }

    private void updateFilePath(String oldPath, String newPath, OwnershipAnalyzerContext context) {
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

    private FileOwnership calculateFileOwnership(String analysisId, String filePath,
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
                            ? (double) contribution.getLinesAdded() / linesAdded * 100.0
                            : 0.0;
                    contribution.setContributionPercentage(contributionPercentage);
                })
                .sorted(Comparator.comparingDouble(AuthorContribution::getContributionPercentage).reversed())
                .toList();

        List<String> leadAuthors = determineLeadAuthors(contributions);

        return FileOwnership.builder()
                .analysisId(analysisId)
                .filePath(filePath)
                .linesAdded(linesAdded)
                .commits(commits)
                .authorContributions(contributions)
                .leadAuthors(leadAuthors)
                .contributors(contributions.size())
                .build();
    }

    private List<String> determineLeadAuthors(List<AuthorContribution> contributions) {
        if (contributions.isEmpty()) return List.of();

        double maxContributionPercentage = contributions.getFirst().getContributionPercentage();

        if (maxContributionPercentage < 1.0) return List.of();

        return contributions.stream()
                .filter(c -> Math.abs(c.getContributionPercentage() - maxContributionPercentage) < 0.01)
                .map(AuthorContribution::getName)
                .toList();
    }

}
