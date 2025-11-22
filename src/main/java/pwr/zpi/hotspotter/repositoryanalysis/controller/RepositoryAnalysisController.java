package pwr.zpi.hotspotter.repositoryanalysis.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import pwr.zpi.hotspotter.repositoryanalysis.queue.RepositoryAnalysisQueue;
import pwr.zpi.hotspotter.repositoryanalysis.service.AsyncRepositoryAnalysisService;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/analysis")
public class RepositoryAnalysisController {

    private final AsyncRepositoryAnalysisService asyncRepositoryAnalysisService;
    private final RepositoryAnalysisQueue repositoryAnalysisQueue;

    @GetMapping
    public SseEmitter analyzeRepository(@Valid @ModelAttribute AnalysisRequest request) {
        SseEmitter emitter = new SseEmitter(0L);

        repositoryAnalysisQueue.submitAnalysis(
                request.repositoryUrl(),
                request.startDate(),
                request.endDate(),
                emitter,
                asyncRepositoryAnalysisService
        );

        return emitter;
    }

    public record AnalysisRequest(
            @NotBlank(message = "Repository URL is required")
            String repositoryUrl,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate
    ) { }

}
