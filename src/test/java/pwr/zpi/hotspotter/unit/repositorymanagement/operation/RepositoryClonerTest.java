package pwr.zpi.hotspotter.unit.repositorymanagement.operation;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.GitCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import pwr.zpi.hotspotter.repositorymanagement.config.RepositoryManagementConfig;
import pwr.zpi.hotspotter.common.exception.RepositoryCloneException;
import pwr.zpi.hotspotter.repositorymanagement.operation.RepositoryCloner;
import pwr.zpi.hotspotter.repositorymanagement.parser.RepositoryUrlParser;
import pwr.zpi.hotspotter.repositorymanagement.repository.RepositoryInfoRepository;
import pwr.zpi.hotspotter.repositorymanagement.storage.DiskSpaceManager;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepositoryClonerTest {

    @Mock
    private DiskSpaceManager diskSpaceManager;
    @Mock
    private RepositoryManagementConfig config;
    @Mock
    private RepositoryInfoRepository repositoryInfoRepository;

    @InjectMocks
    private RepositoryCloner cloner;

    private RepositoryUrlParser.RepositoryData repoData;
    private final Path expectedLocalPath = Path.of("/tmp/repositories/test/test");

    @BeforeEach
    void setup() {
        repoData = new RepositoryUrlParser.RepositoryData(
                "https://github.com/test/test.git",
                "test/test",
                "test",
                "test"
        );

        when(config.getBaseDirectory()).thenReturn("/tmp/repositories");
    }

    private CloneCommand mockCloneSuccess() throws Exception {
        CloneCommand clone = mock(CloneCommand.class);
        Git git = mock(Git.class);

        when(clone.setURI(any())).thenReturn(clone);
        when(clone.setDirectory(any())).thenReturn(clone);
        when(clone.setProgressMonitor(any())).thenReturn(clone);
        when(clone.setCloneAllBranches(anyBoolean())).thenReturn(clone);
        when(clone.call()).thenReturn(git);

        return clone;
    }

    @Test
    void throwsException_WhenInsufficientDiskSpace() {
        when(diskSpaceManager.ensureEnoughFreeSpace()).thenReturn(false);

        assertThrows(RepositoryCloneException.class, () ->
                cloner.clone(repoData)
        );

        verify(diskSpaceManager).deleteRepositoryDirectory(any(File.class), anyString());
    }

    @Test
    void throwsException_WhenDirectoryCreationFails() {
        when(config.getCloneMonitoringIntervalPercentage()).thenReturn(10);
        when(diskSpaceManager.ensureEnoughFreeSpace()).thenReturn(true);

        File badDir = expectedLocalPath.toFile();
        badDir.setWritable(false);

        assertThrows(RepositoryCloneException.class, () ->
                cloner.clone(repoData)
        );
    }

    @Test
    void throwsException_WhenCleanDirectoryFails() {
        when(diskSpaceManager.ensureEnoughFreeSpace()).thenReturn(false);

        File goodDir = expectedLocalPath.toFile();
        goodDir.mkdirs();

        doThrow(new RuntimeException("clean fail"))
                .when(diskSpaceManager).deleteRepositoryDirectory(any(File.class), anyString());

        assertThrows(RuntimeException.class, () ->
                cloner.clone(repoData)
        );
    }

    @Test
    void throwsException_WhenGitCloneThrowsGitAPIException() throws Exception {
        when(config.getCloneMonitoringIntervalPercentage()).thenReturn(10);
        when(diskSpaceManager.ensureEnoughFreeSpace()).thenReturn(true);

        CloneCommand clone = mock(CloneCommand.class);
        when(clone.setURI(any())).thenReturn(clone);
        when(clone.setDirectory(any())).thenReturn(clone);
        when(clone.setProgressMonitor(any())).thenReturn(clone);
        when(clone.setCloneAllBranches(anyBoolean())).thenReturn(clone);
        when(clone.call()).thenThrow(new GitAPIException("clone failed") {});

        try (var gitMock = Mockito.mockStatic(Git.class)) {
            gitMock.when(Git::cloneRepository).thenReturn(clone);

            assertThrows(RepositoryCloneException.class, () ->
                    cloner.clone(repoData)
            );

            verify(diskSpaceManager).deleteRepositoryDirectory(any(), anyString());
        }
    }

    @Test
    void throwsException_WhenGitCloneThrowsUnexpectedException() throws Exception {
        when(config.getCloneMonitoringIntervalPercentage()).thenReturn(10);
        when(diskSpaceManager.ensureEnoughFreeSpace()).thenReturn(true);

        CloneCommand clone = mock(CloneCommand.class);
        when(clone.setURI(any())).thenReturn(clone);
        when(clone.setDirectory(any())).thenReturn(clone);
        when(clone.setProgressMonitor(any())).thenReturn(clone);
        when(clone.setCloneAllBranches(anyBoolean())).thenReturn(clone);
        when(clone.call()).thenThrow(new RuntimeException("unexpected error"));

        try (var gitMock = Mockito.mockStatic(Git.class)) {
            gitMock.when(Git::cloneRepository).thenReturn(clone);
            assertThrows(RepositoryCloneException.class, () ->
                    cloner.clone(repoData)
            );
            verify(diskSpaceManager).deleteRepositoryDirectory(any(), anyString());
        }
    }

    @Test
    void throwsException_WhenRepositoryIsInvalidAfterClone() throws Exception {
        when(config.getCloneMonitoringIntervalPercentage()).thenReturn(10);
        when(diskSpaceManager.ensureEnoughFreeSpace()).thenReturn(true);

        GitCommand<Git> clone = mockCloneSuccess();

        File gitFolder = expectedLocalPath.resolve(".git").toFile();
        gitFolder.delete();

        try (var gitMock = mockStatic(Git.class)) {
            gitMock.when(Git::cloneRepository).thenReturn(clone);
            gitFolder.mkdirs();

            assertThrows(RepositoryCloneException.class, () -> cloner.clone(repoData));

            verify(diskSpaceManager).deleteRepositoryDirectory(any(), anyString());
        }
    }
}
