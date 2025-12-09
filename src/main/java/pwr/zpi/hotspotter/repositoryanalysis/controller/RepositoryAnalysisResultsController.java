package pwr.zpi.hotspotter.repositoryanalysis.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pwr.zpi.hotspotter.repositoryanalysis.dto.*;
import pwr.zpi.hotspotter.repositoryanalysis.service.RepositoryAnalysisResultsService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/analysis")
public class RepositoryAnalysisResultsController {

    private final RepositoryAnalysisResultsService repositoryAnalysisResultsService;

    @GetMapping("/{analysisId}/summary")
    @PreAuthorize("@analysisSecurity.canRead(authentication, #analysisId)")
    public ResponseEntity<AnalysisSummaryDTO> getAnalysisSummary(@PathVariable String analysisId) {
        AnalysisSummaryDTO analysisSummary = repositoryAnalysisResultsService.getAnalysisSummary(analysisId);
        return ResponseEntity.ok(analysisSummary);
    }

    @GetMapping("/{analysisId}/structure")
    @PreAuthorize("@analysisSecurity.canRead(authentication, #analysisId)")
    public ResponseEntity<RepositoryStructureNode> getRepositoryStructure(@PathVariable String analysisId) {
        RepositoryStructureNode response = repositoryAnalysisResultsService.getRepositoryStructure(analysisId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{analysisId}/items")
    @PreAuthorize("@analysisSecurity.canRead(authentication, #analysisId)")
    public ResponseEntity<List<RepositoryItemDTO>> getAllItemsInRepository(@PathVariable String analysisId) {
        List<RepositoryItemDTO> items = repositoryAnalysisResultsService.getAllItemsInRepository(analysisId);
        return ResponseEntity.ok(items);
    }

    @GetMapping("/{analysisId}/files")
    @PreAuthorize("@analysisSecurity.canRead(authentication, #analysisId)")
    public ResponseEntity<?> getAllFilesInRepository(
            @PathVariable String analysisId,
            @RequestParam(required = false) String path
    ) {
        if (path != null) {
            FileDataDTO fileData = repositoryAnalysisResultsService.getFileData(analysisId, path);
            return ResponseEntity.ok(fileData);
        }

        List<FileDataDTO> files = repositoryAnalysisResultsService.getAllFilesData(analysisId);
        return ResponseEntity.ok(files);
    }

    @GetMapping("/{analysisId}/files/types")
    @PreAuthorize("@analysisSecurity.canRead(authentication, #analysisId)")
    public ResponseEntity<List<FileTypeDTO>> getAllFilesTypes(@PathVariable String analysisId) {
        List<FileTypeDTO> filesTypes = repositoryAnalysisResultsService.getAllFilesTypes(analysisId);
        return ResponseEntity.ok(filesTypes);
    }

    @GetMapping("/{analysisId}/files/coupling")
    @PreAuthorize("@analysisSecurity.canRead(authentication, #analysisId)")
    public ResponseEntity<List<FileCouplingDTO>> getAllFilesCoupling(@PathVariable String analysisId) {
        List<FileCouplingDTO> filesCouplings = repositoryAnalysisResultsService.getAllFilesCoupling(analysisId);
        return ResponseEntity.ok(filesCouplings);
    }

    @GetMapping("/{analysisId}/files/code-age")
    @PreAuthorize("@analysisSecurity.canRead(authentication, #analysisId)")
    public ResponseEntity<List<FileCodeAgeDTO>> getAllFilesCodeAge(@PathVariable String analysisId) {
        List<FileCodeAgeDTO> filesCodeAge = repositoryAnalysisResultsService.getAllFilesCodeAge(analysisId);
        return ResponseEntity.ok(filesCodeAge);
    }

    @GetMapping("/{analysisId}/files/knowledge-loss-risk")
    @PreAuthorize("@analysisSecurity.canRead(authentication, #analysisId)")
    public ResponseEntity<List<FileKnowledgeLossRiskDTO>> getAllFilesKnowledgeLossRisk(@PathVariable String analysisId) {
        List<FileKnowledgeLossRiskDTO> filesKnowledgeLossRisk = repositoryAnalysisResultsService.getAllFilesKnowledgeLossRisk(analysisId);
        return ResponseEntity.ok(filesKnowledgeLossRisk);
    }

    @GetMapping("/{analysisId}/files/lead-authors")
    @PreAuthorize("@analysisSecurity.canRead(authentication, #analysisId)")
    public ResponseEntity<List<FileLeadAuthorDTO>> getAllFilesLeadAuthors(@PathVariable String analysisId) {
        List<FileLeadAuthorDTO> filesLeadAuthors = repositoryAnalysisResultsService.getAllFilesLeadAuthors(analysisId);
        return ResponseEntity.ok(filesLeadAuthors);
    }

    @GetMapping("/{analysisId}/files/hotspots")
    @PreAuthorize("@analysisSecurity.canRead(authentication, #analysisId)")
    public ResponseEntity<List<HotspotDTO>> getHotspots(@PathVariable String analysisId) {
        List<HotspotDTO> hotspots = repositoryAnalysisResultsService.getHotspots(analysisId);
        return ResponseEntity.ok(hotspots);
    }

    @GetMapping("/{analysisId}/authors")
    @PreAuthorize("@analysisSecurity.canRead(authentication, #analysisId)")
    public ResponseEntity<List<AuthorSummaryDTO>> getAllAuthors(@PathVariable String analysisId) {
        List<AuthorSummaryDTO> authors = repositoryAnalysisResultsService.getAllAuthors(analysisId);
        return ResponseEntity.ok(authors);
    }

    @GetMapping("/{analysisId}/authors/statistics")
    @PreAuthorize("@analysisSecurity.canRead(authentication, #analysisId)")
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
    @PreAuthorize("@analysisSecurity.canRead(authentication, #analysisId)")
    public ResponseEntity<List<AuthorCouplingDTO>> getAllAuthorsCoupling(@PathVariable String analysisId) {
        List<AuthorCouplingDTO> authorsCouplings = repositoryAnalysisResultsService.getAllAuthorsCouplings(analysisId);
        return ResponseEntity.ok(authorsCouplings);
    }

    @GetMapping("/{analysisId}/trends")
    @PreAuthorize("@analysisSecurity.canRead(authentication, #analysisId)")
    public ResponseEntity<List<DailyStatsDTO>> getActivityTrends(@PathVariable String analysisId) {
        List<DailyStatsDTO> dailyStats = repositoryAnalysisResultsService.getActivityTrends(analysisId);
        return ResponseEntity.ok(dailyStats);
    }

}
