package pwr.zpi.hotspotter.repositoryanalysis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo.model.FileInfo;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo.repository.FileInfoRepository;
import pwr.zpi.hotspotter.repositoryanalysis.model.RepositoryStructureNode;
import pwr.zpi.hotspotter.repositoryanalysis.repository.AnalysisInfoRepository;

import java.util.Collection;

@Slf4j
@Service
@RequiredArgsConstructor
public class RepositoryAnalysisResultsService {

    private final AnalysisInfoRepository analysisInfoRepository;
    private final FileInfoRepository fileInfoRepository;
    private final RepositoryStructureService repositoryStructureService;

    public RepositoryStructureNode getRepositoryStructure(String analysisId) {
        checkIfAnalysisCompleted(analysisId);
        Collection<FileInfo> fileInfoData = fileInfoRepository.findAllByAnalysisId(analysisId);
        return repositoryStructureService.buildRepositoryStructure(fileInfoData);
    }

    private void checkIfAnalysisCompleted(String analysisId) {
        if (!analysisInfoRepository.isAnalysisCompleted(analysisId)) {
            log.warn("Analysis with ID {} does not exist or is not completed.", analysisId);
            throw new IllegalArgumentException("Analysis with given ID does not exist or is not completed.");
        }
    }

}
























