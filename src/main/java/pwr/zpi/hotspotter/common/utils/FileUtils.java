package pwr.zpi.hotspotter.common.utils;

import lombok.experimental.UtilityClass;
import java.nio.file.Path;

@UtilityClass
public class FileUtils {

    public static String getFileNameWithoutExtension(Path path) {
        if (path == null) return "";
        String fileName = path.getFileName().toString();
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return fileName;
        }

        return fileName.substring(0, lastDotIndex);
    }

    public static String getExtension(Path path) {
        if (path == null) return "";
        String fileName = path.getFileName().toString();
        int lastIndexOf = fileName.lastIndexOf(".");
        if (lastIndexOf == -1) return "";

        return fileName.substring(lastIndexOf + 1).toLowerCase();
    }
}
