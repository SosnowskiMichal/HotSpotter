package pwr.zpi.hotspotter.fileanalysis.analyzer.knowledge.model;

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
@Document(collection = "file_knowledge")
@CompoundIndex(name = "analysis_file_idx", def = "{'analysisId': 1, 'filePath': 1}", unique = true)
public class FileKnowledge {

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

    private String leadAuthor;

    @NotNull(message = "Number of contributors is required")
    private Integer contributors;

    private Integer activeContributors;

    @Builder.Default
    private Double knowledgeLoss = 0.0;

    // TODO: Risk (one author, too many, significant knowledge loss, etc.)

}
