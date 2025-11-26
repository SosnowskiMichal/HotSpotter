package pwr.zpi.hotspotter.unit.repositoryanalysis.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import pwr.zpi.hotspotter.repositoryanalysis.exception.AnalysisException;
import pwr.zpi.hotspotter.repositoryanalysis.exception.LogProcessingException;
import pwr.zpi.hotspotter.repositoryanalysis.service.AsyncRepositoryAnalysisService;
import pwr.zpi.hotspotter.repositoryanalysis.service.RepositoryAnalysisService;
import pwr.zpi.hotspotter.common.sse.AnalysisSsePublisher;
import pwr.zpi.hotspotter.repositorymanagement.exception.InvalidRepositoryUrlException;
import pwr.zpi.hotspotter.repositorymanagement.exception.RepositoryCloneException;
import pwr.zpi.hotspotter.repositorymanagement.exception.RepositoryUpdateException;
import pwr.zpi.hotspotter.repositorymanagement.model.RepositoryInfo;

import java.time.LocalDate;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsyncRepositoryAnalysisServiceTest {

    @Mock
    private RepositoryAnalysisService repositoryAnalysisService;
    @Mock
    private AnalysisSsePublisher ssePublisher;
    @Mock
    private SseEmitter emitter;

    @InjectMocks
    private AsyncRepositoryAnalysisService asyncRepositoryAnalysisService;

    @Test
    void completesAnalysisSuccessfully() {
        RepositoryInfo repositoryInfo = mock(RepositoryInfo.class);
        LocalDate start = LocalDate.of(2023, 1, 1);
        LocalDate end = LocalDate.of(2023, 12, 31);

        asyncRepositoryAnalysisService.runRepositoryAnalysis(repositoryInfo, start, end, emitter);

        verify(repositoryAnalysisService).runRepositoryAnalysis(repositoryInfo, start, end, emitter);
        verify(emitter).complete();
    }

    @Test
    void handlesInvalidRepositoryUrlException() {
        RepositoryInfo repositoryInfo = mock(RepositoryInfo.class);
        when(repositoryInfo.getRemoteUrl()).thenReturn("invalid-url");

        LocalDate start = LocalDate.of(2023, 1, 1);
        LocalDate end = LocalDate.of(2023, 12, 31);

        doThrow(new InvalidRepositoryUrlException("Invalid URL")).when(repositoryAnalysisService)
                .runRepositoryAnalysis(repositoryInfo, start, end, emitter);

        asyncRepositoryAnalysisService.runRepositoryAnalysis(repositoryInfo, start, end, emitter);

        verify(ssePublisher).sendError(emitter, "Invalid URL");
        verify(emitter).complete();
    }

    @Test
    void handlesRepositoryCloneException() {
        RepositoryInfo repositoryInfo = mock(RepositoryInfo.class);
        when(repositoryInfo.getRemoteUrl()).thenReturn("https://example.com/repo.git");

        LocalDate start = LocalDate.of(2023, 1, 1);
        LocalDate end = LocalDate.of(2023, 12, 31);

        doThrow(new RepositoryCloneException("Clone failed")).when(repositoryAnalysisService)
                .runRepositoryAnalysis(repositoryInfo, start, end, emitter);

        asyncRepositoryAnalysisService.runRepositoryAnalysis(repositoryInfo, start, end, emitter);

        verify(ssePublisher).sendError(emitter, "Clone failed");
        verify(emitter).complete();
    }

    @Test
    void handlesLogProcessingException() {
        RepositoryInfo repositoryInfo = mock(RepositoryInfo.class);
        when(repositoryInfo.getRemoteUrl()).thenReturn("https://example.com/repo.git");

        LocalDate start = LocalDate.of(2023, 1, 1);
        LocalDate end = LocalDate.of(2023, 12, 31);

        doThrow(new LogProcessingException("Log processing failed")).when(repositoryAnalysisService)
                .runRepositoryAnalysis(repositoryInfo, start, end, emitter);

        asyncRepositoryAnalysisService.runRepositoryAnalysis(repositoryInfo, start, end, emitter);

        verify(ssePublisher).sendError(emitter, "Log processing failed");
        verify(emitter).complete();
    }

    @Test
    void handlesUnexpectedException() {
        RepositoryInfo repositoryInfo = mock(RepositoryInfo.class);
        when(repositoryInfo.getRemoteUrl()).thenReturn("https://example.com/repo.git");

        LocalDate start = LocalDate.of(2023, 1, 1);
        LocalDate end = LocalDate.of(2023, 12, 31);

        doThrow(new RuntimeException("Unexpected error")).when(repositoryAnalysisService)
                .runRepositoryAnalysis(repositoryInfo, start, end, emitter);

        asyncRepositoryAnalysisService.runRepositoryAnalysis(repositoryInfo, start, end, emitter);

        verify(ssePublisher).sendError(emitter, "Unexpected error");
        verify(emitter).complete();
    }

    @Test
    void handlesRepositoryUpdateException() {
        RepositoryInfo repositoryInfo = mock(RepositoryInfo.class);
        when(repositoryInfo.getRemoteUrl()).thenReturn("https://example.com/repo.git");

        LocalDate start = LocalDate.of(2023, 1, 1);
        LocalDate end = LocalDate.of(2023, 12, 31);

        doThrow(new RepositoryUpdateException("Update failed")).when(repositoryAnalysisService)
                .runRepositoryAnalysis(repositoryInfo, start, end, emitter);

        asyncRepositoryAnalysisService.runRepositoryAnalysis(repositoryInfo, start, end, emitter);

        verify(ssePublisher).sendError(emitter, "Update failed");
        verify(emitter).complete();
    }

    @Test
    void handlesAnalysisException() {
        RepositoryInfo repositoryInfo = mock(RepositoryInfo.class);
        when(repositoryInfo.getRemoteUrl()).thenReturn("https://example.com/repo.git");

        LocalDate start = LocalDate.of(2023, 1, 1);
        LocalDate end = LocalDate.of(2023, 12, 31);

        doThrow(new AnalysisException("Analysis failed")).when(repositoryAnalysisService)
                .runRepositoryAnalysis(repositoryInfo, start, end, emitter);

        asyncRepositoryAnalysisService.runRepositoryAnalysis(repositoryInfo, start, end, emitter);

        verify(ssePublisher).sendError(emitter, "Analysis failed");
        verify(emitter).complete();
    }

}
