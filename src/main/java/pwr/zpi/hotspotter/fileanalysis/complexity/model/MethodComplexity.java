package pwr.zpi.hotspotter.fileanalysis.complexity.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MethodComplexity {
    private String name;
    private int complexity;
    private int parameters;
    private int startLine;
    private int endLine;
}