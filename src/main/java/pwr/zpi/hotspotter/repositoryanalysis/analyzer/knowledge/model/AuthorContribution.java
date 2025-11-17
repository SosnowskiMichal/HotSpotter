package pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorContribution {

    private String name;

    @Builder.Default
    private Integer linesAdded = 0;

    @Builder.Default
    private Integer commits = 0;

    @Builder.Default
    private Double contributionPercentage = 0.0;

    public AuthorContribution(String name) {
        this.name = name;
        this.linesAdded = 0;
        this.commits = 0;
    }

    public void increaseLinesAdded(int lines) {
        this.linesAdded += lines;
    }

    public void incrementCommits() {
        this.commits++;
    }

}
