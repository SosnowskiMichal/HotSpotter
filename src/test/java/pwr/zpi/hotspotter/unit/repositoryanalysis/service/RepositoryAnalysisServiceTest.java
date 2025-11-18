package pwr.zpi.hotspotter.unit.repositoryanalysis.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.activitytrends.ActivityTrendsAnalyzer;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.activitytrends.ActivityTrendsContext;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.authors.AuthorsAnalyzer;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.authors.AuthorsAnalyzerContext;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.coupling.CouplingAnalyzer;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.coupling.CouplingAnalyzerContext;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo.FileInfoAnalyzer;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo.FileInfoAnalyzerContext;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge.KnowledgeAnalyzer;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge.KnowledgeAnalyzerContext;
import pwr.zpi.hotspotter.repositoryanalysis.exception.AnalysisException;
import pwr.zpi.hotspotter.repositoryanalysis.exception.LogProcessingException;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.LogExtractor;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.LogParser;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.model.Commit;
import pwr.zpi.hotspotter.repositoryanalysis.model.AnalysisInfo;
import pwr.zpi.hotspotter.repositoryanalysis.repository.AnalysisInfoRepository;
import pwr.zpi.hotspotter.repositoryanalysis.service.RepositoryAnalysisService;
import pwr.zpi.hotspotter.repositoryanalysis.sse.AnalysisSseStatus;
import pwr.zpi.hotspotter.repositoryanalysis.sse.RepositoryAnalysisSsePublisher;
import pwr.zpi.hotspotter.repositorymanagement.model.RepositoryInfo;
import pwr.zpi.hotspotter.repositorymanagement.service.RepositoryManagementService;
import pwr.zpi.hotspotter.sonar.model.fileanalysis.SonarFileAnalysisResult;
import pwr.zpi.hotspotter.sonar.model.repoanalysis.SonarRepoAnalysisComponent;
import pwr.zpi.hotspotter.sonar.model.repoanalysis.SonarRepoAnalysisResult;
import pwr.zpi.hotspotter.sonar.service.SonarResultDownloader;
import pwr.zpi.hotspotter.sonar.service.SonarService;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RepositoryAnalysisServiceTest {

    @Mock private RepositoryManagementService repositoryManagementService;
    @Mock private AnalysisInfoRepository analysisInfoRepository;
    @Mock private LogExtractor logExtractor;
    @Mock private LogParser logParser;
    @Mock private RepositoryAnalysisSsePublisher ssePublisher;
    @Mock private SonarService sonarService;
    @Mock private KnowledgeAnalyzer knowledgeAnalyzer;
    @Mock private AuthorsAnalyzer authorsAnalyzer;
    @Mock private FileInfoAnalyzer fileInfoAnalyzer;
    @Mock private ActivityTrendsAnalyzer activityTrendsAnalyzer;
    @Mock private CouplingAnalyzer couplingAnalyzer;

    @InjectMocks
    private RepositoryAnalysisService repositoryAnalysisService;

    @Test
    void completesAnalysisSuccessfullyWhenAllStepsSucceed() {
        String repositoryUrl = "https://example.com/repo.git";
        LocalDate startDate = LocalDate.of(2023, 1, 1);
        LocalDate endDate = LocalDate.of(2023, 12, 31);
        SseEmitter emitter = mock(SseEmitter.class);

        RepositoryInfo repositoryInfo = mock(RepositoryInfo.class);
        Path repositoryPath = Path.of("/local/repo");
        AnalysisInfo analysisInfo = mock(AnalysisInfo.class);
        Path logFilePath = Path.of("/logs/logfile");

        Stream<Commit> commits = Stream.empty();

        CompletableFuture<SonarResultDownloader.SonarAnalysisResults> sonarResults =
                CompletableFuture.completedFuture(
                        new SonarResultDownloader.SonarAnalysisResults(
                                new SonarRepoAnalysisResult(),
                                List.of(new SonarRepoAnalysisComponent()),
                                new SonarFileAnalysisResult()
                        )
                );

        when(repositoryManagementService.cloneOrUpdateRepository(repositoryUrl)).thenReturn(repositoryInfo);
        when(repositoryInfo.getLocalPath()).thenReturn(repositoryPath.toString());
        when(repositoryInfo.getName()).thenReturn("repo-name");
        when(analysisInfoRepository.save(any(AnalysisInfo.class))).thenReturn(analysisInfo);
        when(logExtractor.extractLogs(eq(repositoryPath), anyString(), eq(startDate), eq(endDate)))
                .thenReturn(logFilePath);
        when(logParser.parseLogs(logFilePath)).thenReturn(commits);
        when(sonarService.runAnalysis(anyString(), eq(repositoryPath), anyString(), anyString()))
                .thenReturn(sonarResults);

        when(knowledgeAnalyzer.startAnalysis(anyString(), eq(repositoryPath)))
                .thenReturn(mock(KnowledgeAnalyzerContext.class));
        when(authorsAnalyzer.startAnalysis(anyString(), eq(endDate)))
                .thenReturn(mock(AuthorsAnalyzerContext.class));
        when(fileInfoAnalyzer.startAnalysis(anyString(), eq(repositoryPath), eq(endDate)))
                .thenReturn(mock(FileInfoAnalyzerContext.class));
        when(activityTrendsAnalyzer.startAnalysis(anyString(), eq(endDate), eq(6)))
                .thenReturn(mock(ActivityTrendsContext.class));
        when(couplingAnalyzer.startAnalysis(anyString(), eq(repositoryPath), eq(endDate)))
                .thenReturn(mock(CouplingAnalyzerContext.class));

        repositoryAnalysisService.runRepositoryAnalysis(repositoryUrl, startDate, endDate, emitter);

        verify(ssePublisher).sendProgress(emitter, AnalysisSseStatus.DOWNLOADING);
        verify(ssePublisher).sendProgress(emitter, AnalysisSseStatus.PROCESSING_DATA);
        verify(ssePublisher).sendProgress(emitter, AnalysisSseStatus.ANALYZING);
        verify(ssePublisher).sendProgress(emitter, AnalysisSseStatus.SONAR);
        verify(ssePublisher).sendComplete(eq(emitter), anyString());
        verify(logExtractor).deleteLogFile(logFilePath);
    }

    @Test
    void handlesLogProcessingExceptionAndMarksAnalysisAsFailed() {
        String repositoryUrl = "https://example.com/repo.git";
        LocalDate startDate = LocalDate.of(2023, 1, 1);
        LocalDate endDate = LocalDate.of(2023, 12, 31);
        SseEmitter emitter = mock(SseEmitter.class);

        RepositoryInfo repositoryInfo = mock(RepositoryInfo.class);
        Path repositoryPath = Path.of("/local/repo");
        AnalysisInfo analysisInfo = mock(AnalysisInfo.class);

        when(repositoryManagementService.cloneOrUpdateRepository(repositoryUrl)).thenReturn(repositoryInfo);
        when(repositoryInfo.getLocalPath()).thenReturn(repositoryPath.toString());
        when(analysisInfoRepository.save(any(AnalysisInfo.class))).thenReturn(analysisInfo);
        when(logExtractor.extractLogs(eq(repositoryPath), anyString(), eq(startDate), eq(endDate)))
                .thenThrow(new LogProcessingException("Log extraction failed"));

        assertThrows(LogProcessingException.class, () ->
                repositoryAnalysisService.runRepositoryAnalysis(repositoryUrl, startDate, endDate, emitter)
        );

        ArgumentCaptor<AnalysisInfo> captor = ArgumentCaptor.forClass(AnalysisInfo.class);
        verify(analysisInfoRepository, atLeastOnce()).save(captor.capture());
        AnalysisInfo failed = captor.getValue();
        assertEquals(AnalysisInfo.AnalysisStatus.FAILED, failed.getStatus());
        verify(ssePublisher).sendError(emitter, "Log extraction failed");
    }

    @Test
    void handlesUnexpectedExceptionAndMarksAnalysisAsFailed() {
        String repositoryUrl = "https://example.com/repo.git";
        LocalDate startDate = LocalDate.of(2023, 1, 1);
        LocalDate endDate = LocalDate.of(2023, 12, 31);
        SseEmitter emitter = mock(SseEmitter.class);

        RepositoryInfo repositoryInfo = mock(RepositoryInfo.class);
        Path repositoryPath = Path.of("/local/repo");
        AnalysisInfo analysisInfo = new AnalysisInfo();
        analysisInfo.setId("123");

        when(repositoryManagementService.cloneOrUpdateRepository(repositoryUrl))
                .thenReturn(repositoryInfo);
        when(repositoryInfo.getLocalPath()).thenReturn(repositoryPath.toString());
        when(analysisInfoRepository.save(any(AnalysisInfo.class))).thenReturn(analysisInfo);
        when(logExtractor.extractLogs(eq(repositoryPath), anyString(), eq(startDate), eq(endDate)))
                .thenThrow(new RuntimeException("Unexpected error"));

        assertThrows(AnalysisException.class, () ->
                repositoryAnalysisService.runRepositoryAnalysis(repositoryUrl, startDate, endDate, emitter)
        );

        ArgumentCaptor<AnalysisInfo> captor = ArgumentCaptor.forClass(AnalysisInfo.class);
        verify(analysisInfoRepository, atLeastOnce()).save(captor.capture());
        AnalysisInfo failed = captor.getValue();
        assertEquals(AnalysisInfo.AnalysisStatus.FAILED, failed.getStatus());
        verify(ssePublisher).sendError(emitter, "Unexpected error");
    }
}