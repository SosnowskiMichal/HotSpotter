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
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.statistics.model.AnalysisStatistics;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.statistics.repository.AnalysisStatisticsRepository;
import pwr.zpi.hotspotter.repositoryanalysis.dto.*;
import pwr.zpi.hotspotter.repositoryanalysis.mapper.*;
import pwr.zpi.hotspotter.repositoryanalysis.model.AnalysisInfo;
import pwr.zpi.hotspotter.repositoryanalysis.repository.AnalysisInfoRepository;
import pwr.zpi.hotspotter.sonar.model.repoanalysis.SonarRepoAnalysisComponent;
import pwr.zpi.hotspotter.sonar.model.repoanalysis.SonarRepoAnalysisResult;
import pwr.zpi.hotspotter.sonar.repository.SonarRepoAnalysisComponentRepository;
import pwr.zpi.hotspotter.sonar.repository.SonarRepoAnalysisRepository;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RepositoryAnalysisResultsService {

    private final AnalysisInfoRepository analysisInfoRepository;
    private final AnalysisStatisticsRepository analysisStatisticsRepository;
    private final FileInfoRepository fileInfoRepository;
    private final FileCouplingRepository fileCouplingRepository;
    private final FileKnowledgeRepository fileKnowledgeRepository;
    private final AuthorStatisticsRepository authorStatisticsRepository;
    private final AuthorCouplingRepository authorCouplingRepository;
    private final ActivityTrendsRepository activityTrendsRepository;
    private final SonarRepoAnalysisRepository sonarAnalysisRepository;
    private final SonarRepoAnalysisComponentRepository sonarAnalysisComponentRepository;

    private final RepositoryStructureService repositoryStructureService;

    private final AnalysisSummaryMapper analysisSummaryMapper;
    private final FileDataMapper fileDataMapper;
    private final FileInfoMapper fileInfoMapper;
    private final FileCouplingMapper fileCouplingMapper;
    private final FileKnowledgeMapper fileKnowledgeMapper;
    private final AuthorStatisticsMapper authorStatisticsMapper;
    private final AuthorCouplingMapper authorCouplingMapper;
    private final DailyStatsMapper dailyStatsMapper;

    public AnalysisSummaryDTO getAnalysisSummary(String analysisId) {
        checkIfAnalysisCompleted(analysisId);

        AnalysisInfo analysisInfo = analysisInfoRepository.findById(analysisId)
                .orElse(null);
        AnalysisStatistics analysisStatistics = analysisStatisticsRepository.findById(analysisId)
                .orElse(null);
        SonarRepoAnalysisResult sonarAnalysisResult = sonarAnalysisRepository.findByRepoAnalysisId(analysisId)
                .orElse(null);

        return analysisSummaryMapper.toDTO(analysisInfo, analysisStatistics, sonarAnalysisResult);
    }

    public RepositoryStructureNode getRepositoryStructure(String analysisId) {
        checkIfAnalysisCompleted(analysisId);
        List<FileInfo> fileInfoData = fileInfoRepository.findAllByAnalysisId(analysisId);
        return repositoryStructureService.buildRepositoryStructure(fileInfoData);
    }

    public List<FilePathNameDTO> getAllFilesInRepository(String analysisId) {
        checkIfAnalysisCompleted(analysisId);
        var projections = fileInfoRepository.findAllPathNamesByAnalysisId(analysisId);

        return projections.stream()
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
        SonarRepoAnalysisComponent sonarAnalysisComponent = sonarAnalysisComponentRepository
                .findByRepoAnalysisIdAndPath(analysisId, path)
                .orElse(null);

        return fileDataMapper.toDTO(fileInfo, fileCoupling, fileKnowledge, sonarAnalysisComponent);
    }

    public List<FileTypeDTO> getAllFilesTypes(String analysisId) {
        checkIfAnalysisCompleted(analysisId);
        var projections = fileInfoRepository.findAllTypesByAnalysisId(analysisId);

        return projections.stream()
                .map(fileInfoMapper::toTypeDTO)
                .toList();
    }

    public List<FileCouplingDTO> getAllFilesCoupling(String analysisId) {
        checkIfAnalysisCompleted(analysisId);
        List<FileCoupling> filesCouplings = fileCouplingRepository.findAllByAnalysisId(analysisId);

        return filesCouplings.stream()
                .map(fileCouplingMapper::toDTO)
                .toList();
    }

    public List<FileCodeAgeDTO> getAllFilesCodeAge(String analysisId) {
        checkIfAnalysisCompleted(analysisId);
        var projections = fileInfoRepository.findAllCodeAgesByAnalysisId(analysisId);

        int maxCodeAge = projections.stream()
                .map(FileInfoRepository.FileCodeAgeProjection::getCodeAgeDays)
                .max(Integer::compareTo)
                .orElse(1);

        return projections.stream()
                .map(projection -> {
                    double normalizedValue = Math.round(100.0 - projection.getCodeAgeDays() * 100.0 / maxCodeAge) / 100.0;
                    return fileInfoMapper.toCodeAgeDTO(projection, normalizedValue);
                })
                .filter(dto -> dto.normalizedValue() != 0.0)
                .toList();
    }

    public List<FileKnowledgeLossRiskDTO> getAllFilesKnowledgeLossRisk(String analysisId) {
        checkIfAnalysisCompleted(analysisId);
        List<FileKnowledge> fileKnowledgeData = fileKnowledgeRepository.findAllByAnalysisId(analysisId);

        return fileKnowledgeData.stream()
                .map(fileKnowledge -> {
                    double normalizedValue = Math.round(fileKnowledge.getKnowledgeLoss()) / 100.0;
                    return fileKnowledgeMapper.toKnowledgeLossRiskDTO(fileKnowledge, normalizedValue);
                })
                .toList();
    }

    public List<FileLeadAuthorDTO> getAllFilesLeadAuthors(String analysisId) {
        checkIfAnalysisCompleted(analysisId);
        List<FileKnowledge> fileKnowledgeData = fileKnowledgeRepository.findAllByAnalysisId(analysisId);

        return fileKnowledgeData.stream()
                .map(fileKnowledgeMapper::toLeadAuthorDTO)
                .toList();
    }

    public List<HotspotDTO> getHotspots(String analysisId) {
        checkIfAnalysisCompleted(analysisId);
        var projections = fileInfoRepository.findAllHotspotsByAnalysisId(analysisId);

        int maxCommits = 1;
        int maxCodeLines = 1;
        List<FileInfoRepository.HotspotProjection> codeFiles = new ArrayList<>();

        for (var projection : projections) {
            if (projection.getCodeLines() != null) {
                codeFiles.add(projection);
                maxCommits = Math.max(maxCommits, projection.getCommitsInHotspotAnalysisPeriod());
                maxCodeLines = Math.max(maxCodeLines, projection.getCodeLines());
            }
        }

        if (codeFiles.isEmpty()) {
            return List.of();
        }

        final double invMaxCommits = 1.0 / maxCommits;
        final double invMaxCodeLines = 1.0 / maxCodeLines;

        double maxHotspotScore = codeFiles.stream()
                .mapToDouble(projection -> {
                    double normalizedCommits = projection.getCommitsInHotspotAnalysisPeriod() * invMaxCommits;
                    double normalizedCodeLines = projection.getCodeLines() * invMaxCodeLines;
                    return calculateHotSpotScore(normalizedCommits, normalizedCodeLines);
                })
                .max()
                .orElse(1.0);

        final double invMaxHotspotScore = 1.0 / maxHotspotScore;

        return codeFiles.stream()
                .map(projection -> {
                    double normalizedCommits = projection.getCommitsInHotspotAnalysisPeriod() * invMaxCommits;
                    double normalizedCodeLines = projection.getCodeLines() * invMaxCodeLines;
                    double hotspotScore = calculateHotSpotScore(normalizedCommits, normalizedCodeLines);
                    double normalizedValue = Math.round(hotspotScore * invMaxHotspotScore * 100.0) / 100.0;

                    return fileInfoMapper.toHotspotDTO(projection, normalizedValue);
                })
                .filter(dto -> dto.normalizedValue() != 0.0)
                .toList();
    }

    private double calculateHotSpotScore(double normalizedCommits, double normalizedCodeLines) {
        return Math.pow(normalizedCommits, 0.65) * Math.pow(normalizedCodeLines, 0.35);
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
