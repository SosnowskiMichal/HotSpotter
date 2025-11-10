package pwr.zpi.hotspotter.user.model.analysispreferences;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UserAnalysisPreferences {
    private static final int DEFAULT_HOT_SPOT_ANALYSIS_PERIOD_MONTHS = 6;
    private static final int DEFAULT_AUTHOR_INACTIVITY_THRESHOLD_MONTHS = 6;

    private static final int DEFAULT_MIN_FILE_COMMITS = 5;
    private static final int DEFAULT_MIN_SHARED_COMMITS = 5;
    private static final double DEFAULT_MIN_FILE_COUPLING = 0.1;
    private static final int DEFAULT_MAX_FILES_PER_COMMIT = 30;

    private int hotSpotAnalysisPeriodMonths = DEFAULT_HOT_SPOT_ANALYSIS_PERIOD_MONTHS;
    private int authorInactivityThresholdMonths = DEFAULT_AUTHOR_INACTIVITY_THRESHOLD_MONTHS;

    private int minFileCommits = DEFAULT_MIN_FILE_COMMITS;
    private int minSharedCommits = DEFAULT_MIN_SHARED_COMMITS;
    private double minFileCoupling = DEFAULT_MIN_FILE_COUPLING;
    private int maxFilesPerCommit = DEFAULT_MAX_FILES_PER_COMMIT;
    private List<FileIgnorePattern> fileIgnorePatterns = new ArrayList<>();
}
