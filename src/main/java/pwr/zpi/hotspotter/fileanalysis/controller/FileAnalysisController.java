package pwr.zpi.hotspotter.fileanalysis.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import pwr.zpi.hotspotter.analysisqueue.AnalysisQueue;

@RestController
@RequiredArgsConstructor
@RequestMapping("/analysis")
public class FileAnalysisController {

    private final AnalysisQueue analysisQueue;

    @GetMapping("/{analysisId}/x-ray")
    public SseEmitter startFileAnalysis(@PathVariable String analysisId, @RequestParam String path) {
        SseEmitter emitter = new SseEmitter(0L);

        analysisQueue.submitFileAnalysis(analysisId, path, emitter);

        return emitter;
    }

}
