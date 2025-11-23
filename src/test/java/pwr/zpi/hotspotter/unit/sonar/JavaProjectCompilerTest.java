package pwr.zpi.hotspotter.unit.sonar;

import org.apache.maven.cli.MavenCli;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.GradleConnector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import pwr.zpi.hotspotter.sonar.service.JavaProjectCompiler;

import java.nio.file.*;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JavaProjectCompilerTest {

    @InjectMocks
    private JavaProjectCompiler compiler;

    @Test
    void isJavaProject_ReturnsTrue_WhenPomExists() {
        try (MockedStatic<Files> files = mockStatic(Files.class)) {
            Path path = Path.of("project");

            files.when(() -> Files.exists(path.resolve("pom.xml"))).thenReturn(true);

            assertTrue(compiler.isJavaProject(path));
        }
    }

    @Test
    void isJavaProject_ReturnsFalse_WhenNoMarkersPresent() {
        try (MockedStatic<Files> files = mockStatic(Files.class)) {
            Path path = Path.of("project");

            files.when(() -> Files.exists(any())).thenReturn(false);

            assertFalse(compiler.isJavaProject(path));
        }
    }

    @Test
    void findCommonJavaSourceRoot_ReturnsPath_WhenProjectIsJavaProject() throws Exception {
        try (MockedStatic<Files> files = mockStatic(Files.class)) {
            Path path = Path.of("repo");

            files.when(() -> Files.exists(path)).thenReturn(true);
            files.when(() -> Files.exists(path.resolve("pom.xml"))).thenReturn(true);

            Optional<Path> result = compiler.findCommonJavaSourceRoot(path);

            assertTrue(result.isPresent());
            assertEquals(path, result.get());
        }
    }

    @Test
    void findCommonJavaSourceRoot_ReturnsCommonParent() throws Exception {
        try (MockedStatic<Files> files = mockStatic(Files.class)) {
            Path root = Path.of("repo");

            Path java1 = root.resolve("a/src").resolve("Test1.java");
            Path java2 = root.resolve("a/b/src").resolve("Test2.java");

            Stream<Path> walk = Stream.of(java1, java2);

            files.when(() -> Files.exists(root)).thenReturn(true);
            files.when(() -> Files.walk(root)).thenReturn(walk);
            files.when(() -> Files.isRegularFile(any())).thenReturn(true);

            Optional<Path> result = compiler.findCommonJavaSourceRoot(root);

            assertTrue(result.isPresent());
            assertEquals(root.resolve("a").toAbsolutePath(), result.get().toAbsolutePath());
        }
    }

    @Test
    void findCommonJavaSourceRoot_ReturnsEmpty_WhenNoJavaFiles() throws Exception {
        try (MockedStatic<Files> files = mockStatic(Files.class)) {
            Path root = Path.of("repo");

            files.when(() -> Files.exists(root)).thenReturn(true);
            files.when(() -> Files.walk(root)).thenReturn(Stream.empty());

            Optional<Path> result = compiler.findCommonJavaSourceRoot(root);

            assertTrue(result.isEmpty());
        }
    }

    @Test
    void compileJavaProject_UsesMaven_WhenPomExists() throws Exception {
        Path project = Path.of("repo");

        try (MockedStatic<Files> files = mockStatic(Files.class);
             MockedStatic<JavaProjectCompiler> mockedJavaCompiler = mockStatic(JavaProjectCompiler.class)) {
            files.when(() -> Files.exists(project)).thenReturn(true);
            files.when(() -> Files.exists(project.resolve("pom.xml"))).thenReturn(true);
            files.when(() -> Files.walk(project)).thenReturn(Stream.of(project.resolve("target/classes")));

            MavenCli cli = mock(MavenCli.class);
            mockedJavaCompiler.when(JavaProjectCompiler::getMavenCli).thenReturn(cli);

            when(cli.doMain(any(), any(), any(), any())).thenReturn(0);

            List<String> result = compiler.compileJavaProject(project);

            assertEquals(1, result.size());
            assertTrue(result.getFirst().endsWith("target/classes"));
        }
    }

    @Test
    void compileJavaProject_Throws_WhenMavenFails() {
        Path project = Path.of("repo");

        try (MockedStatic<Files> files = mockStatic(Files.class);
             MockedStatic<JavaProjectCompiler> mockedJavaCompiler = mockStatic(JavaProjectCompiler.class)) {

            files.when(() -> Files.exists(project)).thenReturn(true);
            files.when(() -> Files.exists(project.resolve("pom.xml"))).thenReturn(true);

            MavenCli cli = mock(MavenCli.class);
            mockedJavaCompiler.when(JavaProjectCompiler::getMavenCli).thenReturn(cli);

            when(cli.doMain(any(), any(), any(), any())).thenReturn(1);

            assertThrows(Exception.class, () -> compiler.compileJavaProject(project));
        }
    }

    @Test
    void compileJavaProject_UsesGradle_WhenBuildGradleExists() throws Exception {
        Path project = Path.of("repo");

        ProjectConnection connection = mock(ProjectConnection.class);
        GradleConnector connector = mock(GradleConnector.class);
        org.gradle.tooling.BuildLauncher buildLauncher = mock(org.gradle.tooling.BuildLauncher.class);

        try (MockedStatic<GradleConnector> gradle = mockStatic(GradleConnector.class)) {
            gradle.when(GradleConnector::newConnector).thenReturn(connector);

            when(connector.forProjectDirectory(any())).thenReturn(connector);
            when(connector.connect()).thenReturn(connection);

            when(connection.newBuild()).thenReturn(buildLauncher);
            when(buildLauncher.forTasks("classes")).thenReturn(buildLauncher);
            doNothing().when(buildLauncher).run();

            try (MockedStatic<Files> files = mockStatic(Files.class)) {
                files.when(() -> Files.exists(project)).thenReturn(true);
                files.when(() -> Files.exists(project.resolve("pom.xml"))).thenReturn(false);
                files.when(() -> Files.exists(project.resolve("build.gradle"))).thenReturn(true);
                files.when(() -> Files.walk(project)).thenReturn(Stream.of(project.resolve("build/classes/java/main")));

                List<String> binaries = compiler.compileJavaProject(project);

                assertEquals(1, binaries.size());
                assertTrue(binaries.getFirst().contains("build/classes/java/main"));
            }
        }
    }

    @Test
    void compileJavaProject_Throws_WhenNoSources() {
        Path project = Path.of("repo");

        try (MockedStatic<Files> files = mockStatic(Files.class)) {

            files.when(() -> Files.exists(project)).thenReturn(true);
            files.when(() -> Files.exists(project.resolve("pom.xml"))).thenReturn(false);
            files.when(() -> Files.exists(project.resolve("build.gradle"))).thenReturn(false);

            files.when(() -> Files.walk(project)).thenReturn(Stream.empty());

            assertThrows(Exception.class, () -> compiler.compileJavaProject(project));
        }
    }
}