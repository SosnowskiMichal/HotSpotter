package pwr.zpi.hotspotter.unit.repositoryanalysis.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo.model.FileInfo;
import pwr.zpi.hotspotter.repositoryanalysis.dto.RepositoryStructureNode;
import pwr.zpi.hotspotter.repositoryanalysis.service.RepositoryStructureService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepositoryStructureServiceTest {

    @InjectMocks
    private RepositoryStructureService service;

    private FileInfo file(String path, String name, Integer commits, Integer loc) {
        FileInfo fi = mock(FileInfo.class);
        when(fi.getFilePath()).thenReturn(path);
        when(fi.getFileName()).thenReturn(name);
        when(fi.getTotalCommits()).thenReturn(commits);
        when(fi.getCodeLines()).thenReturn(loc);
        return fi;
    }

    @Test
    void buildsTreeWithNestedDirectories() {
        FileInfo f1 = file("src/main/App.java", "App.java", 10, 200);
        FileInfo f2 = file("src/test/AppTest.java", "AppTest.java", 5, 100);

        RepositoryStructureNode root = service.buildRepositoryStructure(List.of(f1, f2));

        assertThat(root.getChildren()).hasSize(1);
        RepositoryStructureNode src = root.getChildren().getFirst();

        assertThat(src.getName()).isEqualTo("src");
        assertThat(src.getChildren()).hasSize(2);
        RepositoryStructureNode mainOrTest = src.getChildren().getFirst();

        assertThat(mainOrTest.getName()).isIn("main", "test");
        assertThat(mainOrTest.getChildren()).hasSize(1);
    }

    @Test
    void createsFileNodesCorrectly() {
        FileInfo f = file("folder/file.txt", "file.txt", 5, 100);

        RepositoryStructureNode root = service.buildRepositoryStructure(List.of(f));

        RepositoryStructureNode folder = root.getChildren().getFirst();
        RepositoryStructureNode fileNode = folder.getChildren().getFirst();

        assertThat(fileNode.getName()).isEqualTo("file.txt");
        assertThat(fileNode.getType()).isEqualTo("file");
        assertThat(fileNode.getPath()).isEqualTo("folder/file.txt");
    }

    @Test
    void assignsHeightAndWidthBasedOnMaxValues() {
        FileInfo f1 = file("a.txt", "a.txt", 10, 200);
        FileInfo f2 = file("b.txt", "b.txt", 5, 100);

        RepositoryStructureNode root = service.buildRepositoryStructure(List.of(f1, f2));

        RepositoryStructureNode a = root.getChildren().get(0);
        RepositoryStructureNode b = root.getChildren().get(1);

        assertThat(a.getHeight()).isGreaterThan(b.getHeight());
        assertThat(a.getWidth()).isGreaterThan(b.getWidth());
    }

    @Test
    void setsZeroDimensionsIfValuesAreNull() {
        FileInfo f = file("null.txt", "null.txt", null, null);

        RepositoryStructureNode root = service.buildRepositoryStructure(List.of(f));

        RepositoryStructureNode fileNode = root.getChildren().getFirst();

        assertThat(fileNode.getHeight()).isEqualTo(0.0);
        assertThat(fileNode.getWidth()).isEqualTo(0.0);
    }

    @Test
    void setsZeroDimensionsIfMaxValuesAreZero() {
        FileInfo f1 = file("one.txt", "one.txt", 0, 0);
        FileInfo f2 = file("two.txt", "two.txt", 0, 0);

        RepositoryStructureNode root = service.buildRepositoryStructure(List.of(f1, f2));

        RepositoryStructureNode one = root.getChildren().get(0);
        RepositoryStructureNode two = root.getChildren().get(1);

        assertThat(one.getHeight()).isEqualTo(0.0);
        assertThat(one.getWidth()).isEqualTo(0.0);

        assertThat(two.getHeight()).isEqualTo(0.0);
        assertThat(two.getWidth()).isEqualTo(0.0);
    }

    @Test
    void roundsDimensionsToNearestStep() {
        FileInfo f = file("file.txt", "file.txt", 1, 1);

        RepositoryStructureNode root = service.buildRepositoryStructure(List.of(f));
        RepositoryStructureNode fileNode = root.getChildren().getFirst();

        double height = fileNode.getHeight();
        double width = fileNode.getWidth();

        assertThat((height * 100) % 5).isZero();
        assertThat((width * 100) % 5).isZero();
    }
}