package pwr.zpi.hotspotter.fileanalysis.methods.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class MethodTracker {

    private String currentName;
    private Integer currentStartLine;
    private Integer currentEndLine;
    private List<MethodVersion> versions = new ArrayList<>();
    private Set<String> authors = new HashSet<>();

    public void addVersion(MethodVersion version) {
        this.versions.add(version);
        this.currentName = version.methodName();
        this.currentStartLine = version.startLine();
        this.currentEndLine = version.endLine();
    }

    public void addAuthor(String author) {
        this.authors.add(author);
    }

}
