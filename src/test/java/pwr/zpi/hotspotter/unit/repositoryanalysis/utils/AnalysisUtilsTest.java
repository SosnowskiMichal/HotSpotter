package pwr.zpi.hotspotter.unit.repositoryanalysis.utils;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.api.Test;
import org.springframework.data.repository.CrudRepository;
import pwr.zpi.hotspotter.repositoryanalysis.util.AnalysisUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AnalysisUtilsTest {

    @Test
    void savesDataInCorrectBatchSizes() {
        CrudRepository<Object, Long> repo = mock(CrudRepository.class);

        List<Object> data = List.of(new Object(), new Object(), new Object(), new Object(), new Object());

        AnalysisUtils.saveDataInBatches(repo, data, 2);

        verify(repo, times(3)).saveAll(any());
    }

    @Test
    void savesDataInSingleBatchIfSizeSmallerThanBatch() {
        CrudRepository<Object, Long> repo = mock(CrudRepository.class);

        List<Object> data = List.of(new Object(), new Object());

        AnalysisUtils.saveDataInBatches(repo, data, 10);

        verify(repo, times(1)).saveAll(any());
    }

    @Test
    void doesNothingIfRepositoryIsNull() {
        List<Object> data = List.of(new Object());
        assertThatCode(() -> AnalysisUtils.saveDataInBatches(null, data, 3))
                .doesNotThrowAnyException();
    }

    @Test
    void doesNothingIfDataIsNull() {
        CrudRepository<Object, Long> repo = mock(CrudRepository.class);
        assertThatCode(() -> AnalysisUtils.saveDataInBatches(repo, null, 3))
                .doesNotThrowAnyException();

        verify(repo, never()).saveAll(any());
    }

    @Test
    void doesNothingForEmptyData() {
        CrudRepository<Object, Long> repo = mock(CrudRepository.class);

        AnalysisUtils.saveDataInBatches(repo, List.of(), 2);

        verify(repo, never()).saveAll(any());
    }

    @Test
    void throwsExceptionForBatchSizeBelowOne() {
        CrudRepository<Object, Long> repo = mock(CrudRepository.class);

        assertThatThrownBy(() -> AnalysisUtils.saveDataInBatches(repo, List.of(), 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Batch size must be at least 1");
    }

    @Test
    void returnsFileNamesFromGitRepository() throws Exception {
        Path mockPath = Path.of("/fake/repo");

        Git git = mock(Git.class);
        Repository repo = mock(Repository.class);
        DirCache dirCache = mock(DirCache.class);
        when(git.getRepository()).thenReturn(repo);
        when(repo.readDirCache()).thenReturn(dirCache);
        DirCacheEntry entry1 = mock(DirCacheEntry.class);
        DirCacheEntry entry2 = mock(DirCacheEntry.class);

        when(dirCache.getEntryCount()).thenReturn(2);
        when(dirCache.getEntry(0)).thenReturn(entry1);
        when(dirCache.getEntry(1)).thenReturn(entry2);

        when(entry1.getPathString()).thenReturn("src/Main.java");
        when(entry2.getPathString()).thenReturn("README.md");

        try (var gitMock = mockStatic(Git.class)) {
            gitMock.when(() -> Git.open(mockPath.toFile())).thenReturn(git);
            Set<String> result = AnalysisUtils.getExistingFileNames(mockPath);
            assertThat(result)
                    .containsExactlyInAnyOrder("src/Main.java", "README.md");
        }
    }

    @Test
    void returnsEmptySetWhenIOExceptionIsThrown() {
        Path mockPath = Path.of("/fake/repo");

        try (var gitMock = mockStatic(Git.class)) {
            gitMock.when(() -> Git.open(mockPath.toFile()))
                    .thenThrow(new IOException("fail"));

            Set<String> result = AnalysisUtils.getExistingFileNames(mockPath);

            assertThat(result).isEmpty();
        }
    }
}
