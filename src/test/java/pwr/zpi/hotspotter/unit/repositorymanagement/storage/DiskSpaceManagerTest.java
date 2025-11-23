package pwr.zpi.hotspotter.unit.repositorymanagement.storage;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import pwr.zpi.hotspotter.repositoryanalysis.queue.RepositoryAnalysisQueue;
import pwr.zpi.hotspotter.repositorymanagement.config.RepositoryManagementConfig;
import pwr.zpi.hotspotter.repositorymanagement.model.RepositoryInfo;
import pwr.zpi.hotspotter.repositorymanagement.repository.RepositoryInfoRepository;
import pwr.zpi.hotspotter.repositorymanagement.storage.DiskSpaceManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiskSpaceManagerTest {

    @Mock
    private RepositoryInfoRepository repositoryInfoRepository;
    @Mock
    private RepositoryManagementConfig repositoryManagementConfig;
    @Mock
    private RepositoryAnalysisQueue repositoryAnalysisQueue;

    @InjectMocks
    private DiskSpaceManager diskSpaceManager;

    @Test
    void ensureEnoughFreeSpaceReturnsTrueWhenSpaceIsSufficient() throws IOException {
        when(repositoryManagementConfig.getMinFreeSpaceInBytes()).thenReturn(100L);
        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            FileStore fileStoreMock = mock(FileStore.class);
            filesMock.when(() -> Files.getFileStore(any(Path.class))).thenReturn(fileStoreMock);
            when(fileStoreMock.getUsableSpace()).thenReturn(200L);
            when(repositoryManagementConfig.getBaseDirectory()).thenReturn("/base/dir");

            boolean result = diskSpaceManager.ensureEnoughFreeSpace();

            assertTrue(result);
        }
    }

    @Test
    void ensureEnoughFreeSpaceInitiatesCleanupWhenSpaceIsInsufficient() throws IOException {
        when(repositoryManagementConfig.getMinFreeSpaceInBytes()).thenReturn(200L);
        when(repositoryManagementConfig.getCleanupStrategy()).thenReturn(RepositoryManagementConfig.CleanupStrategy.LEAST_RECENTLY_USED);
        when(repositoryInfoRepository.findAllByOrderByLastAccessedAtAsc()).thenReturn(List.of(
                new RepositoryInfo("1", "/repo1", null, null, null, "/local/path", null, LocalDateTime.now(), 100, 100L),
                new RepositoryInfo("2", "/repo2", null, null, null, "/local/path", null, LocalDateTime.now(), 150, 500L),
                new RepositoryInfo("3", "/repo3", null, null, null, "/local/path", null, LocalDateTime.now(), 200, 100L)
        ));
        when(repositoryManagementConfig.getBaseDirectory()).thenReturn("/base/dir");

        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            FileStore fileStoreMock = mock(FileStore.class);
            filesMock.when(() -> Files.getFileStore(any(Path.class))).thenReturn(fileStoreMock);
            when(fileStoreMock.getUsableSpace()).thenReturn(50L);

            DiskSpaceManager spyManager = spy(diskSpaceManager);
            doReturn(true).when(spyManager).deleteRepositoryDirectory(any(File.class), anyString());

            boolean result = spyManager.ensureEnoughFreeSpace();

            assertTrue(result);
            verify(repositoryInfoRepository, times(2)).delete(any());
        }
    }

    @Test
    void ensureEnoughFreeSpaceFailsWhenCleanupDoesNotFreeEnoughSpace() throws IOException {
        when(repositoryManagementConfig.getMinFreeSpaceInBytes()).thenReturn(300L);
        when(repositoryManagementConfig.getCleanupStrategy()).thenReturn(RepositoryManagementConfig.CleanupStrategy.LEAST_RECENTLY_USED);
        when(repositoryInfoRepository.findAllByOrderByLastAccessedAtAsc()).thenReturn(List.of(
                new RepositoryInfo("1", "/repo1", null, null, null, "local/path", null, LocalDateTime.now(), 100, 100L)
        ));
        when(repositoryManagementConfig.getBaseDirectory()).thenReturn("/base/dir");
        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            FileStore fileStoreMock = mock(FileStore.class);
            filesMock.when(() -> Files.getFileStore(any(Path.class))).thenReturn(fileStoreMock);
            when(fileStoreMock.getUsableSpace()).thenReturn(50L);

            DiskSpaceManager spyManager = spy(diskSpaceManager);
            doReturn(true).when(spyManager).deleteRepositoryDirectory(any(File.class), anyString());

            boolean result = spyManager.ensureEnoughFreeSpace();

            assertFalse(result);
            verify(repositoryInfoRepository, times(1)).delete(any());
        }
    }

    @Test
    void deleteRepositoryDirectoryReturnsTrueWhenDirectoryIsDeleted() {
        File directory = mock(File.class);
        when(directory.exists()).thenReturn(true);

        try (MockedStatic<FileUtils> fileUtilsMock =
                     mockStatic(FileUtils.class)) {
            fileUtilsMock.when(() -> FileUtils.deleteDirectory(directory)).thenAnswer(_ -> null);

            boolean result = diskSpaceManager.deleteRepositoryDirectory(directory, anyString());

            assertTrue(result);
        }
    }

    @Test
    void deleteRepositoryDirectoryReturnsFalseWhenDirectoryDoesNotExist() {
        File directory = mock(File.class);
        when(directory.exists()).thenReturn(false);

        boolean result = diskSpaceManager.deleteRepositoryDirectory(directory, anyString());

        assertFalse(result);
    }

    @Test
    void deleteRepositoryDirectoryLogsErrorWhenDeletionFails() {
        File directory = mock(File.class);
        when(directory.exists()).thenReturn(true);

        try (MockedStatic<FileUtils> fileUtilsMock = mockStatic(FileUtils.class)) {
            fileUtilsMock.when(() -> FileUtils.deleteDirectory(directory)).thenThrow(new IOException("Deletion failed"));

            boolean result = diskSpaceManager.deleteRepositoryDirectory(directory, anyString());

            assertFalse(result);
        }
    }
}
