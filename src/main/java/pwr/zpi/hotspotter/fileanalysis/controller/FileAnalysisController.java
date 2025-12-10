package pwr.zpi.hotspotter.fileanalysis.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pwr.zpi.hotspotter.analysisqueue.AnalysisQueue;
import pwr.zpi.hotspotter.common.exception.AnalysisException;
import pwr.zpi.hotspotter.fileanalysis.dto.FileAnalysisResultDTO;
import pwr.zpi.hotspotter.fileanalysis.service.FileAnalysisResultsService;

import java.util.concurrent.CompletableFuture;

@RestController
@RequiredArgsConstructor
@RequestMapping("/analysis")
public class FileAnalysisController {

    private final AnalysisQueue analysisQueue;
    private final FileAnalysisResultsService fileAnalysisResultsService;

    @GetMapping("/{analysisId}/x-ray")
    public ResponseEntity<FileAnalysisResultDTO> startFileAnalysis(@PathVariable String analysisId, @RequestParam String path) {
        if (fileAnalysisResultsService.checkIfFileAnalysisCompleted(analysisId, path)) {
            FileAnalysisResultDTO analysisResult = fileAnalysisResultsService.getFileAnalysisResult(analysisId, path);
            return ResponseEntity.ok(analysisResult);
        }

        if (fileAnalysisResultsService.checkIfFileAnalysisResultExists(analysisId, path)) {
            throw new IllegalStateException("File analysis for '" + path + "' is already in progress.");
        }

        CompletableFuture<Void> analysisFuture = analysisQueue.submitFileAnalysis(analysisId, path);
        try {
            analysisFuture.get();
            FileAnalysisResultDTO analysisResult = fileAnalysisResultsService.getFileAnalysisResult(analysisId, path);
            return ResponseEntity.ok(analysisResult);
        } catch (Exception e) {
            throw new AnalysisException("Error during file analysis for '" + path + "': " + e.getMessage());
        }
    }

}
