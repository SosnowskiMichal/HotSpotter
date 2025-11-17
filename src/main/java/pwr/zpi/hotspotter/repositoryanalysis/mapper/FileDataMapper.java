package pwr.zpi.hotspotter.repositoryanalysis.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.coupling.model.FileCoupling;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo.model.FileInfo;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge.model.FileKnowledge;
import pwr.zpi.hotspotter.repositoryanalysis.dto.FileCouplingDTO;
import pwr.zpi.hotspotter.repositoryanalysis.dto.FileDataDTO;
import pwr.zpi.hotspotter.repositoryanalysis.dto.FileInfoDTO;
import pwr.zpi.hotspotter.repositoryanalysis.dto.FileKnowledgeDTO;

@Component
@RequiredArgsConstructor
public class FileDataMapper {

    private final FileInfoMapper fileInfoMapper;
    private final FileCouplingMapper fileCouplingMapper;
    private final FileKnowledgeMapper fileKnowledgeMapper;

    public FileDataDTO toDTO(FileInfo fileInfo, FileCoupling fileCoupling, FileKnowledge fileKnowledge) {
        FileInfoDTO fileInfoDTO = fileInfoMapper.toDTO(fileInfo);
        FileCouplingDTO fileCouplingDTO = fileCouplingMapper.toDTO(fileCoupling);
        FileKnowledgeDTO fileKnowledgeDTO = fileKnowledgeMapper.toDTO(fileKnowledge);

        return new FileDataDTO(fileInfoDTO, fileCouplingDTO, fileKnowledgeDTO);
    }

}
