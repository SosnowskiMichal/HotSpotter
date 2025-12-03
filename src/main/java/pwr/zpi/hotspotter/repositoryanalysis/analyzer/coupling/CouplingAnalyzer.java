package pwr.zpi.hotspotter.repositoryanalysis.analyzer.coupling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.coupling.model.AuthorCoupling;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.coupling.model.CoupledAuthor;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.coupling.model.CoupledFile;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.coupling.model.FileCoupling;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.coupling.repository.AuthorCouplingRepository;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.coupling.repository.FileCouplingRepository;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.model.Commit;
import pwr.zpi.hotspotter.repositoryanalysis.filter.AnalysisFileFilter;
import pwr.zpi.hotspotter.common.util.AnalysisUtils;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouplingAnalyzer {

    // TODO: Temporary constants, replace with user preferences
    private static final int MIN_FILE_COMMITS = 5;
    private static final int MIN_SHARED_COMMITS = 5;
    private static final double MIN_FILE_COUPLING_PERCENTAGE = 20.0;

    private static final int MIN_AUTHOR_FILES = 3;
    private static final int MIN_AUTHOR_CHANGES = 10;
    private static final int MIN_SHARED_FILES = 3;
    private static final int MIN_SHARED_CHANGES = 5;
    private static final double MIN_AUTHOR_COUPLING_PERCENTAGE = 10.0;

    private final FileCouplingRepository fileCouplingRepository;
    private final AuthorCouplingRepository authorCouplingRepository;
    private final AnalysisFileFilter analysisFileFilter;

    public CouplingAnalyzerContext startAnalysis(String analysisId, Path repositoryPath, LocalDate referenceDate) {
        log.debug("Starting coupling analysis for ID: {}", analysisId);
        return new CouplingAnalyzerContext(analysisId, repositoryPath, referenceDate);
    }

    public void processCommit(Commit commit, CouplingAnalyzerContext context) {
        if (commit == null || context == null) return;

        String author = commit.author();
        LocalDate date = commit.getCommitDateAsLocalDate();

        List<String> changedFiles = commit.changedFiles().stream()
                .map(fileChange -> {
                    String filePath = fileChange.filePath();
                    if (fileChange.isRenamed()) {
                        context.updateFilePath(fileChange.oldPath(), fileChange.newPath());
                        filePath = fileChange.newPath();
                    }
                    return filePath;
                })
                .toList();

        context.recordCoupling(author, date, changedFiles);
    }

    public void finishAnalysis(CouplingAnalyzerContext context) {
        if (context == null) return;

        log.debug("Finishing coupling analysis for ID: {}", context.getAnalysisId());

        List<FileCoupling> fileCouplings = computeFileCouplings(context);
        try {
            AnalysisUtils.saveDataInBatches(fileCouplingRepository, fileCouplings);
            log.debug("Saved {} file coupling analysis data records for analysis ID: {}", fileCouplings.size(), context.getAnalysisId());
        } catch (Exception e) {
            log.error("Error saving file coupling data for analysis ID: {}: {}", context.getAnalysisId(), e.getMessage(), e);
        }

        List<AuthorCoupling> authorCouplings = computeAuthorCouplings(context);
        try {
            AnalysisUtils.saveDataInBatches(authorCouplingRepository, authorCouplings);
            log.debug("Saved {} author coupling analysis data records for analysis ID: {}", authorCouplings.size(), context.getAnalysisId());
        } catch (Exception e) {
            log.error("Error saving author coupling data for analysis ID: {}: {}", context.getAnalysisId(), e.getMessage(), e);
        }
    }

    private List<FileCoupling> computeFileCouplings(CouplingAnalyzerContext context) {
        List<FileCoupling> result = new ArrayList<>();

        Set<String> existingFiles = AnalysisUtils.getFilteredExistingFileNames(context.getRepositoryPath(),
                analysisFileFilter);
        Map<String, Integer> fileCommits = context.getFileCommits();
        Map<String, Map<String, Integer>> fileCouplings = context.getFileCouplings();

        for (Map.Entry<String, Map<String, Integer>> entry : fileCouplings.entrySet()) {
            String filePath = entry.getKey();
            if (!existingFiles.contains(filePath)) continue;

            int commits = fileCommits.get(filePath);
            if (commits < MIN_FILE_COMMITS) continue;

            Map<String, Integer> coupledFilesMap = entry.getValue();
            List<CoupledFile> coupledFiles = buildCoupledFilesList(coupledFilesMap, commits, existingFiles);

            if (coupledFiles.isEmpty()) continue;
            sortCoupledFiles(coupledFiles);

            FileCoupling fileCoupling = FileCoupling.builder()
                    .analysisId(context.getAnalysisId())
                    .filePath(filePath)
                    .coupledFiles(coupledFiles)
                    .build();

            result.add(fileCoupling);
        }

        return result;
    }

    private List<CoupledFile> buildCoupledFilesList(
            Map<String, Integer> coupledFilesMap,
            int commits,
            Set<String> existingFiles
    ) {
        List<CoupledFile> coupledFiles = new ArrayList<>();

        for (Map.Entry<String, Integer> coupledFileEntry : coupledFilesMap.entrySet()) {
            String coupledFilePath = coupledFileEntry.getKey();
            if (!existingFiles.contains(coupledFilePath)) continue;

            int sharedCommits = coupledFileEntry.getValue();
            if (sharedCommits < MIN_SHARED_COMMITS) continue;

            double couplingPercentage = Math.round(sharedCommits * 10000.0 / commits) / 100.0;
            if (couplingPercentage < MIN_FILE_COUPLING_PERCENTAGE) continue;

            CoupledFile coupledFile = CoupledFile.builder()
                    .filePath(coupledFilePath)
                    .sharedCommits(sharedCommits)
                    .couplingPercentage(couplingPercentage)
                    .build();

            coupledFiles.add(coupledFile);
        }

        return coupledFiles;
    }

    private void sortCoupledFiles(List<CoupledFile> coupledFiles) {
        coupledFiles.sort((a, b) -> {
            int percentageCompare = Double.compare(b.getCouplingPercentage(), a.getCouplingPercentage());
            if (percentageCompare != 0) return percentageCompare;
            return Integer.compare(b.getSharedCommits(), a.getSharedCommits());
        });
    }

    private List<AuthorCoupling> computeAuthorCouplings(CouplingAnalyzerContext context) {
        List<AuthorCoupling> result = new ArrayList<>();

        Map<String, Map<String, Integer>> allAuthorsFileChanges = context.getAuthorFileChanges();

        for (Map.Entry<String, Map<String, Integer>> entry : allAuthorsFileChanges.entrySet()) {
            String author = entry.getKey();
            Map<String, Integer> fileChanges = entry.getValue();

            int filesChanged = fileChanges.size();
            if (filesChanged < MIN_AUTHOR_FILES) continue;

            int totalChanges = fileChanges.values().stream()
                    .mapToInt(Integer::intValue)
                    .sum();
            if (totalChanges < MIN_AUTHOR_CHANGES) continue;

            List<CoupledAuthor> coupledAuthors = buildCoupledAuthorsList(
                    author,
                    fileChanges,
                    totalChanges,
                    allAuthorsFileChanges
            );

            if (coupledAuthors.isEmpty()) continue;
            sortCoupledAuthors(coupledAuthors);

            AuthorCoupling authorCoupling = AuthorCoupling.builder()
                    .analysisId(context.getAnalysisId())
                    .author(author)
                    .filesChanged(filesChanged)
                    .totalChanges(totalChanges)
                    .coupledAuthors(coupledAuthors)
                    .build();

            result.add(authorCoupling);
        }

        return result;
    }

    private List<CoupledAuthor> buildCoupledAuthorsList(
            String author,
            Map<String, Integer> changedFiles,
            int totalChanges,
            Map<String, Map<String, Integer>> allAuthorsFileChanges
    ) {
        List<CoupledAuthor> coupledAuthors = new ArrayList<>();

        for (Map.Entry<String, Map<String, Integer>> entry : allAuthorsFileChanges.entrySet()) {
            String otherAuthor = entry.getKey();
            if (otherAuthor.equals(author)) continue;
            Set<String> authorFiles = changedFiles.keySet();

            Map<String, Integer> otherFileChanges = entry.getValue();
            if (otherFileChanges.size() < MIN_AUTHOR_FILES) continue;

            int otherTotalChanges = otherFileChanges.values().stream()
                    .mapToInt(Integer::intValue)
                    .sum();
            if (otherTotalChanges < MIN_AUTHOR_CHANGES) continue;

            Set<String> sharedFiles = new HashSet<>(authorFiles);
            sharedFiles.retainAll(otherFileChanges.keySet());

            int sharedFilesCount = sharedFiles.size();
            if (sharedFilesCount < MIN_SHARED_FILES) continue;

            int sharedChanges = sharedFiles.stream()
                    .mapToInt(changedFiles::get)
                    .sum();
            if (sharedChanges < MIN_SHARED_CHANGES) continue;

            double couplingPercentage = Math.round(sharedChanges * 10000.0 / totalChanges) / 100.0;
            if (couplingPercentage < MIN_AUTHOR_COUPLING_PERCENTAGE) continue;

            CoupledAuthor coupledAuthor = CoupledAuthor.builder()
                    .author(otherAuthor)
                    .sharedFilesChanged(sharedFilesCount)
                    .sharedChanges(sharedChanges)
                    .couplingPercentage(couplingPercentage)
                    .build();

            coupledAuthors.add(coupledAuthor);
        }

        return coupledAuthors;
    }

    private void sortCoupledAuthors(List<CoupledAuthor> coupledAuthors) {
        coupledAuthors.sort((a, b) -> {
            int percentageCompare = Double.compare(b.getCouplingPercentage(), a.getCouplingPercentage());
            if (percentageCompare != 0) return percentageCompare;
            return Integer.compare(b.getSharedChanges(), a.getSharedChanges());
        });
    }

}
