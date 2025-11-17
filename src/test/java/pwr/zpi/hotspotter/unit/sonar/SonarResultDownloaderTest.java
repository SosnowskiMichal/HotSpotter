package pwr.zpi.hotspotter.unit.sonar;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import pwr.zpi.hotspotter.sonar.config.SonarProperties;
import pwr.zpi.hotspotter.sonar.model.fileanalysis.SonarFileAnalysisResult;
import pwr.zpi.hotspotter.sonar.model.repoanalysis.SonarRepoAnalysisResult;
import pwr.zpi.hotspotter.sonar.service.SonarResultDownloader;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class SonarResultDownloaderTest {

    @Mock
    private RestTemplate restTemplate;
    @Mock
    private SonarProperties sonarProperties;

    @InjectMocks
    private SonarResultDownloader sonarResultDownloader;

    @Test
    void fetchAnalysisResultsReturnsValidResultsWhenDataIsAvailable() {
        String repoAnalysisId = "123";
        String projectKey = "project-key";
        Map<String, Object> componentTree = Map.of("baseComponent", Map.of("name", "Project Name", "measures", List.of(Map.of("metric", "bugs", "value", "5"))));
        List<Map<String, Object>> issues = List.of(Map.of("component", "project-key:file1", "severity", "MAJOR"));

        when(sonarProperties.getToken()).thenReturn("token");
        when(sonarProperties.getHostUrl()).thenReturn("https://sonar.example.com");
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(componentTree))
                .thenReturn(ResponseEntity.ok(Map.of("issues", issues)));

        Pair<SonarRepoAnalysisResult, SonarFileAnalysisResult> result = sonarResultDownloader.fetchAnalysisResults(repoAnalysisId, projectKey);

        assertNotNull(result);
        assertEquals("Project Name", result.getFirst().getProjectName());
        int expectedIssuesCount = SonarResultDownloader.FILE_PROBLEM_TYPES.size() * SonarResultDownloader.FILE_SEVERITIES.size();
        assertEquals(expectedIssuesCount, result.getSecond().getIssues().size());
    }

    @Test
    void fetchAnalysisResultsReturnsNullWhenComponentTreeIsNull() {
        String repoAnalysisId = "123";
        String projectKey = "project-key";

        when(sonarProperties.getToken()).thenReturn("token");
        when(sonarProperties.getHostUrl()).thenReturn("https://sonar.example.com");
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RuntimeException("API error"));

        Pair<SonarRepoAnalysisResult, SonarFileAnalysisResult> result = sonarResultDownloader.fetchAnalysisResults(repoAnalysisId, projectKey);

        assertNull(result);
    }

    @Test
    void fetchPagedStopsFetchingWhenMaxResultsLimitIsReached() {
        String apiPath = "/api/test";
        Map<String, String> queryParams = Map.of("key", "value");
        Map<String, Object> responsePage = Map.of("listKey", List.of(Map.of("id", "1")), "paging", Map.of("total", 15000));

        when(sonarProperties.getToken()).thenReturn("token");
        when(sonarProperties.getHostUrl()).thenReturn("https://sonar.example.com");
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(responsePage));

        Map<String, Object> result = sonarResultDownloader.fetchPaged(apiPath, queryParams, "listKey", "baseKey");

        assertNotNull(result);
        assertEquals(10000, result.get("total"));
    }
}
