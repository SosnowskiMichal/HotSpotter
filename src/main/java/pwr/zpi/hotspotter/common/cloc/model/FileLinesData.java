package pwr.zpi.hotspotter.common.cloc.model;

public record FileLinesData(
        String language,
        int code,
        int comment,
        int blank
) {
    public int total() {
        return code + comment + blank;
    }
}
