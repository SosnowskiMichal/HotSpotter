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
import pwr.zpi.hotspotter.repositoryanalysis.mapper.FileCouplingMapper;
import pwr.zpi.hotspotter.repositoryanalysis.mapper.FileDataMapper;
import pwr.zpi.hotspotter.repositoryanalysis.mapper.FileInfoMapper;
import pwr.zpi.hotspotter.repositoryanalysis.mapper.FileKnowledgeMapper;

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

    @InjectMocks
    private FileDataMapper mapper;

    @Test
    void mapsAllComponentsToDTO() {
        FileInfo fileInfo = new FileInfo();
        FileCoupling fileCoupling = new FileCoupling();
        FileKnowledge fileKnowledge = new FileKnowledge();

        FileInfoDTO infoDTO = new FileInfoDTO("path", "name", "type", String.valueOf(1L), 1,1,1,1,1,1,1,null,null,1,1);
        FileCouplingDTO couplingDTO = new FileCouplingDTO(null);
        FileKnowledgeDTO knowledgeDTO = new FileKnowledgeDTO(1, "", 1.0, 1,1,1.0, KnowledgeRisk.SINGLE_OWNER, null);

        when(fileInfoMapper.toDTO(fileInfo)).thenReturn(infoDTO);
        when(fileCouplingMapper.toDTO(fileCoupling)).thenReturn(couplingDTO);
        when(fileKnowledgeMapper.toDTO(fileKnowledge)).thenReturn(knowledgeDTO);

        FileDataDTO result = mapper.toDTO(fileInfo, fileCoupling, fileKnowledge);

        assertThat(result.info()).isEqualTo(infoDTO);
        assertThat(result.coupling()).isEqualTo(couplingDTO);
        assertThat(result.knowledge()).isEqualTo(knowledgeDTO);
    }
}
