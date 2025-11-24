package pwr.zpi.hotspotter.unit.sonar.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pwr.zpi.hotspotter.sonar.dto.SonarIssueDTO;
import pwr.zpi.hotspotter.sonar.mapper.SonarIssueMapper;
import pwr.zpi.hotspotter.sonar.model.fileanalysis.SonarIssue;
import pwr.zpi.hotspotter.sonar.model.fileanalysis.SonarIssueLocation;
import pwr.zpi.hotspotter.sonar.model.fileanalysis.TextRange;
import pwr.zpi.hotspotter.sonar.service.SonarIssueTranslatorService;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SonarIssueMapperTest {

    @Mock
    SonarIssueTranslatorService translatorService;

    @InjectMocks
    SonarIssueMapper mapper;

    SonarIssue issue;
    SonarIssueLocation location;

    @BeforeEach
    void setup() {
        location = new SonarIssueLocation();
        location.setMessage("loc original");
        location.setMessageTranslations(new HashMap<>());
        location.setTextRange(new TextRange(1, 1, 1, 1));

        issue = new SonarIssue();
        issue.setMessage("original");
        issue.setMessageTranslations(new HashMap<>());
        issue.setTextRange(new TextRange(1, 1, 1, 1));
        issue.setLocations(List.of(location));
    }

    @Test
    void toDTO_ReturnsNull_WhenIssueIsNull() {
        assertNull(mapper.toDTO(null, "pl"));
        verifyNoInteractions(translatorService);
    }

    @Test
    void toDTO_DoesNotTranslate_WhenTargetLanguageIsNull() {
        SonarIssueDTO dto = mapper.toDTO(issue, null);

        verifyNoInteractions(translatorService);
        assertEquals("original", dto.message());
        assertEquals("loc original", dto.locations().getFirst().message());
    }

    @Test
    void toDTO_UsesTranslatedMessage_WhenTranslationExists() {
        issue.getMessageTranslations().put("pl", "translated");

        SonarIssueDTO dto = mapper.toDTO(issue, "pl");

        assertEquals("translated", dto.message());
    }

    @Test
    void toDTO_UsesOriginalMessage_WhenTranslationMissing() {
        SonarIssueDTO dto = mapper.toDTO(issue, "pl");

        assertEquals("original", dto.message());
    }

    @Test
    void toDTO_UsesTranslatedLocationMessage_WhenTranslationExists() {
        location.getMessageTranslations().put("pl", "loc translated");

        SonarIssueDTO dto = mapper.toDTO(issue, "pl");

        assertEquals("loc translated", dto.locations().getFirst().message());
    }

    @Test
    void toDTO_UsesOriginalLocationMessage_WhenTranslationMissing() {
        SonarIssueDTO dto = mapper.toDTO(issue, "pl");

        assertEquals("loc original", dto.locations().getFirst().message());
    }

    @Test
    void toDTO_MapsAllFieldsCorrectly() {
        issue.setPath("path");
        issue.setRule("rule");
        issue.setAuthorEmail("a@b.c");
        issue.setDebt("5min");
        issue.setEffort("2min");
        issue.setSeverity("MAJOR");
        issue.setTags(List.of("tag1"));
        issue.setType("BUG");

        location.setTextRange(new TextRange(1, 5, 1, 10));
        issue.setTextRange(new TextRange(10, 20, 2, 30));

        SonarIssueDTO dto = mapper.toDTO(issue, null);

        assertEquals("path", dto.path());
        assertEquals("rule", dto.rule());
        assertEquals("a@b.c", dto.authorEmail());
        assertEquals("5min", dto.debt());
        assertEquals("2min", dto.effort());
        assertEquals("MAJOR", dto.severity());
        assertEquals(List.of("tag1"), dto.tags());
        assertEquals("BUG", dto.type());
        assertEquals(10, dto.startLine());
        assertEquals(20, dto.endLine());
        assertEquals(1, dto.locations().getFirst().startLine());
        assertEquals(5, dto.locations().getFirst().endLine());
    }
}
