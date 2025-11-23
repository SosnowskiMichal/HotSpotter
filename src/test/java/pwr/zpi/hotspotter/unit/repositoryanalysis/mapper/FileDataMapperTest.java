package pwr.zpi.hotspotter.unit.repositoryanalysis.mapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.coupling.model.FileCoupling;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo.model.FileInfo;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge.model.FileKnowledge;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge.model.KnowledgeRisk;
import pwr.zpi.hotspotter.repositoryanalysis.dto.*;
import pwr.zpi.hotspotter.repositoryanalysis.mapper.*;
import pwr.zpi.hotspotter.sonar.model.repoanalysis.SonarRepoAnalysisComponent;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileDataMapperTest {

    @Mock
    private FileInfoMapper fileInfoMapper;
    @Mock
    private FileCouplingMapper fileCouplingMapper;
    @Mock
    private FileKnowledgeMapper fileKnowledgeMapper;
    @Mock
    private SonarAnalysisResultMapper sonarAnalysisResultMapper;

    @InjectMocks
    private FileDataMapper mapper;

    @Test
    void mapsAllComponentsToDTO() {
        FileInfo fileInfo = new FileInfo();
        FileCoupling fileCoupling = new FileCoupling();
        FileKnowledge fileKnowledge = new FileKnowledge();
        SonarRepoAnalysisComponent sonarRepoAnalysisComponent = new SonarRepoAnalysisComponent();

        FileInfoDTO infoDTO = new FileInfoDTO("path", "name", "type", String.valueOf(1L), 1,1,1,1,1,1,1,null,null,1);
        FileCouplingDTO couplingDTO = new FileCouplingDTO(null, List.of());
        FileKnowledgeDTO knowledgeDTO = new FileKnowledgeDTO(1, "", 1.0, 1,1,1.0, KnowledgeRisk.SINGLE_OWNER, null);
        SonarAnalysisResultDTO sonarDTO = new SonarAnalysisResultDTO(0,0,0,0,0.0);

        when(fileInfoMapper.toDTO(fileInfo)).thenReturn(infoDTO);
        when(fileCouplingMapper.toDTO(fileCoupling)).thenReturn(couplingDTO);
        when(fileKnowledgeMapper.toReducedDTO(fileKnowledge)).thenReturn(knowledgeDTO);
        when(sonarAnalysisResultMapper.toDTO(sonarRepoAnalysisComponent)).thenReturn(sonarDTO);

        FileDataDTO result = mapper.toDTO(fileInfo, fileCoupling, fileKnowledge, sonarRepoAnalysisComponent);

        assertThat(result.info()).isEqualTo(infoDTO);
        assertThat(result.coupling()).isEqualTo(couplingDTO);
        assertThat(result.knowledge()).isEqualTo(knowledgeDTO);
    }
}
