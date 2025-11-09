package pwr.zpi.hotspotter.user.model.analysispreferences;

import lombok.Data;

@Data
public class FileIgnorePattern {
    private String name;
    private String firstPattern;
    private String secondPattern;
}
