package pwr.zpi.hotspotter.repositoryanalysis.analyzer.authors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.fileanalysis.analyzer.knowledge.model.FileKnowledge;
import pwr.zpi.hotspotter.fileanalysis.analyzer.knowledge.repository.FileKnowledgeRepository;
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

    public AuthorsAnalyzerContext startAnalysis(String analysisId) {
        log.debug("Starting authors analysis for ID {}", analysisId);
        return new AuthorsAnalyzerContext(analysisId);
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
            calculateInactivityTime(stats);
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
        }

        try {
            AnalysisUtils.saveDataInBatches(authorStatisticsRepository, authorStatistics);
        } catch (Exception e) {
            log.error("Error saving enriched authors analysis data for ID {}: {}", context.getAnalysisId(), e.getMessage(), e);
        }
    }

    private void calculateInactivityTime(AuthorStatistics authorStatistics) {
        LocalDate lastCommitDate = authorStatistics.getLastCommitDate();
        LocalDate currentDate = LocalDate.now();

        int daysSinceLastCommit = (int) ChronoUnit.DAYS.between(lastCommitDate, currentDate);
        int monthsSinceLastCommit = (int) ChronoUnit.MONTHS.between(lastCommitDate, currentDate);

        authorStatistics.setDaysSinceLastCommit(daysSinceLastCommit);
        authorStatistics.setMonthsSinceLastCommit(monthsSinceLastCommit);
    }

    private void checkIfInactive(AuthorStatistics stats) {
        boolean isActive = stats.getMonthsSinceLastCommit() != null
                && stats.getMonthsSinceLastCommit() < AUTHOR_INACTIVITY_THRESHOLD_MONTHS;
        stats.setIsActive(isActive);
    }

}
