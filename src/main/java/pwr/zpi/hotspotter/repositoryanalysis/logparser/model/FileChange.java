package pwr.zpi.hotspotter.repositoryanalysis.logparser.model;

public record FileChange(String filePath, int linesAdded, int linesDeleted, String oldPath, String newPath) {

    public FileChange(String filePath, int linesAdded, int linesDeleted) {
        this(filePath, linesAdded, linesDeleted, null, null);
    }

    public FileChange withFilePathChange(String oldPath, String newPath) {
        return new FileChange(this.filePath, linesAdded, linesDeleted, oldPath, newPath);
    }

    public boolean isRenamed() {
        return oldPath != null && newPath != null;
    }

}
