package pwr.zpi.hotspotter.unit.repositorymanagement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pwr.zpi.hotspotter.repositorymanagement.exception.InvalidRepositoryUrlException;
import pwr.zpi.hotspotter.repositorymanagement.model.RepositoryInfo;
import pwr.zpi.hotspotter.repositorymanagement.operation.RepositoryCloner;
import pwr.zpi.hotspotter.repositorymanagement.operation.RepositoryOperationQueue;
import pwr.zpi.hotspotter.repositorymanagement.operation.RepositoryUpdater;
import pwr.zpi.hotspotter.repositorymanagement.parser.RepositoryUrlParser;
import pwr.zpi.hotspotter.repositorymanagement.repository.RepositoryInfoRepository;
import pwr.zpi.hotspotter.repositorymanagement.service.RepositoryManagementService;
import pwr.zpi.hotspotter.repositorymanagement.storage.DiskSpaceManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepositoryManagementServiceTest {

    @Mock
    private RepositoryUrlParser repositoryUrlParser;
    @Mock
    private RepositoryInfoRepository repositoryInfoRepository;
    @Mock
    private RepositoryCloner repositoryCloner;
    @Mock
    private RepositoryUpdater repositoryUpdater;
    @Mock
    private RepositoryOperationQueue repositoryOperationQueue;
    @Mock
    private DiskSpaceManager diskSpaceManager;

    @InjectMocks
    private RepositoryManagementService repositoryManagementService;

    @Test
    void cloneOrUpdateRepositoryWithNonexistentRepositoryClonesSuccessfully() {
        String repositoryUrl = "https://github.com/user/repo";
        RepositoryUrlParser.RepositoryData repositoryData = new RepositoryUrlParser.RepositoryData(repositoryUrl, "github", "user", "repo");
        RepositoryInfo clonedRepositoryInfo = new RepositoryInfo();

        when(repositoryUrlParser.parse(repositoryUrl)).thenReturn(repositoryData);
        when(repositoryInfoRepository.findByNameAndOwnerAndPlatform("repo", "user", "github"))
                .thenReturn(Optional.empty());
        when(repositoryCloner.clone(repositoryData)).thenReturn(clonedRepositoryInfo);
        when(repositoryOperationQueue.executeOperation(eq(repositoryUrl), any()))
                .thenAnswer(invocation -> ((Supplier<RepositoryInfo>) invocation.getArgument(1)).get());

        RepositoryInfo result = repositoryManagementService.cloneOrUpdateRepository(repositoryUrl);

        assertEquals(clonedRepositoryInfo, result);
        verify(repositoryCloner).clone(repositoryData);
    }

    @Test
    void cloneOrUpdateRepositoryWithValidRepositoryUpdatesSuccessfully() {
        String repositoryUrl = "https://github.com/user/repo";
        Path localPath = Path.of("/path/to/repo");
        RepositoryUrlParser.RepositoryData repositoryData = new RepositoryUrlParser.RepositoryData(repositoryUrl, "github", "user", "repo");
        RepositoryInfo existingRepositoryInfo = new RepositoryInfo();
        existingRepositoryInfo.setLocalPath(localPath.toString());
        RepositoryInfo updatedRepositoryInfo = new RepositoryInfo();

        when(repositoryUrlParser.parse(repositoryUrl)).thenReturn(repositoryData);
        when(repositoryInfoRepository.findByNameAndOwnerAndPlatform("repo", "user", "github"))
                .thenReturn(Optional.of(existingRepositoryInfo));
        when(repositoryUpdater.update(existingRepositoryInfo)).thenReturn(updatedRepositoryInfo);
        when(repositoryOperationQueue.executeOperation(eq(repositoryUrl), any()))
                .thenAnswer(invocation -> ((Supplier<RepositoryInfo>) invocation.getArgument(1)).get());

        RepositoryManagementService spyService = spy(repositoryManagementService);
        doReturn(true).when(spyService).isValidGitRepository(localPath);

        RepositoryInfo result;
        try (var filesMock = mockStatic(Files.class)) {
            filesMock.when(() -> Files.exists(any())).thenReturn(true);
            filesMock.when(() -> Files.isReadable(any())).thenReturn(true);
            filesMock.when(() -> Files.isWritable(any())).thenReturn(true);

            result = spyService.cloneOrUpdateRepository(repositoryUrl);
        }

        assertEquals(updatedRepositoryInfo, result);
        verify(repositoryUpdater).update(existingRepositoryInfo);
    }


    @Test
    void cloneOrUpdateRepositoryWithCorruptedRepositoryCleansUpAndClones() {
        String repositoryUrl = "https://github.com/user/repo";
        RepositoryUrlParser.RepositoryData repositoryData = new RepositoryUrlParser.RepositoryData(repositoryUrl, "github", "user", "repo");
        RepositoryInfo corruptedRepositoryInfo = new RepositoryInfo();
        corruptedRepositoryInfo.setLocalPath("/invalid/path");
        RepositoryInfo clonedRepositoryInfo = new RepositoryInfo();

        when(repositoryUrlParser.parse(repositoryUrl)).thenReturn(repositoryData);
        when(repositoryInfoRepository.findByNameAndOwnerAndPlatform("repo", "user", "github"))
                .thenReturn(Optional.of(corruptedRepositoryInfo));
        when(repositoryCloner.clone(repositoryData)).thenReturn(clonedRepositoryInfo);
        when(repositoryOperationQueue.executeOperation(eq(repositoryUrl), any()))
                .thenAnswer(invocation -> ((Supplier<RepositoryInfo>) invocation.getArgument(1)).get());
        doNothing().when(repositoryInfoRepository).delete(corruptedRepositoryInfo);
        when(diskSpaceManager.deleteRepositoryDirectory(any())).thenReturn(true);

        RepositoryInfo result = repositoryManagementService.cloneOrUpdateRepository(repositoryUrl);

        assertEquals(clonedRepositoryInfo, result);
        verify(repositoryInfoRepository).delete(corruptedRepositoryInfo);
        verify(diskSpaceManager).deleteRepositoryDirectory(any());
        verify(repositoryCloner).clone(repositoryData);
    }

    @Test
    void cloneOrUpdateRepositoryWithInvalidUrlThrowsException() {
        String repositoryUrl = "invalid-url";

        when(repositoryUrlParser.parse(repositoryUrl)).thenThrow(new InvalidRepositoryUrlException("Invalid URL"));
        when(repositoryOperationQueue.executeOperation(eq(repositoryUrl), any()))
                .thenAnswer(invocation -> ((Supplier<RepositoryInfo>) invocation.getArgument(1)).get());

        assertThrows(InvalidRepositoryUrlException.class, () -> repositoryManagementService.cloneOrUpdateRepository(repositoryUrl));
        verify(repositoryUrlParser).parse(repositoryUrl);
        verifyNoInteractions(repositoryInfoRepository, repositoryCloner, repositoryUpdater);
        verify(repositoryOperationQueue).executeOperation(eq(repositoryUrl), any());
    }

}
