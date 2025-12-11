package pwr.zpi.hotspotter.repositoryanalysis.analyzer.coupling.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "file_couplings")
@CompoundIndex(name = "analysis_file_idx", def = "{'analysisId': 1, 'filePath': 1}", unique = true)
public class FileCoupling {

    @Id
    private String id;

    @Indexed
    @NotBlank(message = "Analysis ID is required")
    private String analysisId;

    @Indexed
    @NotBlank(message = "File path is required")
    private String filePath;

    @Builder.Default
    private List<CoupledFile> coupledFiles = new ArrayList<>();

}
