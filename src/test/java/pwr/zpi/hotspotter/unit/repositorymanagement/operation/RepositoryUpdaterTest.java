package pwr.zpi.hotspotter.unit.repositorymanagement.operation;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PullResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import pwr.zpi.hotspotter.repositorymanagement.config.RepositoryManagementConfig;
import pwr.zpi.hotspotter.common.exception.RepositoryUpdateException;
import pwr.zpi.hotspotter.repositorymanagement.model.RepositoryInfo;
import pwr.zpi.hotspotter.repositorymanagement.operation.RepositoryUpdater;
import pwr.zpi.hotspotter.repositorymanagement.repository.RepositoryInfoRepository;
import pwr.zpi.hotspotter.repositorymanagement.storage.DiskSpaceManager;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepositoryUpdaterTest {

    @Mock
    RepositoryManagementConfig config;
    @Mock
    RepositoryInfoRepository repositoryInfoRepository;
    @Mock
    DiskSpaceManager diskSpaceManager;

    @InjectMocks
    RepositoryUpdater updater;

    private static final String LOCAL_PATH = "/tmp/repo";

    private RepositoryInfo repo() {
        RepositoryInfo info = new RepositoryInfo();
        info.setLocalPath(LOCAL_PATH);
        info.setAccessCount(0);
        return info;
    }

    @Test
    void update_ReturnsUpdatedRepository_WhenPullSuccessful() throws Exception {

        when(diskSpaceManager.ensureEnoughFreeSpace()).thenReturn(true);
        when(config.getUpdateMonitoringIntervalPercentage()).thenReturn(10);

        PullResult pullResult = mock(PullResult.class);
        when(pullResult.isSuccessful()).thenReturn(true);

        Git git = mock(Git.class);
        PullCommand pullCommand = mock(PullCommand.class);

        when(git.pull()).thenReturn(pullCommand);
        when(pullCommand.setProgressMonitor(any())).thenReturn(pullCommand);
        when(pullCommand.setRemote("origin")).thenReturn(pullCommand);
        when(pullCommand.call()).thenReturn(pullResult);

        try (MockedStatic<Git> gitMock = mockStatic(Git.class);
             MockedStatic<FileUtils> fileUtils = mockStatic(FileUtils.class)) {

            gitMock.when(() -> Git.open(Path.of(LOCAL_PATH).toFile())).thenReturn(git);
            fileUtils.when(() -> FileUtils.sizeOfDirectory(any())).thenReturn(1234L);

            RepositoryInfo result = updater.update(repo());

            assertNotNull(result);
            verify(repositoryInfoRepository).save(any());
        }
    }

    @Test
    void update_ThrowsException_WhenPullResultUnsuccessful() throws Exception {

        when(diskSpaceManager.ensureEnoughFreeSpace()).thenReturn(true);
        when(config.getUpdateMonitoringIntervalPercentage()).thenReturn(10);

        PullResult pullResult = mock(PullResult.class);
        when(pullResult.isSuccessful()).thenReturn(false);

        Git git = mock(Git.class);
        PullCommand pullCommand = mock(PullCommand.class);

        when(git.pull()).thenReturn(pullCommand);
        when(pullCommand.setProgressMonitor(any())).thenReturn(pullCommand);
        when(pullCommand.setRemote("origin")).thenReturn(pullCommand);
        when(pullCommand.call()).thenReturn(pullResult);

        try (MockedStatic<Git> gitMock = mockStatic(Git.class)) {

            gitMock.when(() -> Git.open(Path.of(LOCAL_PATH).toFile())).thenReturn(git);

            assertThrows(RepositoryUpdateException.class, () -> updater.update(repo()));
        }
    }

    @Test
    void update_ThrowsException_WhenDiskSpaceInsufficient() {
        when(diskSpaceManager.ensureEnoughFreeSpace()).thenReturn(false);

        assertThrows(RepositoryUpdateException.class, () -> updater.update(repo()));
    }

    @Test
    void update_ThrowsException_WhenGitApiExceptionOccurs() {

        when(diskSpaceManager.ensureEnoughFreeSpace()).thenReturn(true);

        try (MockedStatic<Git> gitMock = mockStatic(Git.class)) {
            gitMock.when(() -> Git.open(any())).thenThrow(new IOException("fail"));

            assertThrows(RepositoryUpdateException.class, () -> updater.update(repo()));
        }
    }

    @Test
    void update_ThrowsException_WhenIOExceptionOccurs() {

        when(diskSpaceManager.ensureEnoughFreeSpace()).thenReturn(true);

        try (MockedStatic<Git> gitMock = mockStatic(Git.class)) {
            gitMock.when(() -> Git.open(any())).thenThrow(new IOException("io fail"));

            assertThrows(RepositoryUpdateException.class, () -> updater.update(repo()));
        }
    }

    @Test
    void update_ThrowsException_WhenUnexpectedExceptionOccurs() {

        when(diskSpaceManager.ensureEnoughFreeSpace()).thenReturn(true);

        try (MockedStatic<Git> gitMock = mockStatic(Git.class)) {
            gitMock.when(() -> Git.open(any())).thenThrow(new RuntimeException("boom"));

            assertThrows(RepositoryUpdateException.class, () -> updater.update(repo()));
        }
    }
}
