package pwr.zpi.hotspotter.unit.repositoryanalysis.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.model.Commit;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.model.FileChange;
import pwr.zpi.hotspotter.repositoryanalysis.filter.AnalysisFileFilter;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AnalysisFileFilterTest {

    private AnalysisFileFilter analysisFileFilter;

    @BeforeEach
    void setUp() {
        analysisFileFilter = new AnalysisFileFilter();
    }

    @Test
    void shouldIncludeJavaSourceFiles() {
        assertTrue(analysisFileFilter.shouldIncludeFile("src/main/java/com/example/Service.java"));
        assertTrue(analysisFileFilter.shouldIncludeFile("Test.java"));
    }

    @Test
    void shouldIncludePythonSourceFiles() {
        assertTrue(analysisFileFilter.shouldIncludeFile("src/main.py"));
        assertTrue(analysisFileFilter.shouldIncludeFile("scripts/deploy.py"));
    }

    @Test
    void shouldIncludeJavaScriptSourceFiles() {
        assertTrue(analysisFileFilter.shouldIncludeFile("src/index.js"));
        assertTrue(analysisFileFilter.shouldIncludeFile("components/Button.jsx"));
        assertTrue(analysisFileFilter.shouldIncludeFile("App.tsx"));
    }

    @Test
    void shouldExcludeDocumentationFiles() {
        assertFalse(analysisFileFilter.shouldIncludeFile("README.md"));
        assertFalse(analysisFileFilter.shouldIncludeFile("docs/guide.txt"));
        assertFalse(analysisFileFilter.shouldIncludeFile("CHANGELOG.rst"));
        assertFalse(analysisFileFilter.shouldIncludeFile("documentation.adoc"));
    }

    @Test
    void shouldExcludeLicenseAndReadmeFiles() {
        assertFalse(analysisFileFilter.shouldIncludeFile("LICENSE"));
        assertFalse(analysisFileFilter.shouldIncludeFile("README"));
        assertFalse(analysisFileFilter.shouldIncludeFile("CHANGELOG"));
        assertFalse(analysisFileFilter.shouldIncludeFile("CONTRIBUTING"));
        assertFalse(analysisFileFilter.shouldIncludeFile("LICENSE.txt"));
    }

    @Test
    void shouldExcludeDataFiles() {
        assertFalse(analysisFileFilter.shouldIncludeFile("data/config.json"));
        assertFalse(analysisFileFilter.shouldIncludeFile("test.csv"));
        assertFalse(analysisFileFilter.shouldIncludeFile("settings.xml"));
        assertFalse(analysisFileFilter.shouldIncludeFile("config.yaml"));
        assertFalse(analysisFileFilter.shouldIncludeFile("database.yml"));
        assertFalse(analysisFileFilter.shouldIncludeFile("config.toml"));
        assertFalse(analysisFileFilter.shouldIncludeFile("settings.ini"));
        assertFalse(analysisFileFilter.shouldIncludeFile(".env"));
    }

    @Test
    void shouldExcludePackageLockFiles() {
        assertFalse(analysisFileFilter.shouldIncludeFile("package-lock.json"));
        assertFalse(analysisFileFilter.shouldIncludeFile("yarn.lock"));
        assertFalse(analysisFileFilter.shouldIncludeFile("Gemfile.lock"));
        assertFalse(analysisFileFilter.shouldIncludeFile("composer.lock"));
        assertFalse(analysisFileFilter.shouldIncludeFile("poetry.lock"));
        assertFalse(analysisFileFilter.shouldIncludeFile("Cargo.lock"));
    }

    @Test
    void shouldExcludeImageFiles() {
        assertFalse(analysisFileFilter.shouldIncludeFile("logo.png"));
        assertFalse(analysisFileFilter.shouldIncludeFile("images/banner.jpg"));
        assertFalse(analysisFileFilter.shouldIncludeFile("icon.svg"));
        assertFalse(analysisFileFilter.shouldIncludeFile("favicon.ico"));
        assertFalse(analysisFileFilter.shouldIncludeFile("background.gif"));
        assertFalse(analysisFileFilter.shouldIncludeFile("photo.webp"));
    }

    @Test
    void shouldExcludeMediaFiles() {
        assertFalse(analysisFileFilter.shouldIncludeFile("video.mp4"));
        assertFalse(analysisFileFilter.shouldIncludeFile("presentation.avi"));
        assertFalse(analysisFileFilter.shouldIncludeFile("audio.mp3"));
        assertFalse(analysisFileFilter.shouldIncludeFile("sound.wav"));
    }

    @Test
    void shouldExcludeBuildArtifacts() {
        assertFalse(analysisFileFilter.shouldIncludeFile("target/classes/Main.class"));
        assertFalse(analysisFileFilter.shouldIncludeFile("build/libs/app.jar"));
        assertFalse(analysisFileFilter.shouldIncludeFile("out/production/module.war"));
        assertFalse(analysisFileFilter.shouldIncludeFile("bin/app.exe"));
        assertFalse(analysisFileFilter.shouldIncludeFile("lib/native.dll"));
        assertFalse(analysisFileFilter.shouldIncludeFile("native.so"));
        assertFalse(analysisFileFilter.shouldIncludeFile("__pycache__/module.pyc"));
    }

    @Test
    void shouldExcludeMinifiedFiles() {
        assertFalse(analysisFileFilter.shouldIncludeFile("dist/bundle.min.js"));
        assertFalse(analysisFileFilter.shouldIncludeFile("styles.min.css"));
        assertFalse(analysisFileFilter.shouldIncludeFile("app.min.js.map"));
    }

    @Test
    void shouldExcludeArchiveFiles() {
        assertFalse(analysisFileFilter.shouldIncludeFile("backup.zip"));
        assertFalse(analysisFileFilter.shouldIncludeFile("release.tar.gz"));
        assertFalse(analysisFileFilter.shouldIncludeFile("data.7z"));
    }

    @Test
    void shouldExcludeLocalizationDirectories() {
        assertFalse(analysisFileFilter.shouldIncludeFile("i18n/en.json"));
        assertFalse(analysisFileFilter.shouldIncludeFile("l10n/messages.properties"));
        assertFalse(analysisFileFilter.shouldIncludeFile("locale/de/translations.yml"));
        assertFalse(analysisFileFilter.shouldIncludeFile("locales/fr.po"));
        assertFalse(analysisFileFilter.shouldIncludeFile("translations/es.json"));
        assertFalse(analysisFileFilter.shouldIncludeFile("lang/pt.xml"));
    }

    @Test
    void shouldExcludeDependencyDirectories() {
        assertFalse(analysisFileFilter.shouldIncludeFile("node_modules/package/index.js"));
        assertFalse(analysisFileFilter.shouldIncludeFile("vendor/composer/autoload.php"));
        assertFalse(analysisFileFilter.shouldIncludeFile("target/dependency/lib.jar"));
    }

    @Test
    void shouldExcludeIDEFiles() {
        assertFalse(analysisFileFilter.shouldIncludeFile(".idea/workspace.xml"));
        assertFalse(analysisFileFilter.shouldIncludeFile(".vscode/settings.json"));
        assertFalse(analysisFileFilter.shouldIncludeFile("project.iml"));
    }

    @Test
    void shouldExcludeVersionControlFiles() {
        assertFalse(analysisFileFilter.shouldIncludeFile(".git/config"));
        assertFalse(analysisFileFilter.shouldIncludeFile(".svn/entries"));
    }

    @Test
    void shouldIncludeImportantExtensionlessFiles() {
        assertTrue(analysisFileFilter.shouldIncludeFile("Dockerfile"));
        assertTrue(analysisFileFilter.shouldIncludeFile("Makefile"));
        assertTrue(analysisFileFilter.shouldIncludeFile("Jenkinsfile"));
        assertTrue(analysisFileFilter.shouldIncludeFile("Gemfile"));
        assertTrue(analysisFileFilter.shouldIncludeFile("Vagrantfile"));
    }

    @Test
    void shouldExcludeExtensionlessDotfiles() {
        assertFalse(analysisFileFilter.shouldIncludeFile(".gitignore"));
        assertFalse(analysisFileFilter.shouldIncludeFile(".dockerignore"));
        assertFalse(analysisFileFilter.shouldIncludeFile(".npmignore"));
        assertFalse(analysisFileFilter.shouldIncludeFile(".prettierrc"));
        assertFalse(analysisFileFilter.shouldIncludeFile(".eslintrc"));
        assertFalse(analysisFileFilter.shouldIncludeFile(".babelrc"));
        assertFalse(analysisFileFilter.shouldIncludeFile(".editorconfig"));
        assertFalse(analysisFileFilter.shouldIncludeFile(".nvmrc"));
        assertFalse(analysisFileFilter.shouldIncludeFile(".ruby-version"));
        assertFalse(analysisFileFilter.shouldIncludeFile(".python-version"));
        assertFalse(analysisFileFilter.shouldIncludeFile("src/.gitignore"));
        assertFalse(analysisFileFilter.shouldIncludeFile("docs/.prettierrc"));
    }

    @Test
    void shouldHandleNullAndEmptyPaths() {
        assertFalse(analysisFileFilter.shouldIncludeFile(null));
        assertFalse(analysisFileFilter.shouldIncludeFile(""));
    }

    @Test
    void shouldHandleWindowsPathSeparators() {
        assertTrue(analysisFileFilter.shouldIncludeFile("src\\main\\java\\Service.java"));
        assertFalse(analysisFileFilter.shouldIncludeFile("docs\\README.md"));
    }

    @Test
    void shouldFilterCommit() {
        List<FileChange> changes = List.of(
                new FileChange("src/Main.java", 10, 5, null, null),
                new FileChange("README.md", 2, 1, null, null),
                new FileChange("config.json", 3, 0, null, null),
                new FileChange("src/Service.java", 15, 3, null, null)
        );

        Commit originalCommit = new Commit("abc123", "2024-01-01", "John Doe", "john@example.com", changes);
        Commit filteredCommit = analysisFileFilter.filterCommit(originalCommit);

        assertEquals(2, filteredCommit.changedFiles().size());
        assertTrue(filteredCommit.changedFiles().stream()
                .anyMatch(fc -> fc.filePath().equals("src/Main.java")));
        assertTrue(filteredCommit.changedFiles().stream()
                .anyMatch(fc -> fc.filePath().equals("src/Service.java")));
        assertFalse(filteredCommit.changedFiles().stream()
                .anyMatch(fc -> fc.filePath().equals("README.md")));
        assertFalse(filteredCommit.changedFiles().stream()
                .anyMatch(fc -> fc.filePath().equals("config.json")));
    }

    @Test
    void shouldReturnOriginalCommitWhenNoFilesFiltered() {
        List<FileChange> changes = List.of(
                new FileChange("src/Main.java", 10, 5, null, null),
                new FileChange("src/Service.java", 15, 3, null, null)
        );

        Commit originalCommit = new Commit("abc123", "2024-01-01", "John Doe", "john@example.com", changes);
        Commit filteredCommit = analysisFileFilter.filterCommit(originalCommit);

        assertSame(originalCommit, filteredCommit);
    }

    @Test
    void shouldHandleNullCommit() {
        Commit result = analysisFileFilter.filterCommit(null);
        assertNull(result);
    }

    @Test
    void shouldFilterFileNamesSet() {
        Set<String> fileNames = Set.of(
                "src/Main.java",
                "README.md",
                "config.json",
                "src/Service.java",
                "logo.png",
                "test/Test.java"
        );

        Set<String> filtered = analysisFileFilter.filterFileNames(fileNames);

        assertEquals(3, filtered.size());
        assertTrue(filtered.contains("src/Main.java"));
        assertTrue(filtered.contains("src/Service.java"));
        assertTrue(filtered.contains("test/Test.java"));
        assertFalse(filtered.contains("README.md"));
        assertFalse(filtered.contains("config.json"));
        assertFalse(filtered.contains("logo.png"));
    }

    @Test
    void shouldHandleEmptyFileNamesSet() {
        Set<String> empty = Set.of();
        Set<String> result = analysisFileFilter.filterFileNames(empty);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldHandleNullFileNamesSet() {
        Set<String> result = analysisFileFilter.filterFileNames(null);
        assertNull(result);
    }

    @Test
    void shouldBeCaseInsensitive() {
        assertFalse(analysisFileFilter.shouldIncludeFile("README.MD"));
        assertFalse(analysisFileFilter.shouldIncludeFile("Config.JSON"));
        assertFalse(analysisFileFilter.shouldIncludeFile("Image.PNG"));
    }

    @Test
    void shouldHandleNestedPaths() {
        assertTrue(analysisFileFilter.shouldIncludeFile("src/main/java/com/example/deep/nested/Service.java"));
        assertFalse(analysisFileFilter.shouldIncludeFile("docs/api/guide/tutorial.md"));
        assertFalse(analysisFileFilter.shouldIncludeFile("src/main/resources/i18n/messages/en_US.properties"));
    }

}
