package pwr.zpi.hotspotter.repositoryanalysis.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pwr.zpi.hotspotter.repositoryanalysis.service.RepositoryAnalysisService;

import java.time.LocalDate;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/analysis")
public class RepositoryAnalysisController {

    private final RepositoryAnalysisService repositoryAnalysisService;

    @PostMapping()
    public ResponseEntity<AnalysisResult> analyzeRepository(@RequestBody AnalysisRequest request) {
        AnalysisResult result = repositoryAnalysisService.runRepositoryAnalysis(
                request.repositoryUrl(), request.startDate(), request.endDate());

        return result.success ?
                ResponseEntity.ok(result) :
                ResponseEntity.unprocessableEntity().body(result);
    }

    public record AnalysisRequest(String repositoryUrl, LocalDate startDate, LocalDate endDate) { }

    public record AnalysisResult(boolean success, String message, String analysisId) {
        public static AnalysisResult success(String message, String analysisId) {
            return new AnalysisResult(true, message, analysisId);
        }

        public static AnalysisResult failure(String message) {
            return new AnalysisResult(false, message, null);
        }
    }

}
