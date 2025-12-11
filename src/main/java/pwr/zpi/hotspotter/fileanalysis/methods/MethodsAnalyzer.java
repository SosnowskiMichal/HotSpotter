package pwr.zpi.hotspotter.fileanalysis.methods;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.fileanalysis.complexity.model.FileComplexityReport;
import pwr.zpi.hotspotter.fileanalysis.complexity.model.MethodComplexity;
import pwr.zpi.hotspotter.fileanalysis.logprocessing.model.FileCommit;
import pwr.zpi.hotspotter.fileanalysis.methods.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class MethodsAnalyzer {

    private final GitDiffParser gitDiffParser;

    public List<MethodStatistics> analyzeMethods(
            Path repositoryPath,
            List<FileCommit> fileCommits,
            Map<String, FileComplexityReport> complexityResults,
            LocalDate endDate
    ) {
        if (fileCommits == null || fileCommits.isEmpty()) {
            return Collections.emptyList();
        }
        if (complexityResults == null || complexityResults.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Map<String, MethodComplexity>> commitMethods = extractMethodsFromComplexity(
                fileCommits, complexityResults
        );

        if (commitMethods.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, MethodTracker> methodTrackers = trackMethodsAcrossCommits(
                repositoryPath,
                fileCommits,
                commitMethods
        );

        return buildMethodStatistics(methodTrackers, endDate);
    }

    private Map<String, Map<String, MethodComplexity>> extractMethodsFromComplexity(
            List<FileCommit> fileCommits,
            Map<String, FileComplexityReport> complexityResults
    ) {
        Map<String, Map<String, MethodComplexity>> commitMethods = new LinkedHashMap<>();

        for (FileCommit commit : fileCommits) {
            String key = commit.hash();
            FileComplexityReport report = complexityResults.get(key);

            if (report != null && report.getMethods() != null && !report.getMethods().isEmpty()) {
                Map<String, MethodComplexity> methodsMap = new HashMap<>();

                for (MethodComplexity method : report.getMethods()) {
                    String signature = generateMethodSignature(method);
                    methodsMap.put(signature, method);
                }

                commitMethods.put(commit.hash(), methodsMap);
            }
        }

        return commitMethods;
    }

    private String generateMethodSignature(MethodComplexity method) {
        return method.getName() + ":" + method.getParameters();
    }

    private Map<String, MethodTracker> trackMethodsAcrossCommits(
            Path repositoryPath,
            List<FileCommit> fileCommits,
            Map<String, Map<String, MethodComplexity>> commitMethods
    ) {
        Map<String, MethodTracker> methodTrackers = new HashMap<>();

        for (int i = 0; i < fileCommits.size(); i++) {
            FileCommit currentCommit = fileCommits.get(i);

            if (!commitMethods.containsKey(currentCommit.hash())) {
                continue;
            }

            if (i == 0) {
                handleFirstCommit(currentCommit, commitMethods, methodTrackers);
            } else {
                FileCommit previousCommit = fileCommits.get(i - 1);
                trackCommitPair(
                        repositoryPath,
                        previousCommit,
                        currentCommit,
                        commitMethods,
                        methodTrackers
                );
            }
        }

        return methodTrackers;
    }

    private void handleFirstCommit(
            FileCommit commit,
            Map<String, Map<String, MethodComplexity>> commitMethods,
            Map<String, MethodTracker> methodTrackers
    ) {
        Map<String, MethodComplexity> methods = commitMethods.get(commit.hash());

        if (methods == null) return;

        for (Map.Entry<String, MethodComplexity> entry : methods.entrySet()) {
            String signature = entry.getKey();
            MethodComplexity method = entry.getValue();

            MethodTracker tracker = createMethodTracker(commit, method);
            methodTrackers.put(signature, tracker);
        }
    }

    private MethodTracker createMethodTracker(FileCommit commit, MethodComplexity method) {
        MethodTracker tracker = new MethodTracker();
        MethodVersion version = new MethodVersion(
                commit.hash(),
                commit.date(),
                method.getName(),
                method.getStartLine(),
                method.getEndLine(),
                method.getComplexity(),
                true
        );

        tracker.addVersion(version);
        tracker.addAuthor(commit.author());
        return tracker;
    }

    private void trackCommitPair(
            Path repositoryPath,
            FileCommit previousCommit,
            FileCommit currentCommit,
            Map<String, Map<String, MethodComplexity>> commitMethods,
            Map<String, MethodTracker> methodTrackers
    ) {
        Map<String, MethodComplexity> previousMethods = commitMethods.get(previousCommit.hash());
        Map<String, MethodComplexity> currentMethods = commitMethods.get(currentCommit.hash());

        if (currentMethods == null) return;

        List<LineRange> changedRanges = getChangedLineRanges(
                repositoryPath,
                previousCommit.hash(),
                currentCommit.hash(),
                currentCommit.path()
        );

        for (Map.Entry<String, MethodComplexity> entry : currentMethods.entrySet()) {
            String currentSignature = entry.getKey();
            MethodComplexity currentMethod = entry.getValue();

            String matchedSignature = findMatchingMethod(
                    currentMethod,
                    previousMethods,
                    currentSignature
            );

            boolean wasTouched = wasMethodTouched(
                    changedRanges,
                    currentMethod.getStartLine(),
                    currentMethod.getEndLine()
            );

            MethodVersion version = new MethodVersion(
                    currentCommit.hash(),
                    currentCommit.date(),
                    currentMethod.getName(),
                    currentMethod.getStartLine(),
                    currentMethod.getEndLine(),
                    currentMethod.getComplexity(),
                    wasTouched
            );

            if (matchedSignature != null && methodTrackers.containsKey(matchedSignature)) {
                MethodTracker tracker = methodTrackers.get(matchedSignature);
                tracker.addVersion(version);
                if (wasTouched) {
                    tracker.addAuthor(currentCommit.author());
                }

                if (!matchedSignature.equals(currentSignature)) {
                    methodTrackers.remove(matchedSignature);
                    methodTrackers.put(currentSignature, tracker);
                }
            } else {
                MethodTracker tracker = new MethodTracker();
                tracker.addVersion(version);
                if (wasTouched) {
                    tracker.addAuthor(currentCommit.author());
                }
                methodTrackers.put(currentSignature, tracker);
            }
        }
    }

    private String findMatchingMethod(
            MethodComplexity currentMethod,
            Map<String, MethodComplexity> previousMethods,
            String currentSignature
    ) {
        if (previousMethods == null) return null;

        if (previousMethods.containsKey(currentSignature)) {
            return currentSignature;
        }

        for (Map.Entry<String, MethodComplexity> entry : previousMethods.entrySet()) {
            MethodComplexity previousMethod = entry.getValue();

            boolean similarLineRange = Math.abs(previousMethod.getStartLine() - currentMethod.getStartLine()) <= 5
                    && Math.abs(previousMethod.getEndLine() - currentMethod.getEndLine()) <= 5;

            boolean similarComplexity = Math.abs(previousMethod.getComplexity() - currentMethod.getComplexity()) <= 2;

            if (similarLineRange && similarComplexity) {
                return entry.getKey();
            }
        }

        return null;
    }

    private boolean wasMethodTouched(
            List<LineRange> changedRanges,
            int methodStartLine,
            int methodEndLine
    ) {
        for (LineRange range : changedRanges) {
            if (rangesOverlap(range, methodStartLine, methodEndLine)) {
                return true;
            }
        }
        return false;
    }

    private boolean rangesOverlap(LineRange range, int methodStart, int methodEnd) {
        return !(range.endLine() < methodStart || range.startLine() > methodEnd);
    }

    private List<LineRange> getChangedLineRanges(
            Path repositoryPath,
            String parentHash,
            String childHash,
            String filePath
    ) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "git", "diff",
                    parentHash, childHash,
                    "--unified=0",
                    "--", filePath
            );
            pb.directory(repositoryPath.toFile());
            pb.redirectErrorStream(true);

            Process process = pb.start();
            InputStream inputStream = process.getInputStream();

            List<LineRange> ranges = gitDiffParser.parseChangedLineRanges(inputStream);

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.warn("Git diff exited with code {} for commits {} -> {}", exitCode, parentHash, childHash);
                return Collections.emptyList();
            }

            return ranges;

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            log.error("Failed to run git diff: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<MethodStatistics> buildMethodStatistics(
            Map<String, MethodTracker> methodTrackers,
            LocalDate referenceDate
    ) {
        List<MethodStatistics> methodsStatistics = new ArrayList<>();

        for (MethodTracker tracker : methodTrackers.values()) {
            List<MethodVersion> touchedVersions = tracker.getVersions().stream()
                    .filter(MethodVersion::wasTouched)
                    .toList();

            if (touchedVersions.isEmpty()) {
                continue;
            }

            List<LocalDate> touchDates = touchedVersions.stream()
                    .map(MethodVersion::commitDate)
                    .distinct()
                    .sorted()
                    .toList();

            LocalDate firstDate = touchDates.getFirst();
            LocalDate lastDate = touchDates.getLast();

            int daysSinceLastCommit = (int) ChronoUnit.DAYS.between(lastDate, referenceDate);
            daysSinceLastCommit = Math.max(daysSinceLastCommit, 0);

            List<MethodVersionStatistics> trends = tracker.getVersions().stream()
                    .filter(version -> version.complexity() != null)
                    .map(version -> {
                        Integer lines = (version.startLine() != null && version.endLine() != null)
                                ? version.endLine() - version.startLine() + 1
                                : null;
                        return new MethodVersionStatistics(version.commitDate(), version.complexity(), lines);
                    })
                    .toList();

            MethodStatistics stats = MethodStatistics.builder()
                    .name(tracker.getCurrentName())
                    .startLine(tracker.getCurrentStartLine())
                    .endLine(tracker.getCurrentEndLine())
                    .lines(tracker.getCurrentEndLine() - tracker.getCurrentStartLine() + 1)
                    .url(null)
                    .commits(touchedVersions.size())
                    .authors(tracker.getAuthors().size())
                    .authorNames(tracker.getAuthors())
                    .firstCommitDate(firstDate)
                    .lastCommitDate(lastDate)
                    .daysSinceLastCommit(daysSinceLastCommit)
                    .complexityTrends(trends)
                    .build();

            methodsStatistics.add(stats);
        }

        return methodsStatistics;
    }

}
