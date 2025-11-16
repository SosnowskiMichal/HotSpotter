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
public class CoupledAuthor {

    @NotBlank(message = "Author name is required")
    private String author;

    @Builder.Default
    private Integer sharedFilesChanged = 0;

    @Builder.Default
    private Integer sharedChanges = 0;

    private Double couplingPercentage;

}
