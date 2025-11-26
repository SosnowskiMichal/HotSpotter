package pwr.zpi.hotspotter.unit.repositoryanalysis.mapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo.model.FileInfo;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge.model.FileKnowledge;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge.model.KnowledgeRisk;
import pwr.zpi.hotspotter.repositoryanalysis.dto.*;
import pwr.zpi.hotspotter.repositoryanalysis.mapper.*;
import pwr.zpi.hotspotter.sonar.model.repoanalysis.SonarRepoAnalysisComponent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileDataMapperTest {

    @Mock
    private FileInfoMapper fileInfoMapper;
    @Mock
    private FileKnowledgeMapper fileKnowledgeMapper;
    @Mock
    private SonarAnalysisResultMapper sonarAnalysisResultMapper;

    @InjectMocks
    private FileDataMapper mapper;

    @Test
    void mapsAllComponentsToDTO() {
        FileInfo fileInfo = new FileInfo();
        FileKnowledge fileKnowledge = new FileKnowledge();
        SonarRepoAnalysisComponent sonarRepoAnalysisComponent = new SonarRepoAnalysisComponent();

        FileInfoDTO infoDTO = new FileInfoDTO("path", "name", "type", String.valueOf(1L), "url", 1,1,1,1,1,1,1,null,null,1);
        FileKnowledgeDTO knowledgeDTO = new FileKnowledgeDTO(1, "", 1.0, 1,1,1.0, KnowledgeRisk.SINGLE_OWNER, null);
        SonarAnalysisResultDTO sonarDTO = new SonarAnalysisResultDTO(0,0,0,0,0.0);

        when(fileInfoMapper.toDTO(fileInfo)).thenReturn(infoDTO);
        when(fileKnowledgeMapper.toReducedDTO(fileKnowledge)).thenReturn(knowledgeDTO);
        when(sonarAnalysisResultMapper.toDTO(sonarRepoAnalysisComponent)).thenReturn(sonarDTO);

        FileDataDTO result = mapper.toDTO(fileInfo, fileKnowledge, sonarRepoAnalysisComponent);

        assertThat(result.info()).isEqualTo(infoDTO);
        assertThat(result.knowledge()).isEqualTo(knowledgeDTO);
        assertThat(result.staticAnalysis()).isEqualTo(sonarDTO);
    }

}
