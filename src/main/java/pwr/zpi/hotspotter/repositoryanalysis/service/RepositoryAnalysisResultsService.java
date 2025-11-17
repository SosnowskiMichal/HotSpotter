package pwr.zpi.hotspotter.repositoryanalysis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pwr.zpi.hotspotter.common.exceptions.ObjectNotFoundException;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.activitytrends.model.ActivityTrends;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.activitytrends.repository.ActivityTrendsRepository;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.authors.model.AuthorStatistics;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.authors.repository.AuthorStatisticsRepository;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.coupling.model.AuthorCoupling;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.coupling.model.FileCoupling;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.coupling.repository.AuthorCouplingRepository;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.coupling.repository.FileCouplingRepository;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo.model.FileInfo;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo.repository.FileInfoRepository;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge.model.FileKnowledge;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge.repository.FileKnowledgeRepository;
import pwr.zpi.hotspotter.repositoryanalysis.dto.*;
import pwr.zpi.hotspotter.repositoryanalysis.mapper.*;
import pwr.zpi.hotspotter.repositoryanalysis.repository.AnalysisInfoRepository;

import java.util.Collection;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RepositoryAnalysisResultsService {

    private final AnalysisInfoRepository analysisInfoRepository;
    private final FileInfoRepository fileInfoRepository;
    private final FileCouplingRepository fileCouplingRepository;
    private final FileKnowledgeRepository fileKnowledgeRepository;
    private final AuthorStatisticsRepository authorStatisticsRepository;
    private final AuthorCouplingRepository authorCouplingRepository;
    private final ActivityTrendsRepository activityTrendsRepository;

    private final RepositoryStructureService repositoryStructureService;

    private final FileDataMapper fileDataMapper;
    private final FileInfoMapper fileInfoMapper;
    private final FileCouplingMapper fileCouplingMapper;
    private final AuthorStatisticsMapper authorStatisticsMapper;
    private final AuthorCouplingMapper authorCouplingMapper;
    private final DailyStatsMapper dailyStatsMapper;

    public RepositoryStructureNode getRepositoryStructure(String analysisId) {
        checkIfAnalysisCompleted(analysisId);
        Collection<FileInfo> fileInfoData = fileInfoRepository.findAllByAnalysisId(analysisId);
        return repositoryStructureService.buildRepositoryStructure(fileInfoData);
    }

    public List<FilePathNameDTO> getAllFilesInRepository(String analysisId) {
        checkIfAnalysisCompleted(analysisId);
        List<FileInfo> fileInfoData = fileInfoRepository.findAllByAnalysisId(analysisId);

        return fileInfoData.stream()
                .map(fileInfoMapper::toPathNameDTO)
                .toList();
    }

    public FileDataDTO getFileData(String analysisId, String path) {
        checkIfAnalysisCompleted(analysisId);

        FileInfo fileInfo = fileInfoRepository.findByAnalysisIdAndFilePath(analysisId, path)
                .orElseThrow(() -> {
                    log.warn("File with path '{}' not found in analysis with ID '{}'.", path, analysisId);
                    return new ObjectNotFoundException("File with path '" + path + "' not found in analysis.");
                });

        FileCoupling fileCoupling = fileCouplingRepository.findByAnalysisIdAndFilePath(analysisId, path)
                .orElse(null);

        FileKnowledge fileKnowledge = fileKnowledgeRepository.findByAnalysisIdAndFilePath(analysisId, path)
                .orElse(null);

        return fileDataMapper.toDTO(fileInfo, fileCoupling, fileKnowledge);
    }

    public List<FileCouplingDTO> getAllFilesCoupling(String analysisId) {
        checkIfAnalysisCompleted(analysisId);
        List<FileCoupling> filesCouplings = fileCouplingRepository.findAllByAnalysisId(analysisId);

        return filesCouplings.stream()
                .map(fileCouplingMapper::toDTO)
                .toList();
    }

    public List<AuthorSummaryDTO> getAllAuthors(String analysisId) {
        checkIfAnalysisCompleted(analysisId);
        List<AuthorStatistics> authorStatistics = authorStatisticsRepository.findAllByAnalysisId(analysisId);

        return authorStatistics.stream()
                .map(authorStatisticsMapper::toSummaryDTO)
                .toList();
    }

    public List<AuthorStatisticsDTO> getAllAuthorsStatistics(String analysisId) {
        checkIfAnalysisCompleted(analysisId);
        List<AuthorStatistics> authorStatistics = authorStatisticsRepository.findAllByAnalysisId(analysisId);

        return authorStatistics.stream()
                .map(authorStatisticsMapper::toDTO)
                .toList();
    }

    public AuthorStatisticsDTO getAuthorStatistics(String analysisId, String name) {
        checkIfAnalysisCompleted(analysisId);

        AuthorStatistics authorStatistics = authorStatisticsRepository.findByAnalysisIdAndName(analysisId, name)
                .orElseThrow(() -> {
                    log.warn("Author with name '{}' not found in analysis with ID '{}'.", name, analysisId);
                    return new ObjectNotFoundException("Author with name '" + name + "' not found in analysis.");
                });

        return authorStatisticsMapper.toDTO(authorStatistics);
    }

    public List<AuthorCouplingDTO> getAllAuthorsCouplings(String analysisId) {
        checkIfAnalysisCompleted(analysisId);
        List<AuthorCoupling> authorsCouplings = authorCouplingRepository.findAllByAnalysisId(analysisId);

        return authorsCouplings.stream()
                .map(authorCouplingMapper::toDTO)
                .toList();
    }

    public List<DailyStatsDTO> getActivityTrends(String analysisId) {
        checkIfAnalysisCompleted(analysisId);

        ActivityTrends activityTrends = activityTrendsRepository.findById(analysisId)
                .orElseThrow(() -> {
                    log.warn("Activity trends not found for analysis with ID '{}'.", analysisId);
                    return new ObjectNotFoundException("Activity trends not found for analysis.");
                });

        return activityTrends.getDailyStats().stream()
                .map(dailyStatsMapper::toDTO)
                .toList();
    }

    private void checkIfAnalysisCompleted(String analysisId) {
        if (!analysisInfoRepository.existsById(analysisId)) {
            log.warn("Analysis with ID '{}' does not exist.", analysisId);
            throw new ObjectNotFoundException("Analysis with ID '" + analysisId + "' does not exist.");
        }

        if (!analysisInfoRepository.isAnalysisCompleted(analysisId)) {
            log.warn("Analysis with ID '{}' is not completed.", analysisId);
            throw new IllegalStateException("Analysis with ID '" + analysisId + "' is not completed.");
        }
    }

}
