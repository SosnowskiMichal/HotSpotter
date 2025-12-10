package pwr.zpi.hotspotter.unit.fileanalysis.complexity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import pwr.zpi.hotspotter.fileanalysis.complexity.component.HeuristicStrategy;
import pwr.zpi.hotspotter.fileanalysis.complexity.model.FileComplexityReport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class HeuristicStrategyTest {

    @InjectMocks
    private HeuristicStrategy heuristicStrategy;

    @Test
    void analyze_ShouldCountKeywordsForCStyleFiles(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("Service.java");
        String content = """
                public class Service {
                    public void test() {
                        if (condition) {           // +1
                            while (true) {         // +1
                                return;
                            }
                        }
                    }
                }
                """;
        Files.writeString(file, content);

        Map<String, FileComplexityReport> result = heuristicStrategy.analyze(tempDir);

        assertTrue(result.containsKey("Service"));
        assertEquals(3, result.get("Service").getTotalCCN());
    }

    @Test
    void analyze_ShouldIgnoreCStyleComments(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("Comments.cpp");
        String content = """
                // if (commented_out) { return; }
                /* while (multiline_comment) {
                       doSomething();
                   }
                */
                if (real_code) {}
                """;
        Files.writeString(file, content);

        Map<String, FileComplexityReport> result = heuristicStrategy.analyze(tempDir);

        assertEquals(2, result.get("Comments").getTotalCCN());
    }

    @Test
    void analyze_ShouldCountKeywordsForHashStyleFiles(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("Script.py");
        String content = """
                def func():
                    # if commented_out: pass
                    if valid:          # +1
                        for x in list: # +1
                            pass
                    elif other:        # +1
                        pass
                """;
        Files.writeString(file, content);

        Map<String, FileComplexityReport> result = heuristicStrategy.analyze(tempDir);

        assertEquals(4, result.get("Script").getTotalCCN());
    }

    @Test
    void analyze_ShouldHandleOperatorsAndSwitchCasesAndIgnoreKeyWordsInStrings(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("Complex.js");
        String content = """
                function x() {
                   return (a && b) || c ? d : e; // &&(+1), ||(+1), ?(+1)
                }
                String s = "if else for while";
                switch(x) {
                    case 1: break; // +1
                    case 2: break; // +1
                }
                """;
        Files.writeString(file, content);

        Map<String, FileComplexityReport> result = heuristicStrategy.analyze(tempDir);

        assertEquals(6, result.get("Complex").getTotalCCN());
    }

    @Test
    void analyze_ShouldWalkDirectoryRecursively(@TempDir Path tempDir) throws IOException {
        Path subDir = Files.createDirectory(tempDir.resolve("sub"));

        Files.writeString(tempDir.resolve("A.java"), "if(x){}");
        Files.writeString(subDir.resolve("B.py"), "if x:");

        Map<String, FileComplexityReport> result = heuristicStrategy.analyze(tempDir);

        assertEquals(2, result.size());
        assertTrue(result.containsKey("A"));
        assertTrue(result.containsKey("B"));
    }

    @Test
    void analyze_ShouldAggregateFilesWithSameName(@TempDir Path tempDir) throws IOException {
        Path v1 = Files.createDirectory(tempDir.resolve("v1"));
        Path v2 = Files.createDirectory(tempDir.resolve("v2"));

        Files.writeString(v1.resolve("Test.java"), "if(a){}");
        Files.writeString(v2.resolve("Test.java"), "if(a){ if(b){} }");

        Map<String, FileComplexityReport> result = heuristicStrategy.analyze(tempDir);

        assertEquals(1, result.size());
        assertTrue(result.containsKey("Test"));

        FileComplexityReport report = result.get("Test");
        assertEquals(5, report.getTotalCCN());
        assertEquals(2, report.getMethodsCount());
    }

    @Test
    void analyze_ShouldHandleUnsupportedExtensionAsCStyle(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("Notes.txt");
        String content = "if someone asks // ignore this";
        Files.writeString(file, content);

        Map<String, FileComplexityReport> result = heuristicStrategy.analyze(tempDir);

        assertEquals(2, result.get("Notes").getTotalCCN());
    }
}