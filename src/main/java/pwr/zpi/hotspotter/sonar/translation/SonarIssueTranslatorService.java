package pwr.zpi.hotspotter.sonar.translation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pwr.zpi.hotspotter.sonar.model.fileanalysis.SonarIssue;
import pwr.zpi.hotspotter.sonar.model.fileanalysis.SonarIssueLocation;
import pwr.zpi.hotspotter.sonar.repository.SonarIssueRepository;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SonarIssueTranslatorService {
    private final TranslatorService translatorService;
    private final SonarIssueRepository sonarIssueRepository;

    private static final String DEFAULT_LANGUAGE = "en";

    public void translateIssueMessage(SonarIssue sonarIssue, String targetLanguage) {
        if (targetLanguage == null || targetLanguage.equals(DEFAULT_LANGUAGE) || sonarIssue == null ||
                sonarIssue.getMessageTranslations().get(targetLanguage) != null) {
            return;
        }

        boolean changed = false;
        try {
            String message = sonarIssue.getMessage();
            if (hasLetters(message)) {
                String translatedMessage = translatorService.translate(
                        message,
                        DEFAULT_LANGUAGE,
                        targetLanguage
                );
                if (translatedMessage != null) {
                    sonarIssue.getMessageTranslations().put(targetLanguage, translatedMessage);
                    changed = true;
                }
            }
        } catch (IOException e) {
            log.error("Failed to translate sonar issue message.", e);
        }

        List<SonarIssueLocation> locations = sonarIssue.getLocations() != null ? sonarIssue.getLocations() : List.of();
        for (SonarIssueLocation location : locations) {
            try {
                String locationMessage = location.getMessage();
                if (hasLetters(locationMessage)) {
                    String translatedLocationMessage = translatorService.translate(
                            locationMessage,
                            DEFAULT_LANGUAGE,
                            targetLanguage
                    );
                    if (translatedLocationMessage != null) {
                        location.getMessageTranslations().put(targetLanguage, translatedLocationMessage);
                        changed = true;
                    }
                }
            } catch (IOException e) {
                log.error("Failed to translate sonar issue location message.", e);
            }
        }

        if (changed)
            sonarIssueRepository.save(sonarIssue);
    }

    public void translateIssueMessagesBulk(Iterable<SonarIssue> sonarIssues, String targetLanguage) {
        if (targetLanguage == null || targetLanguage.equals(DEFAULT_LANGUAGE)) {
            return;
        }

        sonarIssues.forEach(issue -> translateIssueMessage(issue, targetLanguage));
    }

    private boolean hasLetters(String s) {
        return s != null && s.chars().anyMatch(Character::isLetter);
    }
}
