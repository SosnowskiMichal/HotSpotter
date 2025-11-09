package pwr.zpi.hotspotter.repositoryanalysis.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import pwr.zpi.hotspotter.repositoryanalysis.service.RepositoryAnalysisOrchestrationService;

import java.time.LocalDate;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/analysis")
public class RepositoryAnalysisController {

    private final RepositoryAnalysisOrchestrationService repositoryAnalysisOrchestrationService;

    @PostMapping
    public SseEmitter analyzeRepository(@Valid @RequestBody AnalysisRequest request) {
        SseEmitter emitter = new SseEmitter(0L);
        repositoryAnalysisOrchestrationService.startAsyncAnalysis(
                request.repositoryUrl(), request.startDate(), request.endDate(), emitter);

        return emitter;
    }

    public record AnalysisRequest(
            @NotBlank(message = "Repository URL is required")
            String repositoryUrl,
            LocalDate startDate,
            LocalDate endDate
    ) { }

}
