package pwr.zpi.hotspotter.unit.sonar;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import pwr.zpi.hotspotter.sonar.service.JavaProjectCompiler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JavaProjectCompilerTest {

    @InjectMocks
    private JavaProjectCompiler compiler;

    @Test
    void isJavaProjectReturnsTrueForMavenProject() {
        Path projectPath = mock(Path.class);
        try (var filesMock = mockStatic(Files.class)) {
            filesMock.when(() -> Files.exists(projectPath.resolve("pom.xml"))).thenReturn(true);

            assertTrue(compiler.isJavaProject(projectPath));
        }
    }

    @Test
    void isJavaProjectReturnsTrueForGradleProject() {
        Path projectPath = mock(Path.class);
        try (var filesMock = mockStatic(Files.class)) {
            filesMock.when(() -> Files.exists(projectPath.resolve("build.gradle"))).thenReturn(true);
            assertTrue(compiler.isJavaProject(projectPath));
        }
    }

    @Test
    void isJavaProjectReturnsFalseForNonJavaProject() {
        Path projectPath = mock(Path.class);
        try (var filesMock = mockStatic(Files.class)) {
            filesMock.when(() -> Files.exists(projectPath.resolve("pom.xml"))).thenReturn(false);
            filesMock.when(() -> Files.exists(projectPath.resolve("build.gradle"))).thenReturn(false);
            filesMock.when(() -> Files.exists(projectPath.resolve("src/main/java"))).thenReturn(false);

            assertFalse(compiler.isJavaProject(projectPath));
        }
    }

    @Test
    void findCommonJavaSourceRootReturnsEmptyForNullPath() throws IOException {
        assertEquals(Optional.empty(), compiler.findCommonJavaSourceRoot(null));
    }

    @Test
    void findCommonJavaSourceRootReturnsEmptyForNonExistentPath() throws IOException {
        Path projectPath = mock(Path.class);
        try (var filesMock = mockStatic(Files.class)) {
            filesMock.when(() -> Files.exists(projectPath)).thenReturn(false);

            assertEquals(Optional.empty(), compiler.findCommonJavaSourceRoot(projectPath));
        }
    }

    @Test
    void findCommonJavaSourceRootReturnsProjectPathForJavaProject() throws IOException {
        Path projectPath = mock(Path.class);
        try (var filesMock = mockStatic(Files.class)) {
            filesMock.when(() -> Files.exists(projectPath)).thenReturn(true);
            filesMock.when(() -> Files.exists(projectPath.resolve("pom.xml"))).thenReturn(true);
            assertEquals(Optional.of(projectPath), compiler.findCommonJavaSourceRoot(projectPath));
        }
    }

    @Test
    void compileJavaProjectThrowsExceptionForNonExistentPath() {
        Path projectPath = mock(Path.class);
        try (var filesMock = mockStatic(Files.class)) {
            filesMock.when(() -> Files.exists(projectPath)).thenReturn(false);
            Exception exception = assertThrows(IllegalArgumentException.class, () -> compiler.compileJavaProject(projectPath));
            assertEquals("Project path does not exist: " + projectPath, exception.getMessage());
        }
    }
}