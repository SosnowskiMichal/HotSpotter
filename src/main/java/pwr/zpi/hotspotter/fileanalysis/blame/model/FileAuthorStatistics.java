package pwr.zpi.hotspotter.fileanalysis.blame.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileAuthorStatistics {

    private String authorName;

    private Integer linesAuthored;

    private Double percentage;

}
