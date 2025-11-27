package pwr.zpi.hotspotter.unit.repositoryanalysis.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pwr.zpi.hotspotter.common.exception.ObjectNotFoundException;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.activitytrends.model.ActivityTrends;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.activitytrends.repository.ActivityTrendsRepository;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.authors.model.AuthorStatistics;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.authors.repository.AuthorStatisticsRepository;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.coupling.repository.AuthorCouplingRepository;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.coupling.repository.FileCouplingRepository;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo.model.FileInfo;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo.repository.FileInfoRepository;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge.model.FileKnowledge;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge.model.KnowledgeRisk;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge.repository.FileKnowledgeRepository;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.statistics.model.AnalysisStatistics;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.statistics.repository.AnalysisStatisticsRepository;
import pwr.zpi.hotspotter.repositoryanalysis.dto.*;
import pwr.zpi.hotspotter.repositoryanalysis.mapper.*;
import pwr.zpi.hotspotter.repositoryanalysis.model.AnalysisInfo;
import pwr.zpi.hotspotter.repositoryanalysis.repository.AnalysisInfoRepository;
import pwr.zpi.hotspotter.repositoryanalysis.service.RepositoryAnalysisResultsService;
import pwr.zpi.hotspotter.repositoryanalysis.service.RepositoryStructureService;
import pwr.zpi.hotspotter.sonar.model.repoanalysis.SonarRepoAnalysisComponent;
import pwr.zpi.hotspotter.sonar.model.repoanalysis.SonarRepoAnalysisResult;
import pwr.zpi.hotspotter.sonar.repository.SonarRepoAnalysisComponentRepository;
import pwr.zpi.hotspotter.sonar.repository.SonarRepoAnalysisRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepositoryAnalysisResultsServiceTest {

    @Mock
    private AnalysisInfoRepository analysisInfoRepository;
    @Mock
    private AnalysisStatisticsRepository analysisStatisticsRepository;
    @Mock
    private FileInfoRepository fileInfoRepository;
    @Mock
    private FileCouplingRepository fileCouplingRepository;
    @Mock
    private FileKnowledgeRepository fileKnowledgeRepository;
    @Mock
    private AuthorStatisticsRepository authorStatisticsRepository;
    @Mock
    private AuthorCouplingRepository authorCouplingRepository;
    @Mock
    private ActivityTrendsRepository activityTrendsRepository;
    @Mock
    private SonarRepoAnalysisRepository sonarAnalysisRepository;
    @Mock
    private SonarRepoAnalysisComponentRepository sonarAnalysisComponentRepository;
    @Mock
    private RepositoryStructureService repositoryStructureService;

    @Mock
    private AnalysisSummaryMapper analysisSummaryMapper;
    @Mock
    private FileDataMapper fileDataMapper;
    @Mock
    private FileInfoMapper fileInfoMapper;
    @Mock
    private FileCouplingMapper fileCouplingMapper;
    @Mock
    private FileKnowledgeMapper fileKnowledgeMapper;
    @Mock
    private AuthorStatisticsMapper authorStatisticsMapper;
    @Mock
    private AuthorCouplingMapper authorCouplingMapper;
    @Mock
    private DailyStatsMapper dailyStatsMapper;

    @InjectMocks
    private RepositoryAnalysisResultsService service;

    private final String ANALYSIS_ID = "A1";

    @Nested
    class WithSetup {

        @BeforeEach
        void setup() {
            when(analysisInfoRepository.existsById(ANALYSIS_ID)).thenReturn(true);
            when(analysisInfoRepository.isAnalysisCompleted(ANALYSIS_ID)).thenReturn(true);
        }

        @Test
        void getAnalysisSummary_ReturnsDTO() {
            AnalysisInfo info = new AnalysisInfo();
            AnalysisStatistics stats = new AnalysisStatistics();
            SonarRepoAnalysisResult sonar = new SonarRepoAnalysisResult();

            when(analysisInfoRepository.findById(ANALYSIS_ID)).thenReturn(Optional.of(info));
            when(analysisStatisticsRepository.findById(ANALYSIS_ID)).thenReturn(Optional.of(stats));
            when(sonarAnalysisRepository.findByRepoAnalysisId(ANALYSIS_ID)).thenReturn(Optional.of(sonar));

            AnalysisSummaryDTO dto = new AnalysisSummaryDTO(any(), any(), any());
            when(analysisSummaryMapper.toDTO(info, stats, sonar)).thenReturn(dto);

            AnalysisSummaryDTO result = service.getAnalysisSummary(ANALYSIS_ID);

            assertEquals(dto, result);
        }

        @Test
        void getFileData_ReturnsFileDataDTO() {
            String path = "src/File.java";

            FileInfo fileInfo = new FileInfo();
            FileKnowledge knowledge = new FileKnowledge();
            SonarRepoAnalysisComponent sonar = new SonarRepoAnalysisComponent();

            when(fileInfoRepository.findByAnalysisIdAndFilePath(ANALYSIS_ID, path))
                    .thenReturn(Optional.of(fileInfo));
            when(fileKnowledgeRepository.findByAnalysisIdAndFilePath(ANALYSIS_ID, path))
                    .thenReturn(Optional.of(knowledge));
            when(sonarAnalysisComponentRepository.findByRepoAnalysisIdAndPath(ANALYSIS_ID, path))
                    .thenReturn(Optional.of(sonar));

            FileDataDTO dto = new FileDataDTO(any(),any(), any());
            when(fileDataMapper.toDTO(fileInfo, knowledge, sonar)).thenReturn(dto);

            FileDataDTO result = service.getFileData(ANALYSIS_ID, path);

            assertEquals(dto, result);
        }

        @Test
        void getFileData_WhenNotFound_ThrowsException() {
            when(fileInfoRepository.findByAnalysisIdAndFilePath(ANALYSIS_ID, "missing"))
                    .thenReturn(Optional.empty());

            assertThrows(ObjectNotFoundException.class, () -> service.getFileData(ANALYSIS_ID, "missing"));
        }

        @Test
        void getHotspots_ReturnsNormalizedDTOs() {
            FileInfoRepository.HotspotProjection p1 = mock(FileInfoRepository.HotspotProjection.class);
            when(p1.getCodeLines()).thenReturn(100);
            when(p1.getCommitsInHotspotAnalysisPeriod()).thenReturn(10);

            FileInfoRepository.HotspotProjection p2 = mock(FileInfoRepository.HotspotProjection.class);
            when(p2.getCodeLines()).thenReturn(200);
            when(p2.getCommitsInHotspotAnalysisPeriod()).thenReturn(5);

            when(fileInfoRepository.findAllHotspotsByAnalysisId(ANALYSIS_ID))
                    .thenReturn(List.of(p1, p2));

            HotspotDTO dto1 = new HotspotDTO("path1", 1.0);
            HotspotDTO dto2 = new HotspotDTO("path2", 0.79);

            when(fileInfoMapper.toHotspotDTO(eq(p1), anyDouble())).thenReturn(dto1);
            when(fileInfoMapper.toHotspotDTO(eq(p2), anyDouble())).thenReturn(dto2);

            List<HotspotDTO> result = service.getHotspots(ANALYSIS_ID);

            assertEquals(2, result.size());
        }

        @Test
        void getHotspots_NoCodeFiles_ReturnsEmptyList() {
            FileInfoRepository.HotspotProjection p1 = mock(FileInfoRepository.HotspotProjection.class);
            when(p1.getCodeLines()).thenReturn(null);

            when(fileInfoRepository.findAllHotspotsByAnalysisId(ANALYSIS_ID))
                    .thenReturn(List.of(p1));

            assertTrue(service.getHotspots(ANALYSIS_ID).isEmpty());
        }

        @Test
        void getAuthorStatistics_ReturnsDTO() {
            AuthorStatistics stats = new AuthorStatistics();
            when(authorStatisticsRepository.findByAnalysisIdAndName(ANALYSIS_ID, "John"))
                    .thenReturn(Optional.of(stats));

            AuthorStatisticsDTO dto = new AuthorStatisticsDTO(
                    "John",
                    Set.of(),
                    LocalDate.of(1970, 1, 1),
                    LocalDate.of(1970, 1, 1),
                    true,
                    0, 0, 0, 0, 0, 0, 0);
            when(authorStatisticsMapper.toDTO(stats)).thenReturn(dto);

            assertEquals(dto, service.getAuthorStatistics(ANALYSIS_ID, "John"));
        }

        @Test
        void getAuthorStatistics_NotFound_Throws() {
            when(authorStatisticsRepository.findByAnalysisIdAndName(ANALYSIS_ID, "Missing"))
                    .thenReturn(Optional.empty());

            assertThrows(ObjectNotFoundException.class, () -> service.getAuthorStatistics(ANALYSIS_ID, "Missing"));
        }

        @Test
        void getActivityTrends_ReturnsDTOs() {
            ActivityTrends trends = new ActivityTrends();
            trends.setDailyStats(List.of());

            when(activityTrendsRepository.findById(ANALYSIS_ID))
                    .thenReturn(Optional.of(trends));

            List<DailyStatsDTO> result = service.getActivityTrends(ANALYSIS_ID);

            assertNotNull(result);
        }

        @Test
        void getActivityTrends_NotFound_Throws() {
            when(activityTrendsRepository.findById(ANALYSIS_ID))
                    .thenReturn(Optional.empty());

            assertThrows(ObjectNotFoundException.class, () -> service.getActivityTrends(ANALYSIS_ID));
        }

        @Test
        void getRepositoryStructure_ReturnsStructure() {
            FileInfo f1 = new FileInfo();

            when(fileInfoRepository.findAllByAnalysisId(ANALYSIS_ID))
                    .thenReturn(List.of(f1));

            RepositoryStructureNode node = new RepositoryStructureNode();
            when(repositoryStructureService.buildRepositoryStructure(List.of(f1)))
                    .thenReturn(node);

            RepositoryStructureNode result = service.getRepositoryStructure(ANALYSIS_ID);

            assertEquals(node, result);
        }

        @Test
        void getAllItemsInRepository_ReturnsFilesAndFolders() {
            FileInfoRepository.RepositoryItemProjection fileProjection1 = mock(
                    FileInfoRepository.RepositoryItemProjection.class);
            when(fileProjection1.getFilePath()).thenReturn("src/main/App.java");

            FileInfoRepository.RepositoryItemProjection fileProjection2 = mock(
                    FileInfoRepository.RepositoryItemProjection.class);
            when(fileProjection2.getFilePath()).thenReturn("src/test/AppTest.java");

            when(fileInfoRepository.findAllPathNamesByAnalysisId(ANALYSIS_ID))
                    .thenReturn(List.of(fileProjection1, fileProjection2));

            RepositoryItemDTO item1 = new RepositoryItemDTO("src/main/App.java", "App.java", "file");
            RepositoryItemDTO item2 = new RepositoryItemDTO("src/test/AppTest.java", "AppTest.java", "file");
            when(fileInfoMapper.toRepositoryItemDTO(fileProjection1)).thenReturn(item1);
            when(fileInfoMapper.toRepositoryItemDTO(fileProjection2)).thenReturn(item2);

            List<RepositoryItemDTO> result = service.getAllItemsInRepository(ANALYSIS_ID);

            assertNotNull(result);
            assertEquals(6, result.size());

            assertTrue(result.stream().anyMatch(item ->
                    item.path().equals("src/main/App.java")));

            assertTrue(result.stream().anyMatch(item ->
                    item.path().equals("src") && item.name().equals("src")));
        }

        @Test
        void getAllFilesData_ReturnsFullFileDataList() {
            FileInfo fileInfo1 = new FileInfo();
            fileInfo1.setFilePath("src/App.java");

            FileKnowledge fileKnowledge1 = new FileKnowledge();
            fileKnowledge1.setFilePath("src/App.java");

            when(fileInfoRepository.findAllByAnalysisId(ANALYSIS_ID))
                    .thenReturn(List.of(fileInfo1));
            when(fileKnowledgeRepository.findAllByAnalysisId(ANALYSIS_ID))
                    .thenReturn(List.of(fileKnowledge1));
            when(sonarAnalysisComponentRepository.findAllByRepoAnalysisId(anyString()))
                    .thenReturn(List.of());

            FileDataDTO dto1 = new FileDataDTO(mock(FileInfoDTO.class), mock(FileKnowledgeDTO.class), null);
            when(fileDataMapper.toDTO(any(), any(), any())).thenReturn(dto1);

            List<FileDataDTO> result = service.getAllFilesData(ANALYSIS_ID);

            assertNotNull(result);
            assertEquals(1, result.size());
            verify(fileDataMapper).toDTO(fileInfo1, fileKnowledge1, null);
        }

        @Test
        void getAllFilesTypes_ReturnsMappedList() {
            FileInfoRepository.FileTypeProjection projection = mock(FileInfoRepository.FileTypeProjection.class);

            when(fileInfoRepository.findAllTypesByAnalysisId(ANALYSIS_ID))
                    .thenReturn(List.of(projection));

            FileTypeDTO dto = new FileTypeDTO("path", "java");
            when(fileInfoMapper.toTypeDTO(projection)).thenReturn(dto);

            List<FileTypeDTO> result = service.getAllFilesTypes(ANALYSIS_ID);

            assertEquals(1, result.size());
            assertEquals(dto, result.getFirst());
        }

        @Test
        void getAllFilesCodeAge_ReturnsMappedList() {
            FileInfoRepository.FileCodeAgeProjection p1 = mock(FileInfoRepository.FileCodeAgeProjection.class);
            when(p1.getCodeAgeDays()).thenReturn(10);

            FileInfoRepository.FileCodeAgeProjection p2 = mock(FileInfoRepository.FileCodeAgeProjection.class);
            when(p2.getCodeAgeDays()).thenReturn(20);

            when(fileInfoRepository.findAllCodeAgesByAnalysisId(ANALYSIS_ID))
                    .thenReturn(List.of(p1, p2));

            FileCodeAgeDTO dto1 = new FileCodeAgeDTO("path1", 10, 0.5);
            FileCodeAgeDTO dto2 = new FileCodeAgeDTO("path2", 20, 0.5);

            when(fileInfoMapper.toCodeAgeDTO(eq(p1), anyDouble())).thenReturn(dto1);
            when(fileInfoMapper.toCodeAgeDTO(eq(p2), anyDouble())).thenReturn(dto2);

            List<FileCodeAgeDTO> result = service.getAllFilesCodeAge(ANALYSIS_ID);

            assertEquals(2, result.size());
        }

        @Test
        void getAllFilesKnowledgeLossRisk_ReturnsMappedList() {
            FileKnowledgeRepository.FileKnowledgeLossRiskProjection projection =
                    mock(FileKnowledgeRepository.FileKnowledgeLossRiskProjection.class);

            when(projection.getKnowledgeLoss()).thenReturn(50.0);
            when(fileKnowledgeRepository.findAllKnowledgeLossRiskByAnalysisId(ANALYSIS_ID))
                    .thenReturn(List.of(projection));

            FileKnowledgeLossRiskDTO dto = new FileKnowledgeLossRiskDTO(
                    "path", KnowledgeRisk.SINGLE_OWNER, 0.5, 0.5
            );
            when(fileKnowledgeMapper.toKnowledgeLossRiskDTO(eq(projection), anyDouble()))
                    .thenReturn(dto);

            List<FileKnowledgeLossRiskDTO> result = service.getAllFilesKnowledgeLossRisk(ANALYSIS_ID);

            assertEquals(1, result.size());
            assertEquals(dto, result.getFirst());
        }

        @Test
        void getAllFilesLeadAuthors_ReturnsMappedList() {
            FileKnowledgeRepository.FileLeadAuthorProjection projection =
                    mock(FileKnowledgeRepository.FileLeadAuthorProjection.class);

            when(fileKnowledgeRepository.findAllLeadAuthorsByAnalysisId(ANALYSIS_ID))
                    .thenReturn(List.of(projection));

            FileLeadAuthorDTO dto = new FileLeadAuthorDTO("path", "leadAuthor");
            when(fileKnowledgeMapper.toLeadAuthorDTO(projection)).thenReturn(dto);

            List<FileLeadAuthorDTO> result = service.getAllFilesLeadAuthors(ANALYSIS_ID);

            assertEquals(1, result.size());
            assertEquals(dto, result.getFirst());
        }

        @Test
        void getAllAuthors_ReturnsMappedList() {
            AuthorStatisticsRepository.AuthorSummaryProjection projection =
                    mock(AuthorStatisticsRepository.AuthorSummaryProjection.class);

            when(authorStatisticsRepository.findAllSummariesByAnalysisId(ANALYSIS_ID))
                    .thenReturn(List.of(projection));

            AuthorSummaryDTO dto = new AuthorSummaryDTO(
                    "name",
                    Set.of(),
                    true);
            when(authorStatisticsMapper.toSummaryDTO(projection)).thenReturn(dto);

            List<AuthorSummaryDTO> result = service.getAllAuthors(ANALYSIS_ID);

            assertEquals(1, result.size());
            assertEquals(dto, result.getFirst());
        }

        @Test
        void getAllAuthorsStatistics_ReturnsMappedList() {
            AuthorStatistics s = new AuthorStatistics();

            when(authorStatisticsRepository.findAllByAnalysisId(ANALYSIS_ID))
                    .thenReturn(List.of(s));

            AuthorStatisticsDTO dto = new AuthorStatisticsDTO(
                    "name",
                    Set.of(),
                    LocalDate.of(1970, 1, 1),
                    LocalDate.of(1970, 1, 1),
                    true,
                    0, 0, 0, 0, 0, 0, 0);
            when(authorStatisticsMapper.toDTO(s)).thenReturn(dto);

            List<AuthorStatisticsDTO> result = service.getAllAuthorsStatistics(ANALYSIS_ID);

            assertEquals(1, result.size());
            assertEquals(dto, result.getFirst());
        }

        @Test
        void getAllAuthorsCouplings_ReturnsMappedList() {
            AuthorCouplingRepository.AuthorCouplingDataProjection projection =
                    mock(AuthorCouplingRepository.AuthorCouplingDataProjection.class);

            when(authorCouplingRepository.findAllCouplingDataByAnalysisId(ANALYSIS_ID))
                    .thenReturn(List.of(projection));

            AuthorCouplingDTO dto = new AuthorCouplingDTO("name", 0, 0, List.of());
            when(authorCouplingMapper.toDTO(projection)).thenReturn(dto);

            List<AuthorCouplingDTO> result = service.getAllAuthorsCouplings(ANALYSIS_ID);

            assertEquals(1, result.size());
            assertEquals(dto, result.getFirst());
        }

        @Test
        void getAllFilesCoupling_ReturnsMappedList() {
            FileCouplingRepository.FileCouplingDataProjection projection =
                    mock(FileCouplingRepository.FileCouplingDataProjection.class);

            when(fileCouplingRepository.findAllCouplingDataByAnalysisId(ANALYSIS_ID))
                    .thenReturn(List.of(projection));

            FileCouplingDTO dto = new FileCouplingDTO("path", List.of());
            when(fileCouplingMapper.toDTO(projection)).thenReturn(dto);

            List<FileCouplingDTO> result = service.getAllFilesCoupling(ANALYSIS_ID);

            assertEquals(1, result.size());
            assertEquals(dto, result.getFirst());
        }

    }

    @Nested
    class WithoutSetup {
        @Test
        void checkIfAnalysisCompleted_ThrowsIfNotCompleted() {
            when(analysisInfoRepository.existsById("X")).thenReturn(true);
            when(analysisInfoRepository.isAnalysisCompleted("X")).thenReturn(false);

            assertThrows(IllegalStateException.class, () -> service.getAllItemsInRepository("X"));
        }

        @Test
        void checkIfAnalysisCompleted_ThrowsIfAnalysisDoesNotExist() {
            when(analysisInfoRepository.existsById("X")).thenReturn(false);

            assertThrows(ObjectNotFoundException.class, () -> service.getAllAuthors("X"));
        }
    }

}