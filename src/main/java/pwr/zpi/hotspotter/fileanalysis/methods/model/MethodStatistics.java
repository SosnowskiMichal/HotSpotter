package pwr.zpi.hotspotter.fileanalysis.methods.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Data
@Builder
public class MethodStatistics {

    private String name;

    private Integer startLine;

    private Integer endLine;

    private Integer lines;

    private String url;

    private Integer commits;

    private Integer authors;

    private Set<String> authorNames;

    private LocalDate firstCommitDate;

    private LocalDate lastCommitDate;

    private Integer daysSinceLastCommit;

    private List<MethodVersionStatistics> complexityTrends;

}
