package pwr.zpi.hotspotter.repositoryanalysis.model.repositorystructure;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
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

    @JsonProperty("ft")
    private String fileType;

    @JsonProperty("fs")
    private String fileSize;

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

    @JsonProperty("cly")
    private Integer commitsLastYear;

    @JsonProperty("fcd")
    private LocalDate firstCommitDate;

    @JsonProperty("lcd")
    private LocalDate lastCommitDate;

    @JsonProperty("la")
    private String leadAuthor;

    @JsonProperty("lak")
    private Double leadAuthorKnowledgePercentage;

    @JsonProperty("ct")
    private Integer contributors;

    @JsonProperty("act")
    private Integer activeContributors;

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
