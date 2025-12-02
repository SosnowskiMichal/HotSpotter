package pwr.zpi.hotspotter.fileanalysis.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileVersionStatistics {

    private String commitHash;

    private String fileVersionPath;

    private String fileVersionUrl;

    private String fileSize;

    private Integer totalLines;

    private Integer codeLines;

    private Integer commentLines;

    private Integer blankLines;

    // TODO: Add complexity metrics...

    // TODO: Add method-level information...

}
