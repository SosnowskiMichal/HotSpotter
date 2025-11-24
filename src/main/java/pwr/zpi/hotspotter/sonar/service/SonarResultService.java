package pwr.zpi.hotspotter.sonar.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pwr.zpi.hotspotter.common.exceptions.ObjectNotFoundException;
import pwr.zpi.hotspotter.sonar.dto.SonarIssueDTO;
import pwr.zpi.hotspotter.sonar.mapper.SonarIssueMapper;
import pwr.zpi.hotspotter.sonar.model.fileanalysis.SonarIssue;
import pwr.zpi.hotspotter.sonar.model.repoanalysis.SonarRepoAnalysisResult;
import pwr.zpi.hotspotter.sonar.repository.SonarIssueRepository;
import pwr.zpi.hotspotter.sonar.repository.SonarRepoAnalysisRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SonarResultService {
    private final SonarRepoAnalysisRepository sonarRepoAnalysisRepository;
    private final SonarIssueRepository sonarIssueRepository;
    private final SonarIssueMapper sonarIssueMapper;
    private final SonarIssueTranslatorService sonarIssueTranslatorService;

    public SonarRepoAnalysisResult getSonarRepoAnalysisResult(String repoAnalysisId) {
        return sonarRepoAnalysisRepository.findByRepoAnalysisId(repoAnalysisId).orElseThrow(() ->
                new ObjectNotFoundException("SonarQube analysis result not found for ID: " + repoAnalysisId));
    }

    public List<SonarIssueDTO> getSonarIssuesDTOForFile(String repoAnalysisId, String filePath, String targetLanguage) {
        List<SonarIssue> issues = sonarIssueRepository.findAllByRepoAnalysisIdAndPath(repoAnalysisId, filePath);
        sonarIssueTranslatorService.translateIssueMessagesBulk(issues, targetLanguage);
        return issues.stream()
                .map(issue -> sonarIssueMapper.toDTO(issue, targetLanguage))
                .toList();
    }
}
