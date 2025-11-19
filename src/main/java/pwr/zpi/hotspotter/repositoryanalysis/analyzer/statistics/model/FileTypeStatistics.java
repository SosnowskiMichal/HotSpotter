package pwr.zpi.hotspotter.repositoryanalysis.analyzer.statistics.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileTypeStatistics {

    private String fileType;

    private Integer files;

    private Integer codeLines;

    private Integer commentLines;

    private Integer blankLines;

}
