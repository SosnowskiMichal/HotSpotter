package pwr.zpi.hotspotter.repositoryanalysis.analyzer.authors;

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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthorsAnalyzer {

    private static final int AUTHOR_INACTIVITY_THRESHOLD_MONTHS = 6;

    private final AuthorStatisticsRepository authorStatisticsRepository;
    private final FileKnowledgeRepository fileKnowledgeRepository;

    public AuthorsAnalyzerContext startAnalysis(String analysisId, LocalDate referenceDate) {
        log.debug("Starting authors analysis for ID {}", analysisId);
        return new AuthorsAnalyzerContext(analysisId, referenceDate);
    }

    public void processCommit(Commit commit, AuthorsAnalyzerContext context) {
        if (commit == null || context == null) return;

        String author = commit.author();
        String email = commit.email();
        LocalDate date = LocalDate.parse(commit.date(), DateTimeFormatter.ISO_LOCAL_DATE);

        int linesAdded = commit.changedFiles().stream()
                .mapToInt(FileChange::linesAdded)
                .sum();
        int linesDeleted = commit.changedFiles().stream()
                .mapToInt(FileChange::linesDeleted)
                .sum();

        context.recordContribution(author, email, date, linesAdded, linesDeleted);
    }

    public void finishAnalysis(AuthorsAnalyzerContext context) {
        if (context == null) return;

        Collection<AuthorStatistics> authorStatistics = context.getAuthorStatistics().values();
        authorStatistics.forEach(stats -> {
            calculateInactivityTime(stats, context.getReferenceDate());
            checkIfInactive(stats);
        });

        try {
            AnalysisUtils.saveDataInBatches(authorStatisticsRepository, authorStatistics);
        } catch (Exception e) {
            log.error("Error saving authors analysis data for ID {}: {}", context.getAnalysisId(), e.getMessage(), e);
        }
    }

    public void enrichAnalysisData(AuthorsAnalyzerContext context) {
        if (context == null) return;

        Collection<AuthorStatistics> authorStatistics = context.getAuthorStatistics().values();
        List<FileKnowledge> fileKnowledgeData = fileKnowledgeRepository.findAllByAnalysisId(context.getAnalysisId());

        for (FileKnowledge fileKnowledge : fileKnowledgeData) {
            String leadAuthor = fileKnowledge.getLeadAuthor();
            if (leadAuthor != null && !leadAuthor.isBlank()) {
                AuthorStatistics stats = context.getAuthorStatistics().get(leadAuthor);
                if (stats != null) {
                    stats.incrementFilesAsLeadAuthor();
                }
            }

            for (AuthorContribution contribution : fileKnowledge.getAuthorContributions()) {
                String name = contribution.getName();
                AuthorStatistics stats = context.getAuthorStatistics().get(name);
                if (stats != null) {
                    stats.incrementExistingFilesModified();
                }
            }
        }

        try {
            AnalysisUtils.saveDataInBatches(authorStatisticsRepository, authorStatistics);
        } catch (Exception e) {
            log.error("Error saving enriched authors analysis data for ID {}: {}", context.getAnalysisId(), e.getMessage(), e);
        }
    }

    private void calculateInactivityTime(AuthorStatistics authorStatistics, LocalDate referenceDate) {
        LocalDate lastCommitDate = authorStatistics.getLastCommitDate();

        int daysSinceLastCommit = (int) ChronoUnit.DAYS.between(lastCommitDate, referenceDate);
        int monthsSinceLastCommit = (int) ChronoUnit.MONTHS.between(lastCommitDate, referenceDate);

        authorStatistics.setDaysSinceLastCommit(daysSinceLastCommit);
        authorStatistics.setMonthsSinceLastCommit(monthsSinceLastCommit);
    }

    private void checkIfInactive(AuthorStatistics stats) {
        boolean isActive = stats.getMonthsSinceLastCommit() != null
                && stats.getMonthsSinceLastCommit() < AUTHOR_INACTIVITY_THRESHOLD_MONTHS;
        stats.setIsActive(isActive);
    }

}
