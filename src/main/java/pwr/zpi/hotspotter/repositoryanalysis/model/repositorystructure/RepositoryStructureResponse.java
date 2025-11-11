package pwr.zpi.hotspotter.repositoryanalysis.model.repositorystructure;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepositoryStructureResponse {

    @JsonProperty("structure")
    private RepositoryStructureNode structure;

    @JsonProperty("refdata")
    private ReferenceData referenceData;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReferenceData {

        @JsonProperty("mc")
        private Integer maxCommits;

        @JsonProperty("mchs")
        private Integer maxCommitsInHotSpotAnalysisPeriod;

        @JsonProperty("mloc")
        private Integer maxLinesOfCode;

    }

}
