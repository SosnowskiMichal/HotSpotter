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
import pwr.zpi.hotspotter.analysisqueue.AnalysisQueue;
import pwr.zpi.hotspotter.authentication.annotation.CurrentUser;
import pwr.zpi.hotspotter.repositoryanalysis.validation.ValidDateRange;
import pwr.zpi.hotspotter.user.model.User;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/analysis")
public class RepositoryAnalysisController {

    private final AnalysisQueue analysisQueue;

    @GetMapping
    public SseEmitter startRepositoryAnalysis(
            @Valid @ModelAttribute AnalysisRequest request,
            @CurrentUser(required = false) User user
    ) {
        SseEmitter emitter = new SseEmitter(0L);

        analysisQueue.submitRepositoryAnalysis(
                request.repositoryUrl(),
                request.startDate(),
                request.endDate(),
                emitter,
                user
        );

        return emitter;
    }

    @ValidDateRange
    public record AnalysisRequest(
            @NotBlank(message = "Repository URL is required")
            String repositoryUrl,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate
    ) { }

}
