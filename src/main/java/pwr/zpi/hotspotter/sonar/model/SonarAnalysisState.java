package pwr.zpi.hotspotter.sonar.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SonarAnalysisState {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILED;

    @JsonValue
    public String toValue() {
        return name();
    }

    @JsonCreator
    public static SonarAnalysisState fromString(String value) {
        if (value == null) return null;
        try {
            return SonarAnalysisState.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

