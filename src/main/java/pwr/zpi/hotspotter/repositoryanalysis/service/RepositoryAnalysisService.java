package pwr.zpi.hotspotter.repositoryanalysis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo.FileInfoAnalyzer;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo.FileInfoAnalyzerContext;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge.KnowledgeAnalyzer;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge.KnowledgeAnalyzerContext;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.authors.AuthorsAnalyzer;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.authors.AuthorsAnalyzerContext;
import pwr.zpi.hotspotter.repositoryanalysis.exception.AnalysisException;
import pwr.zpi.hotspotter.repositoryanalysis.exception.LogProcessingException;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.LogExtractor;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.LogParser;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.model.Commit;
import pwr.zpi.hotspotter.repositoryanalysis.model.AnalysisInfo;
import pwr.zpi.hotspotter.repositoryanalysis.repository.AnalysisInfoRepository;
import pwr.zpi.hotspotter.repositorymanagement.model.RepositoryInfo;
import pwr.zpi.hotspotter.repositorymanagement.service.RepositoryManagementService;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class RepositoryAnalysisService {

    private final RepositoryManagementService repositoryManagementService;
    private final AnalysisInfoRepository analysisInfoRepository;
    private final LogExtractor logExtractor;
    private final LogParser logParser;

    // Inject all analyzers here
    private final KnowledgeAnalyzer knowledgeAnalyzer;
    private final AuthorsAnalyzer authorsAnalyzer;
    private final FileInfoAnalyzer fileInfoAnalyzer;

    public String runRepositoryAnalysis(String repositoryUrl, LocalDate startDate, LocalDate endDate) {
        long analysisStartTime = System.currentTimeMillis();

        RepositoryInfo repositoryInfo = repositoryManagementService.cloneOrUpdateRepository(repositoryUrl);
        Path repositoryPath = Path.of(repositoryInfo.getLocalPath());

        AnalysisInfo analysisInfo = createAnalysisInfo(repositoryInfo, startDate, endDate);
        String analysisId = analysisInfo.getId();
        analysisInfoRepository.save(analysisInfo);

        Path logFilePath = null;
        try {
            logFilePath = logExtractor.extractLogs(repositoryPath, analysisId, startDate, endDate);
            Stream<Commit> commits = logParser.parseLogs(logFilePath);

            KnowledgeAnalyzerContext knowledgeContext = knowledgeAnalyzer.startAnalysis(analysisId, repositoryPath);
            AuthorsAnalyzerContext authorsContext = authorsAnalyzer.startAnalysis(analysisId, endDate);
            FileInfoAnalyzerContext fileInfoContext = fileInfoAnalyzer.startAnalysis(analysisId, repositoryPath, endDate);

            try (commits) {
                commits.forEach(commit -> {
                    knowledgeAnalyzer.processCommit(commit, knowledgeContext);
                    authorsAnalyzer.processCommit(commit, authorsContext);
                    fileInfoAnalyzer.processCommit(commit, fileInfoContext);
                });
            }

            knowledgeAnalyzer.finishAnalysis(knowledgeContext);
            authorsAnalyzer.finishAnalysis(authorsContext);
            fileInfoAnalyzer.finishAnalysis(fileInfoContext);

            knowledgeAnalyzer.enrichAnalysisData(knowledgeContext);
            authorsAnalyzer.enrichAnalysisData(authorsContext);

            long analysisEndTime = System.currentTimeMillis();
            long analysisDurationSeconds = (analysisEndTime - analysisStartTime) / 1000;

            analysisInfo.setAnalysisTimeInSeconds(analysisDurationSeconds);
            analysisInfo.markAsCompleted();
            analysisInfoRepository.save(analysisInfo);

            log.info("Analysis completed for repository {} in {} seconds, ID: {}",
                    repositoryUrl, analysisDurationSeconds, analysisId);

            return analysisId;

        } catch (LogProcessingException e) {
            analysisInfo.markAsFailed();
            analysisInfoRepository.save(analysisInfo);
            throw e;

        } catch (Exception e) {
            analysisInfo.markAsFailed();
            analysisInfoRepository.save(analysisInfo);
            log.error("Unexpected error during analysis for repository {}: {}", repositoryUrl, e.getMessage(), e);
            throw new AnalysisException("Analysis failed: " + e.getMessage());

        } finally {
            if (logFilePath != null) {
                logExtractor.deleteLogFile(logFilePath);
            }
        }
    }

    private AnalysisInfo createAnalysisInfo(RepositoryInfo repositoryInfo, LocalDate startDate, LocalDate endDate) {
        String analysisId = UUID.randomUUID().toString();
        return AnalysisInfo.builder()
                .id(analysisId)
                .repositoryUrl(repositoryInfo.getRemoteUrl())
                .repositoryName(repositoryInfo.getName())
                .repositoryOwner(repositoryInfo.getOwner())
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }

}
