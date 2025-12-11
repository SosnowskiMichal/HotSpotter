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
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.statistics.AnalysisStatisticsCalculator;
import pwr.zpi.hotspotter.common.exception.LogProcessingException;
import pwr.zpi.hotspotter.repositoryanalysis.filter.AnalysisFileFilter;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.CommitStream;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.LogExtractor;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.model.Commit;
import pwr.zpi.hotspotter.repositoryanalysis.model.AnalysisInfo;
import pwr.zpi.hotspotter.repositoryanalysis.repository.AnalysisInfoRepository;
import pwr.zpi.hotspotter.repositoryanalysis.service.RepositoryAnalysisService;
import pwr.zpi.hotspotter.common.sse.AnalysisSseStatus;
import pwr.zpi.hotspotter.common.sse.AnalysisSsePublisher;
import pwr.zpi.hotspotter.common.exception.InvalidRepositoryUrlException;
import pwr.zpi.hotspotter.common.exception.RepositoryCloneException;
import pwr.zpi.hotspotter.common.exception.RepositoryUpdateException;
import pwr.zpi.hotspotter.repositorymanagement.model.RepositoryInfo;
import pwr.zpi.hotspotter.sonar.model.fileanalysis.SonarIssue;
import pwr.zpi.hotspotter.sonar.model.repoanalysis.SonarRepoAnalysisComponent;
import pwr.zpi.hotspotter.sonar.model.repoanalysis.SonarRepoAnalysisResult;
import pwr.zpi.hotspotter.sonar.service.SonarResultDownloader;
import pwr.zpi.hotspotter.sonar.service.SonarService;
import pwr.zpi.hotspotter.user.model.User;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RepositoryAnalysisServiceTest {

    @Mock private AnalysisInfoRepository analysisInfoRepository;
    @Mock private LogExtractor logExtractor;
    @Mock private AnalysisSsePublisher ssePublisher;
    @Mock private SonarService sonarService;
    @Mock private KnowledgeAnalyzer knowledgeAnalyzer;
    @Mock private AuthorsAnalyzer authorsAnalyzer;
    @Mock private FileInfoAnalyzer fileInfoAnalyzer;
    @Mock private ActivityTrendsAnalyzer activityTrendsAnalyzer;
    @Mock private CouplingAnalyzer couplingAnalyzer;
    @Mock private AnalysisStatisticsCalculator analysisStatisticsCalculator;
    @Mock private AnalysisFileFilter analysisFileFilter;

    @InjectMocks
    private RepositoryAnalysisService repositoryAnalysisService;

    private CommitStream mockCommitStream(Stream<Commit> commits) {
        CommitStream commitStream = mock(CommitStream.class);
        when(commitStream.getStream()).thenReturn(commits);
        return commitStream;
    }

    @Test
    void completesAnalysisSuccessfullyWhenAllStepsSucceed() {
        RepositoryInfo repositoryInfo = mock(RepositoryInfo.class);
        when(repositoryInfo.getRemoteUrl()).thenReturn("https://example.com/repo.git");
        when(repositoryInfo.getLocalPath()).thenReturn("/local/repo");
        when(repositoryInfo.getName()).thenReturn("repo-name");
        when(repositoryInfo.getOwner()).thenReturn("repo-owner");
        when(repositoryInfo.getPlatform()).thenReturn("GitHub");

        User user = mock(User.class);
        LocalDate startDate = LocalDate.of(2023, 1, 1);
        SseEmitter emitter = mock(SseEmitter.class);

        Path repositoryPath = Path.of("/local/repo");
        AnalysisInfo analysisInfo = mock(AnalysisInfo.class);

        Stream<Commit> commits = Stream.empty();
        CommitStream commitStream = mockCommitStream(commits);

        CompletableFuture<SonarResultDownloader.SonarAnalysisResults> sonarResults =
                CompletableFuture.completedFuture(
                        new SonarResultDownloader.SonarAnalysisResults(
                                new SonarRepoAnalysisResult(),
                                List.of(new SonarRepoAnalysisComponent()),
                                List.of(new SonarIssue())
                        )
                );

        when(analysisInfoRepository.save(any(AnalysisInfo.class))).thenReturn(analysisInfo);
        when(logExtractor.extractAndParseCommits(eq(repositoryPath), eq(startDate), eq(null)))
                .thenReturn(commitStream);
        when(sonarService.runAnalysis(anyString(), eq(repositoryPath), anyString(), anyString()))
                .thenReturn(sonarResults);

        when(knowledgeAnalyzer.startAnalysis(anyString(), eq(repositoryPath)))
                .thenReturn(mock(KnowledgeAnalyzerContext.class));
        when(authorsAnalyzer.startAnalysis(anyString(), eq(null)))
                .thenReturn(mock(AuthorsAnalyzerContext.class));
        when(fileInfoAnalyzer.startAnalysis(anyString(), eq(repositoryPath), eq(null)))
                .thenReturn(mock(FileInfoAnalyzerContext.class));
        when(activityTrendsAnalyzer.startAnalysis(anyString(), eq(null)))
                .thenReturn(mock(ActivityTrendsContext.class));
        when(couplingAnalyzer.startAnalysis(anyString(), eq(repositoryPath), eq(null)))
                .thenReturn(mock(CouplingAnalyzerContext.class));

        repositoryAnalysisService.runRepositoryAnalysis(repositoryInfo, startDate, null, emitter, user);

        verify(ssePublisher).sendProgress(emitter, AnalysisSseStatus.PROCESSING_DATA);
        verify(ssePublisher).sendProgress(emitter, AnalysisSseStatus.ANALYZING);
        verify(ssePublisher).sendProgress(emitter, AnalysisSseStatus.FINALIZING);
        verify(ssePublisher).sendSuccess(eq(emitter), anyString());
        verify(commitStream).close();
    }

    @Test
    void extractsRepositoryMetadataFromRepositoryInfo() {
        RepositoryInfo repositoryInfo = mock(RepositoryInfo.class);
        when(repositoryInfo.getRemoteUrl()).thenReturn("https://github.com/test/my-repo.git");
        when(repositoryInfo.getLocalPath()).thenReturn("/local/repo");
        when(repositoryInfo.getName()).thenReturn("my-repo");
        when(repositoryInfo.getOwner()).thenReturn("test-owner");
        when(repositoryInfo.getPlatform()).thenReturn("GitHub");

        LocalDate startDate = LocalDate.of(2023, 1, 1);
        SseEmitter emitter = mock(SseEmitter.class);

        Path repositoryPath = Path.of("/local/repo");
        Stream<Commit> commits = Stream.empty();
        CommitStream commitStream = mockCommitStream(commits);

        CompletableFuture<SonarResultDownloader.SonarAnalysisResults> sonarResults =
                CompletableFuture.completedFuture(
                        new SonarResultDownloader.SonarAnalysisResults(
                                new SonarRepoAnalysisResult(),
                                List.of(new SonarRepoAnalysisComponent()),
                                List.of(new SonarIssue())
                        )
                );

        when(logExtractor.extractAndParseCommits(eq(repositoryPath), eq(startDate), eq(null)))
                .thenReturn(commitStream);
        when(sonarService.runAnalysis(anyString(), eq(repositoryPath), anyString(), anyString()))
                .thenReturn(sonarResults);
        when(knowledgeAnalyzer.startAnalysis(anyString(), eq(repositoryPath)))
                .thenReturn(mock(KnowledgeAnalyzerContext.class));
        when(authorsAnalyzer.startAnalysis(anyString(), eq(null)))
                .thenReturn(mock(AuthorsAnalyzerContext.class));
        when(fileInfoAnalyzer.startAnalysis(anyString(), eq(repositoryPath), eq(null)))
                .thenReturn(mock(FileInfoAnalyzerContext.class));
        when(activityTrendsAnalyzer.startAnalysis(anyString(), eq(null)))
                .thenReturn(mock(ActivityTrendsContext.class));
        when(couplingAnalyzer.startAnalysis(anyString(), eq(repositoryPath), eq(null)))
                .thenReturn(mock(CouplingAnalyzerContext.class));

        repositoryAnalysisService.runRepositoryAnalysis(repositoryInfo, startDate, null, emitter, null);

        ArgumentCaptor<AnalysisInfo> captor = ArgumentCaptor.forClass(AnalysisInfo.class);
        verify(analysisInfoRepository, atLeastOnce()).save(captor.capture());
        AnalysisInfo savedAnalysisInfo = captor.getAllValues().getFirst();

        assertEquals("https://github.com/test/my-repo.git", savedAnalysisInfo.getRepositoryUrl());
        assertEquals("my-repo", savedAnalysisInfo.getRepositoryName());
        assertEquals("test-owner", savedAnalysisInfo.getRepositoryOwner());
        assertEquals("GitHub", savedAnalysisInfo.getRepositoryPlatform());
    }

    @Test
    void usesLocalPathFromRepositoryInfo() {
        RepositoryInfo repositoryInfo = mock(RepositoryInfo.class);
        when(repositoryInfo.getRemoteUrl()).thenReturn("https://example.com/repo.git");
        when(repositoryInfo.getLocalPath()).thenReturn("/custom/path/to/repo");
        when(repositoryInfo.getName()).thenReturn("repo-name");
        when(repositoryInfo.getOwner()).thenReturn("repo-owner");
        when(repositoryInfo.getPlatform()).thenReturn("GitHub");

        LocalDate startDate = LocalDate.of(2023, 1, 1);
        SseEmitter emitter = mock(SseEmitter.class);

        Path customPath = Path.of("/custom/path/to/repo");
        Stream<Commit> commits = Stream.empty();
        CommitStream commitStream = mockCommitStream(commits);

        CompletableFuture<SonarResultDownloader.SonarAnalysisResults> sonarResults =
                CompletableFuture.completedFuture(
                        new SonarResultDownloader.SonarAnalysisResults(
                                new SonarRepoAnalysisResult(),
                                List.of(new SonarRepoAnalysisComponent()),
                                List.of(new SonarIssue())
                        )
                );

        when(logExtractor.extractAndParseCommits(eq(customPath), eq(startDate), eq(null)))
                .thenReturn(commitStream);
        when(sonarService.runAnalysis(anyString(), eq(customPath), anyString(), anyString()))
                .thenReturn(sonarResults);
        when(knowledgeAnalyzer.startAnalysis(anyString(), eq(customPath)))
                .thenReturn(mock(KnowledgeAnalyzerContext.class));
        when(authorsAnalyzer.startAnalysis(anyString(), eq(null)))
                .thenReturn(mock(AuthorsAnalyzerContext.class));
        when(fileInfoAnalyzer.startAnalysis(anyString(), eq(customPath), eq(null)))
                .thenReturn(mock(FileInfoAnalyzerContext.class));
        when(activityTrendsAnalyzer.startAnalysis(anyString(), eq(null)))
                .thenReturn(mock(ActivityTrendsContext.class));
        when(couplingAnalyzer.startAnalysis(anyString(), eq(customPath), eq(null)))
                .thenReturn(mock(CouplingAnalyzerContext.class));

        repositoryAnalysisService.runRepositoryAnalysis(repositoryInfo, startDate, null, emitter, null);

        verify(logExtractor).extractAndParseCommits(eq(customPath), eq(startDate), eq(null));
        verify(knowledgeAnalyzer).startAnalysis(anyString(), eq(customPath));
        verify(fileInfoAnalyzer).startAnalysis(anyString(), eq(customPath), eq(null));
        verify(couplingAnalyzer).startAnalysis(anyString(), eq(customPath), eq(null));
        verify(sonarService).runAnalysis(anyString(), eq(customPath), anyString(), anyString());
    }

    @Test
    void startsAllAnalyzersWithCorrectParameters() {
        RepositoryInfo repositoryInfo = mock(RepositoryInfo.class);
        when(repositoryInfo.getRemoteUrl()).thenReturn("https://example.com/repo.git");
        when(repositoryInfo.getLocalPath()).thenReturn("/local/repo");
        when(repositoryInfo.getName()).thenReturn("repo-name");
        when(repositoryInfo.getOwner()).thenReturn("repo-owner");
        when(repositoryInfo.getPlatform()).thenReturn("GitHub");

        LocalDate startDate = LocalDate.of(2023, 1, 1);
        LocalDate endDate = LocalDate.of(2023, 12, 31);
        SseEmitter emitter = mock(SseEmitter.class);

        Path repositoryPath = Path.of("/local/repo");
        Stream<Commit> commits = Stream.empty();
        CommitStream commitStream = mockCommitStream(commits);

        CompletableFuture<SonarResultDownloader.SonarAnalysisResults> sonarResults =
                CompletableFuture.completedFuture(
                        new SonarResultDownloader.SonarAnalysisResults(
                                new SonarRepoAnalysisResult(),
                                List.of(new SonarRepoAnalysisComponent()),
                                List.of(new SonarIssue())
                        )
                );

        when(logExtractor.extractAndParseCommits(eq(repositoryPath), eq(startDate), eq(endDate)))
                .thenReturn(commitStream);
        when(sonarService.runAnalysis(anyString(), eq(repositoryPath), anyString(), anyString()))
                .thenReturn(sonarResults);
        when(knowledgeAnalyzer.startAnalysis(anyString(), eq(repositoryPath)))
                .thenReturn(mock(KnowledgeAnalyzerContext.class));
        when(authorsAnalyzer.startAnalysis(anyString(), eq(endDate)))
                .thenReturn(mock(AuthorsAnalyzerContext.class));
        when(fileInfoAnalyzer.startAnalysis(anyString(), eq(repositoryPath), eq(endDate)))
                .thenReturn(mock(FileInfoAnalyzerContext.class));
        when(activityTrendsAnalyzer.startAnalysis(anyString(), eq(endDate)))
                .thenReturn(mock(ActivityTrendsContext.class));
        when(couplingAnalyzer.startAnalysis(anyString(), eq(repositoryPath), eq(endDate)))
                .thenReturn(mock(CouplingAnalyzerContext.class));

        repositoryAnalysisService.runRepositoryAnalysis(repositoryInfo, startDate, endDate, emitter, null);

        verify(knowledgeAnalyzer).startAnalysis(anyString(), eq(repositoryPath));
        verify(authorsAnalyzer).startAnalysis(anyString(), eq(endDate));
        verify(fileInfoAnalyzer).startAnalysis(anyString(), eq(repositoryPath), eq(endDate));
        verify(activityTrendsAnalyzer).startAnalysis(anyString(), eq(endDate));
        verify(couplingAnalyzer).startAnalysis(anyString(), eq(repositoryPath), eq(endDate));
    }

    @Test
    void processesCommitsThroughAllAnalyzers() {
        RepositoryInfo repositoryInfo = mock(RepositoryInfo.class);
        when(repositoryInfo.getRemoteUrl()).thenReturn("https://example.com/repo.git");
        when(repositoryInfo.getLocalPath()).thenReturn("/local/repo");
        when(repositoryInfo.getName()).thenReturn("repo-name");
        when(repositoryInfo.getOwner()).thenReturn("repo-owner");
        when(repositoryInfo.getPlatform()).thenReturn("GitHub");

        LocalDate startDate = LocalDate.of(2023, 1, 1);
        SseEmitter emitter = mock(SseEmitter.class);

        Path repositoryPath = Path.of("/local/repo");

        Commit commit1 = mock(Commit.class);
        Commit commit2 = mock(Commit.class);
        Commit commit3 = mock(Commit.class);
        Stream<Commit> commits = Stream.of(commit1, commit2, commit3);
        CommitStream commitStream = mockCommitStream(commits);

        CompletableFuture<SonarResultDownloader.SonarAnalysisResults> sonarResults =
                CompletableFuture.completedFuture(
                        new SonarResultDownloader.SonarAnalysisResults(
                                new SonarRepoAnalysisResult(),
                                List.of(new SonarRepoAnalysisComponent()),
                                List.of(new SonarIssue())
                        )
                );

        KnowledgeAnalyzerContext knowledgeContext = mock(KnowledgeAnalyzerContext.class);
        AuthorsAnalyzerContext authorsContext = mock(AuthorsAnalyzerContext.class);
        FileInfoAnalyzerContext fileInfoContext = mock(FileInfoAnalyzerContext.class);
        ActivityTrendsContext activityTrendsContext = mock(ActivityTrendsContext.class);
        CouplingAnalyzerContext couplingContext = mock(CouplingAnalyzerContext.class);

        when(logExtractor.extractAndParseCommits(eq(repositoryPath), eq(startDate), eq(null)))
                .thenReturn(commitStream);
        when(sonarService.runAnalysis(anyString(), eq(repositoryPath), anyString(), anyString()))
                .thenReturn(sonarResults);

        when(knowledgeAnalyzer.startAnalysis(anyString(), eq(repositoryPath))).thenReturn(knowledgeContext);
        when(authorsAnalyzer.startAnalysis(anyString(), eq(null))).thenReturn(authorsContext);
        when(fileInfoAnalyzer.startAnalysis(anyString(), eq(repositoryPath), eq(null))).thenReturn(fileInfoContext);
        when(activityTrendsAnalyzer.startAnalysis(anyString(), eq(null))).thenReturn(activityTrendsContext);
        when(couplingAnalyzer.startAnalysis(anyString(), eq(repositoryPath), eq(null))).thenReturn(couplingContext);

        when(analysisFileFilter.filterCommit(commit1)).thenReturn(commit1);
        when(analysisFileFilter.filterCommit(commit2)).thenReturn(commit2);
        when(analysisFileFilter.filterCommit(commit3)).thenReturn(commit3);

        repositoryAnalysisService.runRepositoryAnalysis(repositoryInfo, startDate, null, emitter, null);

        verify(knowledgeAnalyzer, times(3)).processCommit(any(Commit.class), eq(knowledgeContext));
        verify(authorsAnalyzer, times(3)).processCommit(any(Commit.class), eq(authorsContext));
        verify(fileInfoAnalyzer, times(3)).processCommit(any(Commit.class), eq(fileInfoContext));
        verify(activityTrendsAnalyzer, times(3)).processCommit(any(Commit.class), eq(activityTrendsContext));
        verify(couplingAnalyzer, times(3)).processCommit(any(Commit.class), eq(couplingContext));
    }

    @Test
    void enrichesDataForCorrectAnalyzers() {
        RepositoryInfo repositoryInfo = mock(RepositoryInfo.class);
        when(repositoryInfo.getRemoteUrl()).thenReturn("https://example.com/repo.git");
        when(repositoryInfo.getLocalPath()).thenReturn("/local/repo");
        when(repositoryInfo.getName()).thenReturn("repo-name");
        when(repositoryInfo.getOwner()).thenReturn("repo-owner");
        when(repositoryInfo.getPlatform()).thenReturn("GitHub");

        LocalDate startDate = LocalDate.of(2023, 1, 1);
        SseEmitter emitter = mock(SseEmitter.class);

        Path repositoryPath = Path.of("/local/repo");
        Stream<Commit> commits = Stream.empty();
        CommitStream commitStream = mockCommitStream(commits);

        CompletableFuture<SonarResultDownloader.SonarAnalysisResults> sonarResults =
                CompletableFuture.completedFuture(
                        new SonarResultDownloader.SonarAnalysisResults(
                                new SonarRepoAnalysisResult(),
                                List.of(new SonarRepoAnalysisComponent()),
                                List.of(new SonarIssue())
                        )
                );

        KnowledgeAnalyzerContext knowledgeContext = mock(KnowledgeAnalyzerContext.class);
        AuthorsAnalyzerContext authorsContext = mock(AuthorsAnalyzerContext.class);

        when(logExtractor.extractAndParseCommits(eq(repositoryPath), eq(startDate), eq(null)))
                .thenReturn(commitStream);
        when(sonarService.runAnalysis(anyString(), eq(repositoryPath), anyString(), anyString()))
                .thenReturn(sonarResults);
        when(knowledgeAnalyzer.startAnalysis(anyString(), eq(repositoryPath))).thenReturn(knowledgeContext);
        when(authorsAnalyzer.startAnalysis(anyString(), eq(null))).thenReturn(authorsContext);
        when(fileInfoAnalyzer.startAnalysis(anyString(), eq(repositoryPath), eq(null)))
                .thenReturn(mock(FileInfoAnalyzerContext.class));
        when(activityTrendsAnalyzer.startAnalysis(anyString(), eq(null)))
                .thenReturn(mock(ActivityTrendsContext.class));
        when(couplingAnalyzer.startAnalysis(anyString(), eq(repositoryPath), eq(null)))
                .thenReturn(mock(CouplingAnalyzerContext.class));

        repositoryAnalysisService.runRepositoryAnalysis(repositoryInfo, startDate, null, emitter, null);

        verify(knowledgeAnalyzer).enrichAnalysisData(eq(knowledgeContext));
        verify(authorsAnalyzer).enrichAnalysisData(eq(authorsContext));
    }

    @Test
    void handlesSonarFailureGracefully() {
        RepositoryInfo repositoryInfo = mock(RepositoryInfo.class);
        when(repositoryInfo.getRemoteUrl()).thenReturn("https://example.com/repo.git");
        when(repositoryInfo.getLocalPath()).thenReturn("/local/repo");
        when(repositoryInfo.getName()).thenReturn("repo-name");
        when(repositoryInfo.getOwner()).thenReturn("repo-owner");
        when(repositoryInfo.getPlatform()).thenReturn("GitHub");

        LocalDate startDate = LocalDate.of(2023, 1, 1);
        SseEmitter emitter = mock(SseEmitter.class);

        Path repositoryPath = Path.of("/local/repo");
        Stream<Commit> commits = Stream.empty();
        CommitStream commitStream = mockCommitStream(commits);

        CompletableFuture<SonarResultDownloader.SonarAnalysisResults> sonarResults =
                CompletableFuture.failedFuture(new RuntimeException("Sonar analysis failed"));

        when(logExtractor.extractAndParseCommits(eq(repositoryPath), eq(startDate), eq(null)))
                .thenReturn(commitStream);
        when(sonarService.runAnalysis(anyString(), eq(repositoryPath), anyString(), anyString()))
                .thenReturn(sonarResults);
        when(knowledgeAnalyzer.startAnalysis(anyString(), eq(repositoryPath)))
                .thenReturn(mock(KnowledgeAnalyzerContext.class));
        when(authorsAnalyzer.startAnalysis(anyString(), eq(null)))
                .thenReturn(mock(AuthorsAnalyzerContext.class));
        when(fileInfoAnalyzer.startAnalysis(anyString(), eq(repositoryPath), eq(null)))
                .thenReturn(mock(FileInfoAnalyzerContext.class));
        when(activityTrendsAnalyzer.startAnalysis(anyString(), eq(null)))
                .thenReturn(mock(ActivityTrendsContext.class));
        when(couplingAnalyzer.startAnalysis(anyString(), eq(repositoryPath), eq(null)))
                .thenReturn(mock(CouplingAnalyzerContext.class));

        repositoryAnalysisService.runRepositoryAnalysis(repositoryInfo, startDate, null, emitter, null);

        verify(ssePublisher).sendSuccess(eq(emitter), anyString());
        ArgumentCaptor<AnalysisInfo> captor = ArgumentCaptor.forClass(AnalysisInfo.class);
        verify(analysisInfoRepository, atLeastOnce()).save(captor.capture());
        AnalysisInfo finalInfo = captor.getValue();
        assertEquals(AnalysisInfo.AnalysisStatus.COMPLETED, finalInfo.getStatus());
    }

    @Test
    void handlesSonarInterruptedException() throws Exception {
        RepositoryInfo repositoryInfo = mock(RepositoryInfo.class);
        when(repositoryInfo.getRemoteUrl()).thenReturn("https://example.com/repo.git");
        when(repositoryInfo.getLocalPath()).thenReturn("/local/repo");
        when(repositoryInfo.getName()).thenReturn("repo-name");
        when(repositoryInfo.getOwner()).thenReturn("repo-owner");
        when(repositoryInfo.getPlatform()).thenReturn("GitHub");

        LocalDate startDate = LocalDate.of(2023, 1, 1);
        SseEmitter emitter = mock(SseEmitter.class);

        Path repositoryPath = Path.of("/local/repo");
        Stream<Commit> commits = Stream.empty();
        CommitStream commitStream = mockCommitStream(commits);

        CompletableFuture<SonarResultDownloader.SonarAnalysisResults> sonarResults = mock(CompletableFuture.class);
        when(sonarResults.get()).thenThrow(new InterruptedException("Interrupted"));

        when(logExtractor.extractAndParseCommits(eq(repositoryPath), eq(startDate), eq(null)))
                .thenReturn(commitStream);
        when(sonarService.runAnalysis(anyString(), eq(repositoryPath), anyString(), anyString()))
                .thenReturn(sonarResults);
        when(knowledgeAnalyzer.startAnalysis(anyString(), eq(repositoryPath)))
                .thenReturn(mock(KnowledgeAnalyzerContext.class));
        when(authorsAnalyzer.startAnalysis(anyString(), eq(null)))
                .thenReturn(mock(AuthorsAnalyzerContext.class));
        when(fileInfoAnalyzer.startAnalysis(anyString(), eq(repositoryPath), eq(null)))
                .thenReturn(mock(FileInfoAnalyzerContext.class));
        when(activityTrendsAnalyzer.startAnalysis(anyString(), eq(null)))
                .thenReturn(mock(ActivityTrendsContext.class));
        when(couplingAnalyzer.startAnalysis(anyString(), eq(repositoryPath), eq(null)))
                .thenReturn(mock(CouplingAnalyzerContext.class));

        repositoryAnalysisService.runRepositoryAnalysis(repositoryInfo, startDate, null, emitter, null);

        verify(ssePublisher).sendSuccess(eq(emitter), anyString());
        ArgumentCaptor<AnalysisInfo> captor = ArgumentCaptor.forClass(AnalysisInfo.class);
        verify(analysisInfoRepository, atLeastOnce()).save(captor.capture());
        AnalysisInfo finalInfo = captor.getValue();
        assertEquals(AnalysisInfo.AnalysisStatus.COMPLETED, finalInfo.getStatus());
    }

    @Test
    void handlesInvalidRepositoryUrlExceptionWithSseError() {
        RepositoryInfo repositoryInfo = mock(RepositoryInfo.class);
        when(repositoryInfo.getRemoteUrl()).thenReturn("invalid-url");
        when(repositoryInfo.getLocalPath()).thenReturn("/local/repo");
        when(repositoryInfo.getName()).thenReturn("repo-name");
        when(repositoryInfo.getOwner()).thenReturn("repo-owner");
        when(repositoryInfo.getPlatform()).thenReturn("GitHub");

        LocalDate startDate = LocalDate.of(2023, 1, 1);
        SseEmitter emitter = mock(SseEmitter.class);

        Path repositoryPath = Path.of("/local/repo");

        when(logExtractor.extractAndParseCommits(eq(repositoryPath), eq(startDate), eq(null)))
                .thenThrow(new InvalidRepositoryUrlException("Invalid URL"));

        repositoryAnalysisService.runRepositoryAnalysis(repositoryInfo, startDate, null, emitter, null);

        verify(ssePublisher).sendError(emitter, "Invalid URL");
        verify(emitter).complete();
    }

    @Test
    void handlesRepositoryCloneExceptionWithSseError() {
        RepositoryInfo repositoryInfo = mock(RepositoryInfo.class);
        when(repositoryInfo.getRemoteUrl()).thenReturn("https://example.com/repo.git");
        when(repositoryInfo.getLocalPath()).thenReturn("/local/repo");
        when(repositoryInfo.getName()).thenReturn("repo-name");
        when(repositoryInfo.getOwner()).thenReturn("repo-owner");
        when(repositoryInfo.getPlatform()).thenReturn("GitHub");

        LocalDate startDate = LocalDate.of(2023, 1, 1);
        SseEmitter emitter = mock(SseEmitter.class);

        Path repositoryPath = Path.of("/local/repo");

        when(logExtractor.extractAndParseCommits(eq(repositoryPath), eq(startDate), eq(null)))
                .thenThrow(new RepositoryCloneException("Clone failed"));

        repositoryAnalysisService.runRepositoryAnalysis(repositoryInfo, startDate, null, emitter, null);

        verify(ssePublisher).sendError(emitter, "Clone failed");
        verify(emitter).complete();
    }

    @Test
    void handlesRepositoryUpdateExceptionWithSseError() {
        RepositoryInfo repositoryInfo = mock(RepositoryInfo.class);
        when(repositoryInfo.getRemoteUrl()).thenReturn("https://example.com/repo.git");
        when(repositoryInfo.getLocalPath()).thenReturn("/local/repo");
        when(repositoryInfo.getName()).thenReturn("repo-name");
        when(repositoryInfo.getOwner()).thenReturn("repo-owner");
        when(repositoryInfo.getPlatform()).thenReturn("GitHub");

        LocalDate startDate = LocalDate.of(2023, 1, 1);
        SseEmitter emitter = mock(SseEmitter.class);

        Path repositoryPath = Path.of("/local/repo");

        when(logExtractor.extractAndParseCommits(eq(repositoryPath), eq(startDate), eq(null)))
                .thenThrow(new RepositoryUpdateException("Update failed"));

        repositoryAnalysisService.runRepositoryAnalysis(repositoryInfo, startDate, null, emitter, null);

        verify(ssePublisher).sendError(emitter, "Update failed");
        verify(emitter).complete();
    }

    @Test
    void handlesLogProcessingExceptionWithSseError() {
        RepositoryInfo repositoryInfo = mock(RepositoryInfo.class);
        when(repositoryInfo.getRemoteUrl()).thenReturn("https://example.com/repo.git");
        when(repositoryInfo.getLocalPath()).thenReturn("/local/repo");
        when(repositoryInfo.getName()).thenReturn("repo-name");
        when(repositoryInfo.getOwner()).thenReturn("repo-owner");
        when(repositoryInfo.getPlatform()).thenReturn("GitHub");

        LocalDate startDate = LocalDate.of(2023, 1, 1);
        SseEmitter emitter = mock(SseEmitter.class);

        Path repositoryPath = Path.of("/local/repo");

        when(analysisInfoRepository.save(any(AnalysisInfo.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(logExtractor.extractAndParseCommits(eq(repositoryPath), eq(startDate), eq(null)))
                .thenThrow(new LogProcessingException("Log processing failed"));

        repositoryAnalysisService.runRepositoryAnalysis(repositoryInfo, startDate, null, emitter, null);

        verify(ssePublisher).sendError(emitter, "Log processing failed");
        verify(emitter).complete();
    }

    @Test
    void handlesAnalysisExceptionWithSseError() {
        RepositoryInfo repositoryInfo = mock(RepositoryInfo.class);
        when(repositoryInfo.getRemoteUrl()).thenReturn("https://example.com/repo.git");
        when(repositoryInfo.getLocalPath()).thenReturn("/local/repo");
        when(repositoryInfo.getName()).thenReturn("repo-name");
        when(repositoryInfo.getOwner()).thenReturn("repo-owner");
        when(repositoryInfo.getPlatform()).thenReturn("GitHub");

        LocalDate startDate = LocalDate.of(2023, 1, 1);
        SseEmitter emitter = mock(SseEmitter.class);

        Path repositoryPath = Path.of("/local/repo");

        when(analysisInfoRepository.save(any(AnalysisInfo.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(logExtractor.extractAndParseCommits(eq(repositoryPath), eq(startDate), eq(null)))
                .thenThrow(new RuntimeException("Parsing failed"));

        repositoryAnalysisService.runRepositoryAnalysis(repositoryInfo, startDate, null, emitter, null);

        verify(ssePublisher).sendError(emitter, "Parsing failed");
        verify(emitter).complete();
    }

    @Test
    void handlesUnexpectedExceptionWithSseErrorMessage() {
        RepositoryInfo repositoryInfo = mock(RepositoryInfo.class);
        when(repositoryInfo.getRemoteUrl()).thenReturn("https://example.com/repo.git");
        when(repositoryInfo.getLocalPath()).thenReturn("/local/repo");
        when(repositoryInfo.getName()).thenReturn("repo-name");
        when(repositoryInfo.getOwner()).thenReturn("repo-owner");
        when(repositoryInfo.getPlatform()).thenReturn("GitHub");

        LocalDate startDate = LocalDate.of(2023, 1, 1);
        SseEmitter emitter = mock(SseEmitter.class);

        Path repositoryPath = Path.of("/local/repo");

        when(logExtractor.extractAndParseCommits(eq(repositoryPath), eq(startDate), eq(null)))
                .thenThrow(new RuntimeException("Unexpected error"));

        repositoryAnalysisService.runRepositoryAnalysis(repositoryInfo, startDate, null, emitter, null);

        verify(ssePublisher).sendError(emitter, "Unexpected error");
        verify(emitter).complete();
    }

    @Test
    void marksAnalysisAsFailedWhenLogProcessingFails() {
        RepositoryInfo repositoryInfo = mock(RepositoryInfo.class);
        when(repositoryInfo.getRemoteUrl()).thenReturn("https://example.com/repo.git");
        when(repositoryInfo.getLocalPath()).thenReturn("/local/repo");
        when(repositoryInfo.getName()).thenReturn("repo-name");
        when(repositoryInfo.getOwner()).thenReturn("repo-owner");
        when(repositoryInfo.getPlatform()).thenReturn("GitHub");

        LocalDate startDate = LocalDate.of(2023, 1, 1);
        SseEmitter emitter = mock(SseEmitter.class);

        Path repositoryPath = Path.of("/local/repo");
        AnalysisInfo analysisInfo = new AnalysisInfo();
        analysisInfo.setId("test-id");

        when(analysisInfoRepository.save(any(AnalysisInfo.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(logExtractor.extractAndParseCommits(eq(repositoryPath), eq(startDate), eq(null)))
                .thenThrow(new LogProcessingException("Log extraction failed"));

        repositoryAnalysisService.runRepositoryAnalysis(repositoryInfo, startDate, null, emitter, null);

        ArgumentCaptor<AnalysisInfo> captor = ArgumentCaptor.forClass(AnalysisInfo.class);
        verify(analysisInfoRepository, atLeastOnce()).save(captor.capture());

        boolean hasFailedStatus = captor.getAllValues().stream()
                .anyMatch(info -> info.getStatus() == AnalysisInfo.AnalysisStatus.FAILED);
        assertTrue(hasFailedStatus, "Analysis should be marked as FAILED");

        verify(ssePublisher).sendError(emitter, "Log extraction failed");
        verify(emitter).complete();
    }

    @Test
    void marksAnalysisAsFailedWhenUnexpectedErrorOccurs() {
        RepositoryInfo repositoryInfo = mock(RepositoryInfo.class);
        when(repositoryInfo.getRemoteUrl()).thenReturn("https://example.com/repo.git");
        when(repositoryInfo.getLocalPath()).thenReturn("/local/repo");
        when(repositoryInfo.getName()).thenReturn("repo-name");
        when(repositoryInfo.getOwner()).thenReturn("repo-owner");
        when(repositoryInfo.getPlatform()).thenReturn("GitHub");

        LocalDate startDate = LocalDate.of(2023, 1, 1);
        SseEmitter emitter = mock(SseEmitter.class);

        Path repositoryPath = Path.of("/local/repo");
        AnalysisInfo analysisInfo = new AnalysisInfo();
        analysisInfo.setId("test-id");

        when(analysisInfoRepository.save(any(AnalysisInfo.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(logExtractor.extractAndParseCommits(eq(repositoryPath), eq(startDate), eq(null)))
                .thenThrow(new RuntimeException("Unexpected error"));

        repositoryAnalysisService.runRepositoryAnalysis(repositoryInfo, startDate, null, emitter, null);

        ArgumentCaptor<AnalysisInfo> captor = ArgumentCaptor.forClass(AnalysisInfo.class);
        verify(analysisInfoRepository, atLeastOnce()).save(captor.capture());

        boolean hasFailedStatus = captor.getAllValues().stream()
                .anyMatch(info -> info.getStatus() == AnalysisInfo.AnalysisStatus.FAILED);
        assertTrue(hasFailedStatus, "Analysis should be marked as FAILED");

        verify(ssePublisher).sendError(emitter, "Unexpected error");
        verify(emitter).complete();
    }

}
