package pwr.zpi.hotspotter.repositoryanalysis.mapper;

import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.coupling.model.CoupledFile;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.coupling.model.FileCoupling;
import pwr.zpi.hotspotter.repositoryanalysis.dto.FileCouplingDTO;

import java.util.List;

@Component
public class FileCouplingMapper {

    public FileCouplingDTO toDTO(FileCoupling fileCoupling) {
        if (fileCoupling == null) return null;

        List<FileCouplingDTO.CoupledFileDTO> coupledFilesDTOs = fileCoupling.getCoupledFiles().stream()
                .map(this::toCoupledFileDTO)
                .toList();

        return new FileCouplingDTO(coupledFilesDTOs);
    }

    private FileCouplingDTO.CoupledFileDTO toCoupledFileDTO(CoupledFile coupledFile) {
        return new FileCouplingDTO.CoupledFileDTO(
                coupledFile.getFilePath(),
                coupledFile.getSharedCommits(),
                coupledFile.getCouplingPercentage()
        );
    }

}
