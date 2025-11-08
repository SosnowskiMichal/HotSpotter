package pwr.zpi.hotspotter.repositoryanalysis.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pwr.zpi.hotspotter.repositoryanalysis.exception.AnalysisException;
import pwr.zpi.hotspotter.repositoryanalysis.exception.LogProcessingException;
import pwr.zpi.hotspotter.repositoryanalysis.service.RepositoryAnalysisService;
import pwr.zpi.hotspotter.repositorymanagement.exception.InvalidRepositoryUrlException;
import pwr.zpi.hotspotter.repositorymanagement.exception.RepositoryCloneException;
import pwr.zpi.hotspotter.repositorymanagement.exception.RepositoryUpdateException;

import java.time.LocalDate;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/analysis")
public class RepositoryAnalysisController {

    private final RepositoryAnalysisService repositoryAnalysisService;

    @PostMapping
    public ResponseEntity<String> analyzeRepository(@Valid @RequestBody AnalysisRequest request) {
        try {
            String analysisId = repositoryAnalysisService.runRepositoryAnalysis(
                    request.repositoryUrl(), request.startDate(), request.endDate());

            log.info("Analysis for repository {} completed successfully with ID {}", request.repositoryUrl(), analysisId);
            return ResponseEntity.ok(analysisId);

        } catch (InvalidRepositoryUrlException e) {
            log.warn("Invalid repository URL {}: {}", request.repositoryUrl(), e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (RepositoryCloneException | RepositoryUpdateException e) {
            log.error("Repository operation failed for {}: {}", request.repositoryUrl(), e.getMessage());
            return ResponseEntity.unprocessableEntity().body(e.getMessage());

        } catch (LogProcessingException e) {
            log.warn("Log processing failed for repository {}: {}", request.repositoryUrl(), e.getMessage());
            return ResponseEntity.unprocessableEntity().body(e.getMessage());

        } catch (AnalysisException e) {
            log.error("Analysis failed for repository {}: {}", request.repositoryUrl(), e.getMessage());
            return ResponseEntity.unprocessableEntity().body(e.getMessage());

        } catch (Exception e) {
            log.error("Unexpected error during analysis of repository {}: {}", request.repositoryUrl(), e.getMessage());
            return ResponseEntity.internalServerError().body("Analysis failed due to an unexpected error: " + e.getMessage());
        }
    }

    public record AnalysisRequest(
            @NotBlank(message = "Repository URL is required")
            String repositoryUrl,
            LocalDate startDate,
            LocalDate endDate
    ) { }

}
