package pwr.zpi.hotspotter.repositoryanalysis.analyzer.coupling.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "author_couplings")
@CompoundIndex(name = "analysis_author_idx", def = "{'analysisId': 1, 'author': 1}", unique = true)
public class AuthorCoupling {

    @Id
    private String id;

    @NotBlank(message = "Analysis ID is required")
    private String analysisId;

    @NotBlank(message = "Author name is required")
    private String author;

    @Builder.Default
    private Integer filesChanged = 0;

    @Builder.Default
    private Integer totalChanges = 0;

    @Builder.Default
    private List<CoupledAuthor> coupledAuthors = new ArrayList<>();

}
