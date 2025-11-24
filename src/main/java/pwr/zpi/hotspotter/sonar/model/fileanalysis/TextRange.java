package pwr.zpi.hotspotter.sonar.model.fileanalysis;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TextRange {
    private int startLine;
    private int endLine;
    private int startOffset;
    private int endOffset;
}
