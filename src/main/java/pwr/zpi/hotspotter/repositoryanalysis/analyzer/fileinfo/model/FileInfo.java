package pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "file_info")
@CompoundIndex(name = "analysis_file_idx", def = "{'analysisId': 1, 'filePath': 1}", unique = true)
public class FileInfo {

    @Id
    private String id;

    @NotBlank(message = "Analysis ID is required")
    private String analysisId;

    @NotBlank(message = "File path is required")
    private String filePath;

    private String fileType;

    private Integer totalLines;

    private Integer codeLines;

    private Integer commentLines;

    private Integer blankLines;

    @Builder.Default
    private Integer commitsLastMonth = 0;

    @Builder.Default
    private Integer commitsLastYear = 0;

    private LocalDate lastCommitDate;

    private Integer codeAgeDays;

    private Integer codeAgeMonths;

    public void incrementCommitsLastMonth() {
        commitsLastMonth++;
    }

    public void incrementCommitsLastYear() {
        commitsLastYear++;
    }

}
