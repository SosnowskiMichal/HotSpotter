package pwr.zpi.hotspotter.unit.sonar.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pwr.zpi.hotspotter.sonar.translation.TranslatorService;
import pwr.zpi.hotspotter.sonar.model.fileanalysis.SonarIssue;
import pwr.zpi.hotspotter.sonar.model.fileanalysis.SonarIssueLocation;
import pwr.zpi.hotspotter.sonar.repository.SonarIssueRepository;
import pwr.zpi.hotspotter.sonar.translation.SonarIssueTranslatorService;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SonarIssueTranslatorServiceTest {

    @Mock
    private TranslatorService translatorService;
    @Mock
    private SonarIssueRepository sonarIssueRepository;

    @InjectMocks
    private SonarIssueTranslatorService service;

    private SonarIssue issue;
    private SonarIssueLocation location;

    @BeforeEach
    void setup() {
        issue = new SonarIssue();
        issue.setMessage("Test message");
        issue.setMessageTranslations(new HashMap<>());

        location = new SonarIssueLocation();
        location.setMessage("Location message");
        location.setMessageTranslations(new HashMap<>());

        issue.setLocations(List.of(location));
    }

    @Test
    void doesNothing_WhenTargetLanguageIsNull() {
        service.translateIssueMessage(issue, null);

        verifyNoInteractions(translatorService);
        verifyNoInteractions(sonarIssueRepository);
    }

    @Test
    void doesNothing_WhenTargetLanguageIsEnglish() {
        service.translateIssueMessage(issue, "en");

        verifyNoInteractions(translatorService);
        verifyNoInteractions(sonarIssueRepository);
    }

    @Test
    void doesNothing_WhenIssueIsNull() {
        service.translateIssueMessage(null, "pl");

        verifyNoInteractions(translatorService);
        verifyNoInteractions(sonarIssueRepository);
    }

    @Test
    void doesNothing_WhenTranslationAlreadyExists() {
        issue.getMessageTranslations().put("pl", "już jest");

        service.translateIssueMessage(issue, "pl");

        verifyNoInteractions(translatorService);
        verifyNoInteractions(sonarIssueRepository);
    }

    @Test
    void translatesMessageAndLocations_WhenValid() throws Exception {
        when(translatorService.translate("Test message", "en", "pl")).thenReturn("Test msg PL");
        when(translatorService.translate("Location message", "en", "pl")).thenReturn("Loc msg PL");

        service.translateIssueMessage(issue, "pl");

        assertEquals("Test msg PL", issue.getMessageTranslations().get("pl"));
        assertEquals("Loc msg PL", location.getMessageTranslations().get("pl"));

        verify(sonarIssueRepository).save(issue);
    }

    @Test
    void skipsTranslation_WhenMessageHasNoLetters() {
        issue.setMessage("12345");
        issue.setLocations(Collections.emptyList());

        service.translateIssueMessage(issue, "pl");

        verifyNoInteractions(translatorService);
    }

    @Test
    void skipsTranslation_WhenLocationHasNoLetters() throws Exception {
        location.setMessage("!!@@??");

        when(translatorService.translate("Test message", "en", "pl")).thenReturn("Translated");

        service.translateIssueMessage(issue, "pl");

        assertEquals("Translated", issue.getMessageTranslations().get("pl"));
        assertTrue(location.getMessageTranslations().isEmpty());

        verify(sonarIssueRepository).save(issue);
    }

    @Test
    void continues_WhenMessageTranslationThrowsIOException() throws Exception {
        when(translatorService.translate("Test message", "en", "pl"))
                .thenThrow(new IOException("fail"));

        when(translatorService.translate("Location message", "en", "pl"))
                .thenReturn("LOC");

        service.translateIssueMessage(issue, "pl");

        assertNull(issue.getMessageTranslations().get("pl"));
        assertEquals("LOC", location.getMessageTranslations().get("pl"));

        verify(sonarIssueRepository).save(issue);
    }

    @Test
    void logsError_WhenLocationTranslationThrowsIOException() throws Exception {
        when(translatorService.translate("Test message", "en", "pl"))
                .thenReturn("MSG_PL");

        when(translatorService.translate("Location message", "en", "pl"))
                .thenThrow(new IOException("fail"));

        service.translateIssueMessage(issue, "pl");

        assertEquals("MSG_PL", issue.getMessageTranslations().get("pl"));
        assertTrue(location.getMessageTranslations().isEmpty());

        verify(sonarIssueRepository).save(issue);
    }

    @Test
    void doesNotSave_WhenBothTranslationsFail() throws Exception {
        when(translatorService.translate(any(), any(), any()))
                .thenThrow(new IOException("fail"));

        service.translateIssueMessage(issue, "pl");

        verifyNoInteractions(sonarIssueRepository);
    }

    @Test
    void doesNothing_WhenIterableIsEmpty() {
        service.translateIssueMessagesBulk(Collections.emptyList(), "pl");

        verifyNoInteractions(translatorService);
        verifyNoInteractions(sonarIssueRepository);
    }

    @Test
    void doesNothing_WhenTargetLanguageIsNull_InBulk() {
        service.translateIssueMessagesBulk(List.of(issue), null);

        verifyNoInteractions(translatorService);
        verifyNoInteractions(sonarIssueRepository);
    }

    @Test
    void translatesMultipleIssues_InBulk() throws Exception {
        SonarIssue issue2 = new SonarIssue();
        issue2.setMessage("Second message");
        issue2.setMessageTranslations(new HashMap<>());
        issue2.setLocations(Collections.emptyList());

        when(translatorService.translate("Test message", "en", "pl")).thenReturn("Test msg PL");
        when(translatorService.translate("Second message", "en", "pl")).thenReturn("Second msg PL");
        when(translatorService.translate("Location message", "en", "pl")).thenReturn("Loc msg PL");

        service.translateIssueMessagesBulk(List.of(issue, issue2), "pl");

        assertEquals("Test msg PL", issue.getMessageTranslations().get("pl"));
        assertEquals("Second msg PL", issue2.getMessageTranslations().get("pl"));

        verify(sonarIssueRepository).save(issue);
        verify(sonarIssueRepository).save(issue2);
    }
}
