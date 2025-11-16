package pwr.zpi.hotspotter.repositoryanalysis.analyzer.coupling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.coupling.model.CoupledFile;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.coupling.model.FileCoupling;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.coupling.repository.FileCouplingRepository;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.model.Commit;
import pwr.zpi.hotspotter.repositoryanalysis.util.AnalysisUtils;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouplingAnalyzer {

    // TODO: Temporary constants, replace with user preferences
    private static final int MIN_FILE_COMMITS = 5;
    private static final int MIN_SHARED_COMMITS = 5;
    private static final double MIN_FILE_COUPLING_PERCENTAGE = 20.0;

    private final FileCouplingRepository fileCouplingRepository;

    public CouplingAnalyzerContext startAnalysis(String analysisId, Path repositoryPath, LocalDate referenceDate) {
        log.debug("Starting coupling analysis for ID: {}", analysisId);
        return new CouplingAnalyzerContext(analysisId, repositoryPath, referenceDate);
    }

    public void processCommit(Commit commit, CouplingAnalyzerContext context) {
        if (commit == null || context == null) return;

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

        context.recordCoupling(changedFiles);
    }

    public void finishAnalysis(CouplingAnalyzerContext context) {
        if (context == null) return;

        log.info("Finishing coupling analysis for ID: {}", context.getAnalysisId());
        List<FileCoupling> fileCouplings = computeFileCouplings(context);

        try {
            AnalysisUtils.saveDataInBatches(fileCouplingRepository, fileCouplings);
        } catch (Exception e) {
            log.error("Error saving file coupling data for analysis ID {}: {}", context.getAnalysisId(), e.getMessage(), e);
        }
    }

    private List<FileCoupling> computeFileCouplings(CouplingAnalyzerContext context) {
        List<FileCoupling> result = new ArrayList<>();

        Set<String> existingFiles = AnalysisUtils.getExistingFileNames(context.getRepositoryPath());
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

}
