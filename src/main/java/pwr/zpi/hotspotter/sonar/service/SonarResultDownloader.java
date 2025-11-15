package pwr.zpi.hotspotter.sonar.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import pwr.zpi.hotspotter.sonar.config.SonarProperties;
import pwr.zpi.hotspotter.sonar.model.fileanalysis.SonarFileAnalysisResult;
import pwr.zpi.hotspotter.sonar.model.fileanalysis.SonarIssue;
import pwr.zpi.hotspotter.sonar.model.fileanalysis.SonarIssueLocation;
import pwr.zpi.hotspotter.sonar.model.fileanalysis.TextRange;
import pwr.zpi.hotspotter.sonar.model.repoanalysis.SonarRepoAnalysisComponent;
import pwr.zpi.hotspotter.sonar.model.repoanalysis.SonarRepoAnalysisResult;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SonarResultDownloader {
    private final static List<String> REPO_ANALYSIS_METRICS = List.of(
            "bugs",
            "vulnerabilities",
            "code_smells",
            "coverage",
            "duplicated_lines_density",
            "complexity"
    );
    private final static List<String> FILE_PROBLEM_TYPES = List.of("BUG", "CODE_SMELL");
    private final static List<String> FILE_SEVERITIES = List.of("MAJOR", "CRITICAL");
    private final static int DEFAULT_PAGE_SIZE = 500;
    private final static int MAX_API_RESULTS = 10000;

    private final RestTemplate restTemplate;
    private final SonarProperties sonarProperties;


    public Pair<SonarRepoAnalysisResult, SonarFileAnalysisResult> fetchAnalysisResults(String repoAnalysisId, String projectKey) {
        try {
            log.info("Fetching analysis results for project: {}", projectKey);

            Map<String, Object> componentTree = fetchComponentTree(projectKey);
            if (componentTree == null) {
                log.error("Failed to fetch component tree for project: {}", projectKey);
                return null;
            }

            List<Map<String, Object>> issues = fetchIssues(projectKey);

            return Pair.of(
                    mapToAnalysisResult(repoAnalysisId, projectKey, componentTree),
                    mapIssuesToFileAnalysisResult(repoAnalysisId, projectKey, issues)
            );

        } catch (Exception e) {
            log.error("Error fetching/saving analysis results for {}: {}", projectKey, e.getMessage(), e);
            return null;
        }
    }

    private Map<String, Object> fetchPaged(String apiPath, Map<String, String> queryParams, String listKey, String baseKey) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + sonarProperties.getToken());
            HttpEntity<String> entity = new HttpEntity<>(headers);

            int page = 1;
            List<Map<String, Object>> allItems = new ArrayList<>();
            Map<String, Object> baseObject = null;
            Integer total = null;

            while (true) {
                int alreadyFetched = (page - 1) * DEFAULT_PAGE_SIZE;
                if (alreadyFetched >= MAX_API_RESULTS) {
                    log.warn("Sonar API limits to first {} results, stopping fetch for [{}].", MAX_API_RESULTS, apiPath);
                    break;
                }

                int currentPageSize = DEFAULT_PAGE_SIZE;
                if (page * DEFAULT_PAGE_SIZE > MAX_API_RESULTS) {
                    currentPageSize = MAX_API_RESULTS - alreadyFetched;
                }

                UriComponentsBuilder builder = UriComponentsBuilder
                        .fromUriString(sonarProperties.getHostUrl())
                        .path(apiPath);

                if (queryParams != null) {
                    for (Map.Entry<String, String> queryParam : queryParams.entrySet()) {
                        builder.queryParam(queryParam.getKey(), queryParam.getValue());
                    }
                }

                URI uri = builder
                        .queryParam("ps", currentPageSize)
                        .queryParam("p", page)
                        .build()
                        .encode()
                        .toUri();

                ResponseEntity<Map> response = restTemplate.exchange(uri, HttpMethod.GET, entity, Map.class);
                Map<String, Object> body = response.getBody();
                if (body == null) break;

                if (baseKey != null && baseObject == null && body.get(baseKey) != null) {
                    baseObject = (Map<String, Object>) body.get(baseKey);
                }

                List<Map<String, Object>> items = (List<Map<String, Object>>) body.get(listKey);
                if (items != null && !items.isEmpty()) {
                    allItems.addAll(items);
                }

                if (total == null) {
                    Map<String, Object> paging = (Map<String, Object>) body.get("paging");
                    if (paging != null && paging.get("total") != null) {
                        try {
                            total = Integer.parseInt(String.valueOf(paging.get("total")));
                            if (total > MAX_API_RESULTS) {
                                log.warn("Sonar reported total {} for [{}], capping to {}.", total, apiPath, MAX_API_RESULTS);
                                total = MAX_API_RESULTS;
                            }
                        } catch (Exception ignored) {}
                    }
                }

                if (items == null || items.size() < currentPageSize) break;
                if (total != null && allItems.size() >= total) break;

                page++;
            }

            Map<String, Object> combined = new HashMap<>();
            if (baseObject != null) combined.put(baseKey, baseObject);
            combined.put(listKey, allItems);
            if (total != null) combined.put("total", total);

            return combined;

        } catch (Exception e) {
            log.error("Error fetching paged data [{}]: {}", apiPath, e.getMessage(), e);
            return null;
        }
    }

    private Map<String, Object> fetchComponentTree(String projectKey) {
        Map<String, String> params = new HashMap<>();
        params.put("component", projectKey);
        params.put("metricKeys", String.join(",", REPO_ANALYSIS_METRICS));

        return fetchPaged("/api/measures/component_tree", params, "components", "baseComponent");
    }

    private List<Map<String, Object>> fetchIssues(String projectKey) {
        Map<String, String> params = new HashMap<>();
        List<Map<String, Object>> result = new ArrayList<>();
        params.put("componentKeys", projectKey);

        for (String type : FILE_PROBLEM_TYPES) {
            for (String severity : FILE_SEVERITIES) {
                params.put("types", type);
                params.put("severities", severity);
                Map<String, Object> partial = fetchPaged("/api/issues/search", params, "issues", null);

                if (partial != null) {
                    Object issuesObj = partial.get("issues");
                    if (issuesObj == null) continue;
                    List<Map<String, Object>> issues = (List<Map<String, Object>>) issuesObj;
                    result.addAll(issues);
                }
            }
        }

        return result;
    }

    private SonarRepoAnalysisResult mapToAnalysisResult(String repoAnalysisId, String projectKey, Map<String, Object> data) {
        SonarRepoAnalysisResult result = new SonarRepoAnalysisResult(projectKey);
        result.setRepoAnalysisId(repoAnalysisId);
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

    public SonarFileAnalysisResult mapIssuesToFileAnalysisResult(
            String repoAnalysisId,
            String projectKey,
            List<Map<String, Object>> issuesList) {

        SonarFileAnalysisResult result = new SonarFileAnalysisResult(projectKey);
        result.setRepoAnalysisId(repoAnalysisId);
        result.setAnalysisDate(LocalDateTime.now());

        if (issuesList == null || issuesList.isEmpty()) {
            result.setIssues(new ArrayList<>());
            return result;
        }

        List<SonarIssue> sonarIssues = new ArrayList<>();
        for (Map<String, Object> rawIssue : issuesList) {
            SonarIssue issue = new SonarIssue();

            Object component = rawIssue.get("component");
            String filePath = extractFilePath(projectKey, component);
            issue.setFilePath(filePath);

            issue.setTextRange(extractTextRange(rawIssue));
            issue.setLocations(extractProblemLocations(rawIssue, projectKey));

            issue.setSeverity(parseToStringOrNull(rawIssue.get("severity")));
            issue.setMessage(parseToStringOrNull(rawIssue.get("message")));
            issue.setType(parseToStringOrNull(rawIssue.get("type")));
            issue.setRule(parseToStringOrNull(rawIssue.get("rule")));
            issue.setEffort(parseToStringOrNull(rawIssue.get("effort")));
            issue.setDebt(parseToStringOrNull(rawIssue.get("debt")));
            issue.setAuthorEmail(parseToStringOrNull(rawIssue.get("author")));

            Object tagsObj = rawIssue.get("tags");
            if (tagsObj instanceof List) {
                issue.setTags((List<String>) tagsObj);
            }

            issue.setCreationDate(parseToLocalDateTime(rawIssue.get("creationDate")));
            issue.setUpdateDate(parseToLocalDateTime(rawIssue.get("updateDate")));

            sonarIssues.add(issue);
        }

        result.setIssues(sonarIssues);
        return result;
    }

    private static String extractFilePath(String projectKey, Object component) {
        String filePath = parseToStringOrNull(component);
        if (filePath != null && filePath.startsWith(projectKey + ":")) {
            filePath = filePath.substring(projectKey.length() + 1);
        }
        return filePath;
    }

    private TextRange extractTextRange(Map<String, Object> raw) {
        TextRange textRange = new TextRange();
        Object textRangeObj = raw.get("textRange");
        
        if (textRangeObj instanceof Map) {
            Map<String, Object> textRangeMap = (Map<String, Object>) textRangeObj;
            textRange.setStartLine(parseIntOrZero(textRangeMap.get("startLine")));
            textRange.setEndLine(parseIntOrZero(textRangeMap.get("endLine")));
            textRange.setStartOffset(parseIntOrZero(textRangeMap.get("startOffset")));
            textRange.setEndOffset(parseIntOrZero(textRangeMap.get("endOffset")));
        } else {
            Integer line = parseIntegerOrNull(String.valueOf(raw.get("line")));
            if (line != null) {
                textRange.setStartLine(line);
                textRange.setEndLine(line);
            } else {
                textRange.setStartLine(0);
                textRange.setEndLine(0);
            }
            textRange.setStartOffset(0);
            textRange.setEndOffset(0);
        }

        return textRange;
    }

    private List<SonarIssueLocation> extractProblemLocations(Map<String, Object> raw, String projectKey) {
        Object flowsObj = raw.get("flows");
        if (!(flowsObj instanceof List)) {
            return null;
        }
        List<Map<String, List<Object>>> flowsList = (List<Map<String, List<Object>>>) flowsObj;
        if (flowsList.isEmpty()) {
            return null;
        }

        List<SonarIssueLocation> problemLocations = new ArrayList<>();

        for (Map<String, List<Object>> flow : flowsList) {
            List<Object> locations = flow.get("locations");
            if (locations == null) continue;
            for (Object stepObj : locations) {
                if (stepObj instanceof Map) {
                    Map<String, Object> stepMap = (Map<String, Object>) stepObj;
                    SonarIssueLocation location = new SonarIssueLocation();

                    TextRange textRange = extractTextRange(stepMap);
                    location.setTextRange(textRange);
                    Object component = stepMap.get("component");
                    location.setFilePath(extractFilePath(projectKey, component));
                    location.setMessage(parseToStringOrNull(stepMap.get("msg")));

                    problemLocations.add(location);
                }
            }
        }

        return problemLocations;
    }

    private LocalDateTime parseToLocalDateTime(Object object) {
        if (object == null) return null;
        String string = String.valueOf(object);
        try {
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
            return OffsetDateTime.parse(string, dateTimeFormatter).toLocalDateTime();
        } catch (Exception e) {
            return null;
        }
    }

    private int parseIntOrZero(Object object) {
        if (object == null) return 0;
        try {
            return Integer.parseInt(String.valueOf(object));
        } catch (Exception e) {
            return 0;
        }
    }

    private static String parseToStringOrNull(Object object) {
        return object != null ? String.valueOf(object) : null;
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
