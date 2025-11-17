package pwr.zpi.hotspotter.repositoryanalysis.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pwr.zpi.hotspotter.repositoryanalysis.dto.FileDataDTO;
import pwr.zpi.hotspotter.repositoryanalysis.dto.FilePathNameDTO;
import pwr.zpi.hotspotter.repositoryanalysis.dto.RepositoryStructureNode;
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
    public ResponseEntity<List<FilePathNameDTO>> getFilesInRepository(@PathVariable String analysisId) {
        List<FilePathNameDTO> files = repositoryAnalysisResultsService.getFilesInRepository(analysisId);
        return ResponseEntity.ok(files);
    }

    @GetMapping("/{analysisId}/file")
    public ResponseEntity<FileDataDTO> getFileData(@PathVariable String analysisId, @RequestParam String path) {
        FileDataDTO fileData = repositoryAnalysisResultsService.getFileData(analysisId, path);
        return ResponseEntity.ok(fileData);
    }

}
