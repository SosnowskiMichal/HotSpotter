package pwr.zpi.hotspotter.repositoryanalysis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import pwr.zpi.hotspotter.repositoryanalysis.exception.AnalysisException;
import pwr.zpi.hotspotter.repositoryanalysis.exception.LogProcessingException;
import pwr.zpi.hotspotter.repositoryanalysis.sse.RepositoryAnalysisSsePublisher;
import pwr.zpi.hotspotter.repositorymanagement.exception.InvalidRepositoryUrlException;
import pwr.zpi.hotspotter.repositorymanagement.exception.RepositoryCloneException;
import pwr.zpi.hotspotter.repositorymanagement.exception.RepositoryUpdateException;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class RepositoryAnalysisOrchestrationService {

    private final RepositoryAnalysisService repositoryAnalysisService;
    private final RepositoryAnalysisSsePublisher sse;

    @Async("repoAnalysisExecutor")
    public void startAsyncAnalysis(String repoUrl, LocalDate start, LocalDate end, SseEmitter emitter) {
        try {
            repositoryAnalysisService.runRepositoryAnalysis(repoUrl, start, end, emitter);

        } catch (InvalidRepositoryUrlException e) {
            log.warn("Invalid repository URL {}: {}", repoUrl, e.getMessage());
            sse.sendError(emitter, e.getMessage());

        } catch (RepositoryCloneException | RepositoryUpdateException e) {
            log.error("Repository operation failed for {}: {}", repoUrl, e.getMessage());
            sse.sendError(emitter, e.getMessage());

        } catch (LogProcessingException e) {
            log.warn("Log processing failed for repository {}: {}", repoUrl, e.getMessage());
            sse.sendError(emitter, e.getMessage());

        } catch (AnalysisException e) {
            log.error("Analysis failed for repository {}: {}", repoUrl, e.getMessage());
            sse.sendError(emitter, e.getMessage());

        } catch (Exception e) {
            log.error("Unexpected error during analysis of repository {}: {}", repoUrl, e.getMessage());
            sse.sendError(emitter, e.getMessage());
        } finally {
            emitter.complete();
        }
    }

}
