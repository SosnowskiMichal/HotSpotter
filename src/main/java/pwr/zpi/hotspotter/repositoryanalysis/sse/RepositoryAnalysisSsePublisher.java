package pwr.zpi.hotspotter.repositoryanalysis.sse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import pwr.zpi.hotspotter.repositoryanalysis.model.AnalysisInfo;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

@Component
public class RepositoryAnalysisSsePublisher {

    public void sendProgress(SseEmitter emitter, AnalysisInfo.AnalysisSseStatus status) {
        try {
            emitter.send(SseEmitter.event()
                    .name("progress")
                    .data(Map.of("status", status)));
        } catch (IOException ignored) {}
    }

    public void sendComplete(SseEmitter emitter, String analysisId) {
        try {
            emitter.send(SseEmitter.event()
                    .name("complete")
                    .data(Map.of(
                            "success", true,
                            "data", analysisId,
                            "timestamp", Instant.now().toString()
                    )));
        } catch (IOException ignored) {}
    }

    public void sendError(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data(Map.of("message", message)));
        } catch (IOException ignored) {}
    }
}
