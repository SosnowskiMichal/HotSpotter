package pwr.zpi.hotspotter.repositoryanalysis.model.repositorystructure;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RepositoryStructureNode {

    @JsonProperty("n")
    private String name;

    @JsonProperty("p")
    private String path;

    @JsonProperty("t")
    private String type;

    @JsonProperty("nf")
    private Integer numberOfFiles;

    @JsonProperty("loc")
    private Integer linesOfCode;

    @JsonProperty("cm")
    private Integer commits;

    @JsonProperty("acm")
    private Integer averageCommits;

    @JsonProperty("chs")
    private Integer commitsInHotSpotAnalysisPeriod;

    @JsonProperty("ch")
    private List<RepositoryStructureNode> children = new ArrayList<>();

    @JsonProperty("h")
    private Double height;

    @JsonProperty("w")
    private Double width;

    public void addChild(RepositoryStructureNode child) {
        if (children == null) {
            children = new ArrayList<>();
        }
        children.add(child);
    }

}
