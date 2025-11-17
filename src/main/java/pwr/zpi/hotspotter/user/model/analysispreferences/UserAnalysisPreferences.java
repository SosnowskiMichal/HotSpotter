package pwr.zpi.hotspotter.user.model.analysispreferences;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UserAnalysisPreferences {

    private static final int DEFAULT_HOT_SPOT_ANALYSIS_PERIOD_MONTHS = 12;
    private static final int DEFAULT_AUTHOR_INACTIVITY_THRESHOLD_MONTHS = 6;

    private static final int DEFAULT_MIN_FILE_COMMITS = 5;
    private static final int DEFAULT_MIN_SHARED_COMMITS = 5;
    private static final double DEFAULT_MIN_FILE_COUPLING_PERCENTAGE = 20.0;
    private static final int DEFAULT_MAX_FILES_PER_COMMIT = 25;

    private static final int AUTHOR_COUPLING_ANALYSIS_PERIOD_MONTHS = 12;
    private static final int DEFAULT_MIN_AUTHOR_FILES = 3;
    private static final int DEFAULT_MIN_AUTHOR_CHANGES = 10;
    private static final int DEFAULT_MIN_SHARED_FILES = 3;
    private static final int DEFAULT_MIN_SHARED_CHANGES = 5;
    private static final double DEFAULT_MIN_AUTHOR_COUPLING_PERCENTAGE = 10.0;

    // ==================================================

    private int hotSpotAnalysisPeriodMonths = DEFAULT_HOT_SPOT_ANALYSIS_PERIOD_MONTHS;
    private int authorInactivityThresholdMonths = DEFAULT_AUTHOR_INACTIVITY_THRESHOLD_MONTHS;

    private int minFileCommits = DEFAULT_MIN_FILE_COMMITS;
    private int minSharedCommits = DEFAULT_MIN_SHARED_COMMITS;
    private double minFileCouplingPercentage = DEFAULT_MIN_FILE_COUPLING_PERCENTAGE;
    private int maxFilesPerCommit = DEFAULT_MAX_FILES_PER_COMMIT;
    private List<FileIgnorePattern> fileIgnorePatterns = new ArrayList<>();

    private int authorCouplingAnalysisPeriodMonths = AUTHOR_COUPLING_ANALYSIS_PERIOD_MONTHS;
    private int minAuthorFiles = DEFAULT_MIN_AUTHOR_FILES;
    private int minAuthorChanges = DEFAULT_MIN_AUTHOR_CHANGES;
    private int minSharedFiles = DEFAULT_MIN_SHARED_FILES;
    private int minSharedChanges = DEFAULT_MIN_SHARED_CHANGES;
    private double minAuthorCouplingPercentage = DEFAULT_MIN_AUTHOR_COUPLING_PERCENTAGE;

}
