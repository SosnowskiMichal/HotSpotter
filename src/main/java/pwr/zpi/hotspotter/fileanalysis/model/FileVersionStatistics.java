package pwr.zpi.hotspotter.fileanalysis.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class FileVersionStatistics {

    private String hash;

    private LocalDate date;

    private String path;

    private String url; // TODO

    private Integer totalLines;

    private Integer codeLines;

    private Integer commentLines;

    private Integer blankLines;

    private Integer complexity;

    private Integer numberOfMethods;

}
