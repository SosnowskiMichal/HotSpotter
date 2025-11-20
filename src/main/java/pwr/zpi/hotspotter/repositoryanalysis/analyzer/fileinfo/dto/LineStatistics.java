package pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LineStatistics {

    private Integer codeLines;

    private Integer commentLines;

    private Integer blankLines;

}
