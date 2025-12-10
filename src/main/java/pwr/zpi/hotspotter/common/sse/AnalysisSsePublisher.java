package pwr.zpi.hotspotter.common.sse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Component
public class AnalysisSsePublisher {

    public void sendProgress(SseEmitter emitter, AnalysisSseStatus status) {
        if (emitter == null) return;
        sendEvent(emitter, "progress", status);
    }

    public void sendSuccess(SseEmitter emitter, String analysisId) {
        sendEvent(emitter, "success", analysisId);
    }

    public void sendError(SseEmitter emitter, String message) {
        sendEvent(emitter, "error", message);
    }

    private void sendEvent(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
        } catch (IOException _) {}
    }

}
