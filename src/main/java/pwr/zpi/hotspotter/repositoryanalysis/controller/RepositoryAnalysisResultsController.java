package pwr.zpi.hotspotter.repositoryanalysis.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pwr.zpi.hotspotter.repositoryanalysis.dto.*;
import pwr.zpi.hotspotter.repositoryanalysis.service.RepositoryAnalysisResultsService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/analysis")
public class RepositoryAnalysisResultsController {

    private final RepositoryAnalysisResultsService repositoryAnalysisResultsService;

    @GetMapping("/{analysisId}/structure")
    public ResponseEntity<RepositoryStructureNode> getRepositoryStructure(@PathVariable String analysisId) {
        RepositoryStructureNode response = repositoryAnalysisResultsService.getRepositoryStructure(analysisId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{analysisId}/files")
    public ResponseEntity<List<FilePathNameDTO>> getAllFilesInRepository(@PathVariable String analysisId) {
        List<FilePathNameDTO> files = repositoryAnalysisResultsService.getAllFilesInRepository(analysisId);
        return ResponseEntity.ok(files);
    }

    @GetMapping("/{analysisId}/file")
    public ResponseEntity<FileDataDTO> getFileData(@PathVariable String analysisId, @RequestParam String path) {
        FileDataDTO fileData = repositoryAnalysisResultsService.getFileData(analysisId, path);
        return ResponseEntity.ok(fileData);
    }

    @GetMapping("/{analysisId}/authors")
    public ResponseEntity<List<AuthorSummaryDTO>> getAllAuthors(@PathVariable String analysisId) {
        List<AuthorSummaryDTO> authors = repositoryAnalysisResultsService.getAllAuthors(analysisId);
        return ResponseEntity.ok(authors);
    }

    @GetMapping("/{analysisId}/authors/statistics")
    public ResponseEntity<?> getAllAuthorsStatistics(
            @PathVariable String analysisId,
            @RequestParam(required = false) String name
    ) {
        if (name == null) {
            List<AuthorStatisticsDTO> authorsStatistics = repositoryAnalysisResultsService.getAllAuthorsStatistics(analysisId);
            return ResponseEntity.ok(authorsStatistics);
        }

        AuthorStatisticsDTO authorStatistics = repositoryAnalysisResultsService.getAuthorStatistics(analysisId, name);
        return ResponseEntity.ok(authorStatistics);
    }

    @GetMapping("/{analysisId}/authors/coupling")
    public ResponseEntity<List<AuthorCouplingDTO>> getAllAuthorsCoupling(@PathVariable String analysisId) {
        List<AuthorCouplingDTO> authorsCouplings = repositoryAnalysisResultsService.getAllAuthorsCouplings(analysisId);
        return ResponseEntity.ok(authorsCouplings);
    }

}
