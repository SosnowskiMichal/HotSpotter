package pwr.zpi.hotspotter.unit.repositoryanalysis.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import pwr.zpi.hotspotter.repositoryanalysis.exception.LogProcessingException;
import pwr.zpi.hotspotter.repositoryanalysis.service.RepositoryAnalysisOrchestrationService;
import pwr.zpi.hotspotter.repositoryanalysis.service.RepositoryAnalysisService;
import pwr.zpi.hotspotter.repositoryanalysis.sse.RepositoryAnalysisSsePublisher;
import pwr.zpi.hotspotter.repositorymanagement.exception.InvalidRepositoryUrlException;
import pwr.zpi.hotspotter.repositorymanagement.exception.RepositoryCloneException;

import java.time.LocalDate;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepositoryAnalysisOrchestrationServiceTest {

    @Mock
    private RepositoryAnalysisService repositoryAnalysisService;
    @Mock
    private RepositoryAnalysisSsePublisher sse;
    @Mock
    private SseEmitter emitter;

    @InjectMocks
    private RepositoryAnalysisOrchestrationService orchestrationService;

    @Test
    void completesAnalysisSuccessfully() {
        String repoUrl = "https://example.com/repo.git";
        LocalDate start = LocalDate.of(2023, 1, 1);
        LocalDate end = LocalDate.of(2023, 12, 31);

        orchestrationService.startAsyncAnalysis(repoUrl, start, end, emitter);

        verify(repositoryAnalysisService).runRepositoryAnalysis(repoUrl, start, end, emitter);
        verify(emitter).complete();
    }

    @Test
    void handlesInvalidRepositoryUrlException() {
        String repoUrl = "invalid-url";
        LocalDate start = LocalDate.of(2023, 1, 1);
        LocalDate end = LocalDate.of(2023, 12, 31);
        doThrow(new InvalidRepositoryUrlException("Invalid URL")).when(repositoryAnalysisService)
                .runRepositoryAnalysis(repoUrl, start, end, emitter);

        orchestrationService.startAsyncAnalysis(repoUrl, start, end, emitter);

        verify(sse).sendError(emitter, "Invalid URL");
        verify(emitter).complete();
    }

    @Test
    void handlesRepositoryOperationException() {
        String repoUrl = "https://example.com/repo.git";
        LocalDate start = LocalDate.of(2023, 1, 1);
        LocalDate end = LocalDate.of(2023, 12, 31);
        doThrow(new RepositoryCloneException("Clone failed")).when(repositoryAnalysisService)
                .runRepositoryAnalysis(repoUrl, start, end, emitter);

        orchestrationService.startAsyncAnalysis(repoUrl, start, end, emitter);

        verify(sse).sendError(emitter, "Clone failed");
        verify(emitter).complete();
    }

    @Test
    void handlesLogProcessingException() {
        String repoUrl = "https://example.com/repo.git";
        LocalDate start = LocalDate.of(2023, 1, 1);
        LocalDate end = LocalDate.of(2023, 12, 31);
        doThrow(new LogProcessingException("Log processing failed")).when(repositoryAnalysisService)
                .runRepositoryAnalysis(repoUrl, start, end, emitter);

        orchestrationService.startAsyncAnalysis(repoUrl, start, end, emitter);

        verify(sse).sendError(emitter, "Log processing failed");
        verify(emitter).complete();
    }

    @Test
    void handlesUnexpectedException() {
        String repoUrl = "https://example.com/repo.git";
        LocalDate start = LocalDate.of(2023, 1, 1);
        LocalDate end = LocalDate.of(2023, 12, 31);
        doThrow(new RuntimeException("Unexpected error")).when(repositoryAnalysisService)
                .runRepositoryAnalysis(repoUrl, start, end, emitter);

        orchestrationService.startAsyncAnalysis(repoUrl, start, end, emitter);

        verify(sse).sendError(emitter, "Unexpected error");
        verify(emitter).complete();
    }
}
