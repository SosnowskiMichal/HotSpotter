package pwr.zpi.hotspotter.user.model.analysispreferences;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FileCouplingSettings {
    public static final int DEFAULT_MIN_FILE_COMMITS = 5;
    public static final int DEFAULT_MIN_SHARED_COMMITS = 5;
    public static final double DEFAULT_MIN_COUPLING = 0.1;
    public static final int DEFAULT_MAX_FILES_PER_COMMIT = 30;

    private int minFileCommits = DEFAULT_MIN_FILE_COMMITS;
    private int minSharedCommits = DEFAULT_MIN_SHARED_COMMITS;
    private double minCoupling = DEFAULT_MIN_COUPLING;
    private int maxFilesPerCommit = DEFAULT_MAX_FILES_PER_COMMIT;
    private List<FileIgnorePattern> fileIgnorePatterns = new ArrayList<>();
}
