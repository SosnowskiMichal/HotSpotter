package pwr.zpi.hotspotter.repositoryanalysis.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.coupling.model.FileCoupling;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo.model.FileInfo;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge.model.FileKnowledge;
import pwr.zpi.hotspotter.repositoryanalysis.dto.*;
import pwr.zpi.hotspotter.sonar.model.repoanalysis.SonarRepoAnalysisComponent;

@Component
@RequiredArgsConstructor
public class FileDataMapper {

    private final FileInfoMapper fileInfoMapper;
    private final FileCouplingMapper fileCouplingMapper;
    private final FileKnowledgeMapper fileKnowledgeMapper;
    private final SonarAnalysisComponentMapper sonarAnalysisComponentMapper;

    public FileDataDTO toDTO(
            FileInfo fileInfo,
            FileCoupling fileCoupling,
            FileKnowledge fileKnowledge,
            SonarRepoAnalysisComponent sonarAnalysisComponent
    ) {
        FileInfoDTO fileInfoDTO = fileInfoMapper.toDTO(fileInfo);
        FileCouplingDTO fileCouplingDTO = fileCouplingMapper.toDTO(fileCoupling);
        FileKnowledgeDTO fileKnowledgeDTO = fileKnowledgeMapper.toDTO(fileKnowledge);
        SonarAnalysisComponentDTO sonarAnalysisComponentDTO = sonarAnalysisComponentMapper.toDTO(sonarAnalysisComponent);

        return new FileDataDTO(fileInfoDTO, fileCouplingDTO, fileKnowledgeDTO, sonarAnalysisComponentDTO);
    }

}
