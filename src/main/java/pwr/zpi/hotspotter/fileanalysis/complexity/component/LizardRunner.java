package pwr.zpi.hotspotter.fileanalysis.complexity.component;

import org.springframework.stereotype.Component;
import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
public class LizardRunner {
    public List<String> runLizard(Path path) throws IOException {
        List<String> outputLines = new ArrayList<>();
        ProcessBuilder pb = new ProcessBuilder("lizard", ".", "--csv");
        pb.directory(path.toFile());
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                outputLines.add(line);
            }
        }

        return outputLines;
    }
}
