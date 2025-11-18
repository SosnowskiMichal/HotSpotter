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
    public ResponseEntity<?> getAllFilesInRepository(
            @PathVariable String analysisId,
            @RequestParam(required = false) String path
    ) {
        if (path != null) {
            FileDataDTO fileData = repositoryAnalysisResultsService.getFileData(analysisId, path);
            return ResponseEntity.ok(fileData);
        }

        List<FilePathNameDTO> files = repositoryAnalysisResultsService.getAllFilesInRepository(analysisId);
        return ResponseEntity.ok(files);
    }

    @GetMapping("/{analysisId}/files/coupling")
    public ResponseEntity<List<FileCouplingDTO>> getAllFilesCoupling(@PathVariable String analysisId) {
        List<FileCouplingDTO> filesCouplings = repositoryAnalysisResultsService.getAllFilesCoupling(analysisId);
        return ResponseEntity.ok(filesCouplings);
    }

    @GetMapping("/{analysisId}/files/code-age")
    public ResponseEntity<List<FileCodeAgeDTO>> getAllFilesCodeAge(@PathVariable String analysisId) {
        List<FileCodeAgeDTO> filesCodeAge = repositoryAnalysisResultsService.getAllFilesCodeAge(analysisId);
        return ResponseEntity.ok(filesCodeAge);
    }

    @GetMapping("/{analysisId}/files/knowledge-loss-risk")
    public ResponseEntity<List<FileKnowledgeLossRiskDTO>> getAllFilesKnowledgeLossRisk(@PathVariable String analysisId) {
        List<FileKnowledgeLossRiskDTO> filesKnowledgeLossRisk = repositoryAnalysisResultsService.getAllFilesKnowledgeLossRisk(analysisId);
        return ResponseEntity.ok(filesKnowledgeLossRisk);
    }

    @GetMapping("/{analysisId}/files/lead-authors")
    public ResponseEntity<List<FileLeadAuthorDTO>> getAllFilesLeadAuthors(@PathVariable String analysisId) {
        List<FileLeadAuthorDTO> filesLeadAuthors = repositoryAnalysisResultsService.getAllFilesLeadAuthors(analysisId);
        return ResponseEntity.ok(filesLeadAuthors);
    }

    @GetMapping("/{analysisId}/files/hotspots")
    public ResponseEntity<List<HotspotDTO>> getHotspots(@PathVariable String analysisId) {
        List<HotspotDTO> hotspots = repositoryAnalysisResultsService.getHotspots(analysisId);
        return ResponseEntity.ok(hotspots);
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
        if (name != null) {
            AuthorStatisticsDTO authorStatistics = repositoryAnalysisResultsService.getAuthorStatistics(analysisId, name);
            return ResponseEntity.ok(authorStatistics);
        }

        List<AuthorStatisticsDTO> authorsStatistics = repositoryAnalysisResultsService.getAllAuthorsStatistics(analysisId);
        return ResponseEntity.ok(authorsStatistics);
    }

    @GetMapping("/{analysisId}/authors/coupling")
    public ResponseEntity<List<AuthorCouplingDTO>> getAllAuthorsCoupling(@PathVariable String analysisId) {
        List<AuthorCouplingDTO> authorsCouplings = repositoryAnalysisResultsService.getAllAuthorsCouplings(analysisId);
        return ResponseEntity.ok(authorsCouplings);
    }

    @GetMapping("/{analysisId}/trends")
    public ResponseEntity<List<DailyStatsDTO>> getActivityTrends(@PathVariable String analysisId) {
        List<DailyStatsDTO> dailyStats = repositoryAnalysisResultsService.getActivityTrends(analysisId);
        return ResponseEntity.ok(dailyStats);
    }

}
