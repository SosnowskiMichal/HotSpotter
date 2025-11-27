package pwr.zpi.hotspotter.fileanalysis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import pwr.zpi.hotspotter.repositoryanalysis.model.AnalysisInfo;
import pwr.zpi.hotspotter.repositorymanagement.model.RepositoryInfo;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileAnalysisService {

    public void runFileAnalysis(
            RepositoryInfo repositoryInfo,
            AnalysisInfo analysisInfo,
            String filePath,
            SseEmitter emitter
    ) {
        try {
            executeFileAnalysis(repositoryInfo, analysisInfo, filePath, emitter);

            // TODO: Custom exception handling

        } catch (Exception e) {
            // TODO: General exception handling

        } finally {
            emitter.complete();
        }
    }

    private void executeFileAnalysis(
            RepositoryInfo repositoryInfo,
            AnalysisInfo analysisInfo,
            String filePath,
            SseEmitter emitter
    ) {
        log.info("Starting analysis for file {} in repository: {}", filePath, repositoryInfo.getRemoteUrl());

        // TODO: Implement

    }

}
