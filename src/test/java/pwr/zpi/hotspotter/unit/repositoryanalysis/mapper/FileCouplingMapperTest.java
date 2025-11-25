package pwr.zpi.hotspotter.unit.repositoryanalysis.mapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.coupling.model.CoupledFile;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.coupling.model.FileCoupling;
import pwr.zpi.hotspotter.repositoryanalysis.dto.FileCouplingDTO;
import pwr.zpi.hotspotter.repositoryanalysis.mapper.FileCouplingMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class FileCouplingMapperTest {

    @InjectMocks
    private FileCouplingMapper mapper;

    @Test
    void returnsNullWhenInputNull() {
        assertThat(mapper.toDTO((FileCoupling) null)).isNull();
    }

    @Test
    void mapsCoupledFilesCorrectly() {
        FileCoupling fileCoupling = new FileCoupling(
                "id",
                "analysisId",
                "file/path/Main.java",
                List.of(
                        new CoupledFile("a.java", 5, 0.5),
                        new CoupledFile("b.java", 3, 0.3)
                )
        );

        FileCouplingDTO dto = mapper.toDTO(fileCoupling);

        assertThat(fileCoupling.getId()).isEqualTo("id");
        assertThat(fileCoupling.getAnalysisId()).isEqualTo("analysisId");
        assertThat(fileCoupling.getFilePath()).isEqualTo("file/path/Main.java");
        assertThat(dto.coupledFiles()).hasSize(2);

        assertThat(dto.coupledFiles().getFirst().path()).isEqualTo("a.java");
        assertThat(dto.coupledFiles().get(0).sharedCommits()).isEqualTo(5);
        assertThat(dto.coupledFiles().get(0).percentage()).isEqualTo(0.5);

        assertThat(dto.coupledFiles().get(1).path()).isEqualTo("b.java");
        assertThat(dto.coupledFiles().get(1).sharedCommits()).isEqualTo(3);
        assertThat(dto.coupledFiles().get(1).percentage()).isEqualTo(0.3);
    }
}
