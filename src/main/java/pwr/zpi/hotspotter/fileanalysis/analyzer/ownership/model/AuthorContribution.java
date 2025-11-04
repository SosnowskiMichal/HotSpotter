package pwr.zpi.hotspotter.fileanalysis.analyzer.ownership.model;

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
    private int linesAdded = 0;

    @Builder.Default
    private int commits = 0;

    @Builder.Default
    private double contributionPercentage = 0.0;

    public AuthorContribution(String name) {
        this.name = name;
    }

    public void increaseLinesAdded(int lines) {
        this.linesAdded += lines;
    }

    public void incrementCommits() {
        this.commits++;
    }

}
