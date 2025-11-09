package pwr.zpi.hotspotter.user.model.analysispreferences;

import lombok.Data;

@Data
public class UserAnalysisPreferences {
    private static final int DEFAULT_ANALYSIS_PERIOD_MONTHS = 6;
    private static final int DEFAULT_ACTIVE_AUTHOR_MAX_INACTIVITY_MONTHS = 6;

    private int analysisPeriodMonths = DEFAULT_ANALYSIS_PERIOD_MONTHS;
    private int activeAuthorMaxInactivityMonths = DEFAULT_ACTIVE_AUTHOR_MAX_INACTIVITY_MONTHS;
    private FileCouplingSettings fileCouplingSettings = new FileCouplingSettings();
}
