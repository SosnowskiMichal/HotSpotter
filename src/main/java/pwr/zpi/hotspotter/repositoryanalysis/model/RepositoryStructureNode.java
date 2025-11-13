package pwr.zpi.hotspotter.repositoryanalysis.model;

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

    private String name;

    private String path;

    private String type;

    private List<RepositoryStructureNode> children = new ArrayList<>();

    private Double height;

    private Double width;

    public void addChild(RepositoryStructureNode child) {
        if (children == null) {
            children = new ArrayList<>();
        }
        children.add(child);
    }

}
