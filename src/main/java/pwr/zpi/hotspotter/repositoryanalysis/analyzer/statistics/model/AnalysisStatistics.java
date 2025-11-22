package pwr.zpi.hotspotter.repositoryanalysis.analyzer.statistics.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "analysis_statistics")
public class AnalysisStatistics {

    @Id
    private String analysisId;

    private Integer authors;

    private Integer activeAuthors;

    private Integer commits;

    private Integer files;

    private Integer codeLines;

    private Integer commentLines;

    private Integer blankLines;

    private List<FileTypeStatistics> fileTypeSummaries;

}
