package pwr.zpi.hotspotter.fileanalysis.analyzer.ownership.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "file_ownership")
@CompoundIndex(name = "analysis_file_idx", def = "{'analysisId': 1, 'filePath': 1}", unique = true)
public class FileOwnership {

    @Id
    private String id;

    @NotBlank(message = "Analysis ID is required")
    private String analysisId;

    @NotBlank(message = "File path is required")
    private String filePath;

    @NotNull(message = "Total lines added is required")
    private Integer linesAdded;

    @NotNull(message = "Total commits is required")
    private Integer commits;

    @NotNull(message = "Author contributions are required")
    private List<AuthorContribution> authorContributions;

    @NotNull(message = "Lead authors list is required")
    private List<String> leadAuthors;

    @NotNull(message = "Number of contributors is required")
    private Integer contributors;

}
