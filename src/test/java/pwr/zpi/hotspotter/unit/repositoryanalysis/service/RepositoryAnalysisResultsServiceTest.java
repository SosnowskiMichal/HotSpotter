package pwr.zpi.hotspotter.unit.repositoryanalysis.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pwr.zpi.hotspotter.common.exceptions.ObjectNotFoundException;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.coupling.model.FileCoupling;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.coupling.repository.FileCouplingRepository;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo.model.FileInfo;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo.repository.FileInfoRepository;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge.model.FileKnowledge;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge.repository.FileKnowledgeRepository;
import pwr.zpi.hotspotter.repositoryanalysis.dto.FileDataDTO;
import pwr.zpi.hotspotter.repositoryanalysis.dto.FilePathNameDTO;
import pwr.zpi.hotspotter.repositoryanalysis.dto.RepositoryStructureNode;
import pwr.zpi.hotspotter.repositoryanalysis.mapper.FileDataMapper;
import pwr.zpi.hotspotter.repositoryanalysis.repository.AnalysisInfoRepository;
import pwr.zpi.hotspotter.repositoryanalysis.service.RepositoryAnalysisResultsService;
import pwr.zpi.hotspotter.repositoryanalysis.service.RepositoryStructureService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepositoryAnalysisResultsServiceTest {

    @Mock
    private AnalysisInfoRepository analysisInfoRepository;
    @Mock
    private FileInfoRepository fileInfoRepository;
    @Mock
    private FileCouplingRepository fileCouplingRepository;
    @Mock
    private FileKnowledgeRepository fileKnowledgeRepository;
    @Mock
    private RepositoryStructureService repositoryStructureService;
    @Mock
    private FileDataMapper fileDataMapper;

    @InjectMocks
    private RepositoryAnalysisResultsService repositoryAnalysisResultsService;

    @Test
    void returnsRepositoryStructureWhenAnalysisIsCompleted() {
        String analysisId = "analysis-123";
        List<FileInfo> fileInfoData = List.of(mock(FileInfo.class));
        RepositoryStructureNode expectedStructure = mock(RepositoryStructureNode.class);

        when(analysisInfoRepository.existsById(analysisId)).thenReturn(true);
        when(analysisInfoRepository.isAnalysisCompleted(analysisId)).thenReturn(true);
        when(fileInfoRepository.findAllByAnalysisId(analysisId)).thenReturn(fileInfoData);
        when(repositoryStructureService.buildRepositoryStructure(fileInfoData)).thenReturn(expectedStructure);

        RepositoryStructureNode result = repositoryAnalysisResultsService.getRepositoryStructure(analysisId);

        assertEquals(expectedStructure, result);
    }

    @Test
    void throwsExceptionWhenAnalysisDoesNotExist() {
        String analysisId = "non-existent-analysis";

        when(analysisInfoRepository.existsById(analysisId)).thenReturn(false);

        assertThrows(ObjectNotFoundException.class, () -> repositoryAnalysisResultsService.getRepositoryStructure(analysisId));
    }

    @Test
    void throwsExceptionWhenAnalysisIsNotCompleted() {
        String analysisId = "incomplete-analysis";

        when(analysisInfoRepository.existsById(analysisId)).thenReturn(true);
        when(analysisInfoRepository.isAnalysisCompleted(analysisId)).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> repositoryAnalysisResultsService.getRepositoryStructure(analysisId));
    }

    @Test
    void returnsFileDataWhenFileExists() {
        String analysisId = "analysis-123";
        String filePath = "/path/to/file";
        FileInfo fileInfo = mock(FileInfo.class);
        FileCoupling fileCoupling = mock(FileCoupling.class);
        FileKnowledge fileKnowledge = mock(FileKnowledge.class);
        FileDataDTO expectedFileData = mock(FileDataDTO.class);

        when(analysisInfoRepository.existsById(analysisId)).thenReturn(true);
        when(analysisInfoRepository.isAnalysisCompleted(analysisId)).thenReturn(true);
        when(fileInfoRepository.findByAnalysisIdAndFilePath(analysisId, filePath)).thenReturn(Optional.of(fileInfo));
        when(fileCouplingRepository.findByAnalysisIdAndFilePath(analysisId, filePath)).thenReturn(Optional.of(fileCoupling));
        when(fileKnowledgeRepository.findByAnalysisIdAndFilePath(analysisId, filePath)).thenReturn(Optional.of(fileKnowledge));
        when(fileDataMapper.toDTO(fileInfo, fileCoupling, fileKnowledge)).thenReturn(expectedFileData);

        FileDataDTO result = repositoryAnalysisResultsService.getFileData(analysisId, filePath);

        assertEquals(expectedFileData, result);
    }

    @Test
    void throwsExceptionWhenFileDoesNotExist() {
        String analysisId = "analysis-123";
        String filePath = "/non/existent/file";

        when(analysisInfoRepository.existsById(analysisId)).thenReturn(true);
        when(analysisInfoRepository.isAnalysisCompleted(analysisId)).thenReturn(true);
        when(fileInfoRepository.findByAnalysisIdAndFilePath(analysisId, filePath)).thenReturn(Optional.empty());

        assertThrows(ObjectNotFoundException.class, () -> repositoryAnalysisResultsService.getFileData(analysisId, filePath));
    }

    @Test
    void returnsFilesInRepositoryWhenAnalysisIsCompleted() {
        String analysisId = "analysis-123";
        FileInfo fileInfo = mock(FileInfo.class);
        List<FileInfo> fileInfoData = List.of(fileInfo);

        when(analysisInfoRepository.existsById(analysisId)).thenReturn(true);
        when(analysisInfoRepository.isAnalysisCompleted(analysisId)).thenReturn(true);
        when(fileInfoRepository.findAllByAnalysisId(analysisId)).thenReturn(fileInfoData);

        List<FilePathNameDTO> result = repositoryAnalysisResultsService.getFilesInRepository(analysisId);

        assertEquals(fileInfoData.size(), result.size());
    }
}
