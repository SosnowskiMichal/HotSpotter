package pwr.zpi.hotspotter.repositoryanalysis.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pwr.zpi.hotspotter.authentication.annotation.CurrentUser;
import pwr.zpi.hotspotter.repositoryanalysis.dto.AnalysisHistoryEntryDTO;
import pwr.zpi.hotspotter.repositoryanalysis.service.RepositoryAnalysisHistoryService;
import pwr.zpi.hotspotter.user.model.User;

@RestController
@RequiredArgsConstructor
@RequestMapping("/analysis")
public class RepositoryAnalysisHistoryController {

    private final RepositoryAnalysisHistoryService repositoryAnalysisHistoryService;

    @GetMapping("/history")
    public ResponseEntity<Page<AnalysisHistoryEntryDTO>> getAnalysisHistory(
            @CurrentUser User user,
            @PageableDefault(size = 20, sort = "analysisStartedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<AnalysisHistoryEntryDTO> history = repositoryAnalysisHistoryService.getUserAnalysisHistory(user, pageable);
        return ResponseEntity.ok(history);
    }

}
