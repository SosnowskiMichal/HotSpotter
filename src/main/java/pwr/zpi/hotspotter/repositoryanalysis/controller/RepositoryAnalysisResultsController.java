package pwr.zpi.hotspotter.repositoryanalysis.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pwr.zpi.hotspotter.repositoryanalysis.model.repositorystructure.RepositoryStructureResponse;
import pwr.zpi.hotspotter.repositoryanalysis.service.RepositoryAnalysisResultsService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/analysis")
public class RepositoryAnalysisResultsController {

    private final RepositoryAnalysisResultsService repositoryAnalysisResultsService;

    @GetMapping("/{analysisId}/structure")
    public ResponseEntity<RepositoryStructureResponse> getRepositoryStructure(@PathVariable String analysisId) {
        RepositoryStructureResponse response = repositoryAnalysisResultsService.getRepositoryStructure(analysisId);
        return ResponseEntity.ok(response);
    }

}
