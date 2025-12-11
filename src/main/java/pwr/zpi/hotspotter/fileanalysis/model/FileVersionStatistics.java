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

    private String url;

    @Builder.Default
    private Integer totalLines = 0;

    @Builder.Default
    private Integer codeLines = 0;

    @Builder.Default
    private Integer commentLines = 0;

    @Builder.Default
    private Integer blankLines = 0;

    @Builder.Default
    private Integer complexity = 1;

    @Builder.Default
    private Integer numberOfMethods = 0;

}
