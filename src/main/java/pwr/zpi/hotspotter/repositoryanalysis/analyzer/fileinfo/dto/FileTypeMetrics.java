package pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileTypeMetrics {

    private String fileType;

    private Integer files;

    private Integer codeLines;

    private Integer commentLines;

    private Integer blankLines;

}
