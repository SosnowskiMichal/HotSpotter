package pwr.zpi.hotspotter.sonar.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pwr.zpi.hotspotter.google.translation.TranslatorService;
import pwr.zpi.hotspotter.sonar.model.fileanalysis.SonarIssue;
import pwr.zpi.hotspotter.sonar.repository.SonarIssueRepository;

import java.io.IOException;

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
        try {
            String message = sonarIssue.getMessage();
            if (hasLetters(message)) {
                String translatedMessage = translatorService.translate(
                        message,
                        DEFAULT_LANGUAGE,
                        targetLanguage
                );
                if (translatedMessage != null)
                    sonarIssue.getMessageTranslations().put(targetLanguage, translatedMessage);
            }

            sonarIssue.getLocations().forEach(location -> {
                try {
                    String locationMessage = location.getMessage();
                    if (hasLetters(locationMessage)) {
                        String translatedLocationMessage = translatorService.translate(
                                locationMessage,
                                DEFAULT_LANGUAGE,
                                targetLanguage
                        );
                        if (translatedLocationMessage != null)
                            location.getMessageTranslations().put(targetLanguage, translatedLocationMessage);
                    }
                } catch (IOException e) {
                    log.error("Failed to translate sonar issue location message.", e);
                }
            });

            sonarIssueRepository.save(sonarIssue);
        } catch (IOException e) {
            log.error("Failed to translate sonar issue message.", e);
        }
    }

    private boolean hasLetters(String s) {
        return s != null && s.chars().anyMatch(Character::isLetter);
    }
}
