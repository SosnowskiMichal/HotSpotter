package pwr.zpi.hotspotter.sonar.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.sonar.dto.SonarIssueDTO;
import pwr.zpi.hotspotter.sonar.model.fileanalysis.SonarIssue;
import pwr.zpi.hotspotter.sonar.model.fileanalysis.SonarIssueLocation;
import pwr.zpi.hotspotter.sonar.service.SonarIssueTranslatorService;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SonarIssueMapper {

    private final SonarIssueTranslatorService sonarIssueTranslatorService;

    public SonarIssueDTO toDTO(SonarIssue sonarIssue, String targetLanguage) {
        if (sonarIssue == null) return null;

        if (targetLanguage != null) {
            sonarIssueTranslatorService.translateIssueMessage(sonarIssue, targetLanguage);
        }
        String message = targetLanguage == null ? sonarIssue.getMessage() :
                sonarIssue.getMessageTranslations().getOrDefault(targetLanguage, sonarIssue.getMessage());

        List<SonarIssueDTO.SonarIssueLocationDTO> locationDTOs = sonarIssue.getLocations().stream()
                .map(location -> toDTO(location, targetLanguage))
                .toList();

        return new SonarIssueDTO(
                "gitUrl-placeholder",
                sonarIssue.getPath(),
                sonarIssue.getTextRange(),
                sonarIssue.getSeverity(),
                message,
                sonarIssue.getType(),
                sonarIssue.getRule(),
                sonarIssue.getEffort(),
                sonarIssue.getDebt(),
                sonarIssue.getAuthorEmail(),
                sonarIssue.getTags(),
                sonarIssue.getCreationDate(),
                sonarIssue.getUpdateDate(),
                locationDTOs
        );
    }

    private SonarIssueDTO.SonarIssueLocationDTO toDTO(SonarIssueLocation sonarIssueLocation, String targetLanguage) {
        if (sonarIssueLocation == null) return null;

        String message = targetLanguage == null ? sonarIssueLocation.getMessage() :
                sonarIssueLocation.getMessageTranslations().getOrDefault(targetLanguage, sonarIssueLocation.getMessage());

        return new SonarIssueDTO.SonarIssueLocationDTO(
                message,
                sonarIssueLocation.getTextRange()
        );
    }
}

