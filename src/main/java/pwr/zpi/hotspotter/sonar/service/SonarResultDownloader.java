package pwr.zpi.hotspotter.sonar.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import pwr.zpi.hotspotter.sonar.config.SonarProperties;
import pwr.zpi.hotspotter.sonar.model.repoanalysis.SonarRepoAnalysisComponent;
import pwr.zpi.hotspotter.sonar.model.repoanalysis.SonarRepoAnalysisResult;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SonarResultDownloader {
    private final static String REPO_ANALYSIS_METRICS = "bugs,vulnerabilities,code_smells,coverage,duplicated_lines_density,complexity";

    private final RestTemplate restTemplate;
    private final SonarProperties sonarProperties;


    public SonarRepoAnalysisResult fetchAnalysisResults(String repoAnalysisId, String projectKey) {
        try {
            log.info("Fetching analysis results for project: {}", projectKey);

            Map<String, Object> componentTree = fetchComponentTree(projectKey);
            if (componentTree == null) {
                log.error("Failed to fetch component tree for project: {}", projectKey);
                return null;
            }

            return mapToAnalysisResult(repoAnalysisId, projectKey, componentTree);

        } catch (Exception e) {
            log.error("Error fetching/saving analysis results for {}: {}", projectKey, e.getMessage(), e);
            return null;
        }
    }

    private Map<String, Object> fetchComponentTree(String projectKey) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + sonarProperties.getToken());
            HttpEntity<String> entity = new HttpEntity<>(headers);

            final int pageSize = 500;
            int page = 1;
            List<Map<String, Object>> allComponents = new java.util.ArrayList<>();
            Map<String, Object> baseComponent = null;
            Integer total = null;

            while (true) {
                URI uri = UriComponentsBuilder
                        .fromUriString(sonarProperties.getHostUrl())
                        .path("/api/measures/component_tree")
                        .queryParam("component", projectKey)
                        .queryParam("metricKeys", REPO_ANALYSIS_METRICS)
                        .queryParam("ps", pageSize)
                        .queryParam("p", page)
                        .build()
                        .encode()
                        .toUri();

                ResponseEntity<Map> response = restTemplate.exchange(uri, HttpMethod.GET, entity, Map.class);
                Map<String, Object> body = response.getBody();
                if (body == null) break;

                if (baseComponent == null && body.get("baseComponent") != null) {
                    baseComponent = (Map<String, Object>) body.get("baseComponent");
                }

                List<Map<String, Object>> components = (List<Map<String, Object>>) body.get("components");
                if (components != null && !components.isEmpty()) {
                    allComponents.addAll(components);
                }

                if (total == null) {
                    Map<String, Object> paging = (Map<String, Object>) body.get("paging");
                    if (paging != null && paging.get("total") != null) {
                        try {
                            total = Integer.parseInt(String.valueOf(paging.get("total")));
                        } catch (Exception ignored) { }
                    }
                }

                if (components == null || components.size() < pageSize) break;
                if (total != null && allComponents.size() >= total) break;

                page++;
            }

            Map<String, Object> combined = new java.util.HashMap<>();
            if (baseComponent != null) combined.put("baseComponent", baseComponent);
            combined.put("components", allComponents);
            if (total != null) {
                Map<String, Object> pagingMap = new java.util.HashMap<>();
                pagingMap.put("total", total);
                combined.put("paging", pagingMap);
            }

            return combined;

        } catch (Exception e) {
            log.error("Error fetching component tree: {}", e.getMessage(), e);
            return null;
        }
    }


    private SonarRepoAnalysisResult mapToAnalysisResult(String repoAnalysisId, String projectKey, Map<String, Object> data) {
        SonarRepoAnalysisResult result = new SonarRepoAnalysisResult(repoAnalysisId, projectKey);
        result.setProjectKey(projectKey);
        result.setAnalysisDate(LocalDateTime.now());

        Map<String, Object> baseComponent = (Map<String, Object>) data.get("baseComponent");
        if (baseComponent != null) {
            result.setProjectName((String) baseComponent.get("name"));
            List<Map<String, Object>> measures = (List<Map<String, Object>>) baseComponent.get("measures");
            Metrics projectMetrics = extractMetrics(measures);

            result.setBugs(projectMetrics.bugs);
            result.setVulnerabilities(projectMetrics.vulnerabilities);
            result.setCodeSmells(projectMetrics.codeSmells);
            result.setCoverage(projectMetrics.coverage);
            result.setDuplicatedLinesDensity(projectMetrics.duplicatedLinesDensity);
            result.setComplexity(projectMetrics.complexity);
        }

        List<Map<String, Object>> components = (List<Map<String, Object>>) data.get("components");
        if (components != null) {
            List<SonarRepoAnalysisComponent> componentList = components.stream()
                    .map(this::mapComponent)
                    .collect(Collectors.toList());

            result.setComponents(componentList);
        }

        return result;
    }

    private SonarRepoAnalysisComponent mapComponent(Map<String, Object> componentData) {
        SonarRepoAnalysisComponent comp = new SonarRepoAnalysisComponent();

        comp.setKey((String) componentData.get("key"));
        comp.setName((String) componentData.get("name"));
        comp.setQualifier((String) componentData.get("qualifier"));
        comp.setPath((String) componentData.get("path"));

        List<Map<String, Object>> measures = (List<Map<String, Object>>) componentData.get("measures");
        if (measures != null) {
            Metrics componentMetrics = extractMetrics(measures);
            comp.setBugs(componentMetrics.bugs);
            comp.setVulnerabilities(componentMetrics.vulnerabilities);
            comp.setCodeSmells(componentMetrics.codeSmells);
            comp.setCoverage(componentMetrics.coverage);
            comp.setDuplicatedLinesDensity(componentMetrics.duplicatedLinesDensity);
            comp.setComplexity(componentMetrics.complexity);
        }

        return comp;
    }

    private Metrics extractMetrics(List<Map<String, Object>> measures) {
        Metrics metrics = new Metrics();
        if (measures == null) return metrics;

        Map<String, String> metricsMap = measures.stream()
                .collect(Collectors.toMap(
                        m -> (String) m.get("metric"),
                        m -> (String) m.get("value"),
                        (v1, _) -> v1
                ));

        metrics.bugs = parseIntegerOrNull(metricsMap.get("bugs"));
        metrics.vulnerabilities = parseIntegerOrNull(metricsMap.get("vulnerabilities"));
        metrics.codeSmells = parseIntegerOrNull(metricsMap.get("code_smells"));
        metrics.coverage = parseDoubleOrNull(metricsMap.get("coverage"));
        metrics.duplicatedLinesDensity = parseDoubleOrNull(metricsMap.get("duplicated_lines_density"));
        metrics.complexity = parseIntegerOrNull(metricsMap.get("complexity"));

        return metrics;
    }

    private Integer parseIntegerOrNull(String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double parseDoubleOrNull(String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Data
    private static class Metrics {
        private Integer bugs;
        private Integer vulnerabilities;
        private Integer codeSmells;
        private Double coverage;
        private Double duplicatedLinesDensity;
        private Integer complexity;
    }
}
