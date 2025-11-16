package pwr.zpi.hotspotter.repositoryanalysis.analyzer.coupling.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoupledFile {

    @NotBlank(message = "File path is required")
    private String filePath;

    @Builder.Default
    private Integer sharedCommits = 0;

    private Double couplingPercentage;

}
