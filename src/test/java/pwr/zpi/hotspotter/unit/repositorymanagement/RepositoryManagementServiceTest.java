package pwr.zpi.hotspotter.unit.repositorymanagement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void cloneOrUpdateRepository_shouldCloneRepositoryWhenNonexistent() {
        String repositoryUrl = "https://github.com/user/repo";
        RepositoryUrlParser.RepositoryData repositoryData = new RepositoryUrlParser.RepositoryData(repositoryUrl, "github", "user", "repo");
        RepositoryInfo repositoryInfo = new RepositoryInfo();

        when(repositoryUrlParser.parse(repositoryUrl)).thenReturn(repositoryData);
        when(repositoryInfoRepository.findByNameAndOwnerAndPlatform("repo", "user", "github"))
                .thenReturn(Optional.empty());
        when(repositoryCloner.clone(repositoryData)).thenReturn(repositoryInfo);
        when(repositoryOperationQueue.executeOperation(eq(repositoryUrl), any())).thenAnswer(invocation -> {
            Object operation = invocation.getArgument(1);
            try {
                java.lang.reflect.Method target = null;
                for (java.lang.reflect.Method m : operation.getClass().getDeclaredMethods()) {
                    if (m.getParameterCount() == 0 && RepositoryInfo.class.isAssignableFrom(m.getReturnType())) {
                        target = m;
                        break;
                    }
                }
                if (target == null) {
                    target = operation.getClass().getMethod("call");
                }
                target.setAccessible(true);
                return target.invoke(operation);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });

        RepositoryInfo result = repositoryManagementService.cloneOrUpdateRepository(repositoryUrl);

        assertEquals(repositoryInfo, result);
        verify(repositoryCloner).clone(repositoryData);
    }

    @Test
    void cloneOrUpdateRepository_shouldUpdateRepositoryWhenValid() {
        String repositoryUrl = "https://github.com/user/repo";
        RepositoryUrlParser.RepositoryData repositoryData = new RepositoryUrlParser.RepositoryData(repositoryUrl, "github", "user", "repo");
        RepositoryInfo repositoryInfo = new RepositoryInfo();
        repositoryInfo.setLocalPath("/path/to/repo");

        when(repositoryUrlParser.parse(repositoryUrl)).thenReturn(repositoryData);
        when(repositoryInfoRepository.findByNameAndOwnerAndPlatform("repo", "user", "github"))
                .thenReturn(Optional.of(repositoryInfo));
        when(repositoryUpdater.update(repositoryInfo)).thenReturn(repositoryInfo);
        when(repositoryOperationQueue.executeOperation(eq(repositoryUrl), any())).thenAnswer(invocation -> {
            Object operation = invocation.getArgument(1);
            try {
                java.lang.reflect.Method target = null;
                for (java.lang.reflect.Method m : operation.getClass().getDeclaredMethods()) {
                    if (m.getParameterCount() == 0 && RepositoryInfo.class.isAssignableFrom(m.getReturnType())) {
                        target = m;
                        break;
                    }
                }
                if (target == null) {
                    target = operation.getClass().getMethod("call");
                }
                target.setAccessible(true);
                return target.invoke(operation);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });

        RepositoryInfo result = repositoryManagementService.cloneOrUpdateRepository(repositoryUrl);

        assertEquals(repositoryInfo, result);
        verify(repositoryUpdater).update(repositoryInfo);
    }

    @Test
    void cloneOrUpdateRepository_shouldCleanupAndCloneWhenCorrupted() {
        String repositoryUrl = "https://github.com/user/repo";
        RepositoryUrlParser.RepositoryData repositoryData = new RepositoryUrlParser.RepositoryData(repositoryUrl, "github", "user", "repo");
        RepositoryInfo repositoryInfo = new RepositoryInfo();
        repositoryInfo.setLocalPath("/path/to/repo");

        when(repositoryUrlParser.parse(repositoryUrl)).thenReturn(repositoryData);
        when(repositoryInfoRepository.findByNameAndOwnerAndPlatform("repo", "user", "github"))
                .thenReturn(Optional.of(repositoryInfo));
        when(repositoryCloner.clone(repositoryData)).thenReturn(repositoryInfo);
        when(repositoryOperationQueue.executeOperation(eq(repositoryUrl), any())).thenAnswer(invocation -> {
            Object operation = invocation.getArgument(1);
            try {
                java.lang.reflect.Method target = null;
                for (java.lang.reflect.Method m : operation.getClass().getDeclaredMethods()) {
                    if (m.getParameterCount() == 0 && RepositoryInfo.class.isAssignableFrom(m.getReturnType())) {
                        target = m;
                        break;
                    }
                }
                if (target == null) {
                    target = operation.getClass().getMethod("call");
                }
                target.setAccessible(true);
                return target.invoke(operation);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });

        doAnswer(invocation -> {
            Path path = Path.of(repositoryInfo.getLocalPath());
            Files.deleteIfExists(path);
            return null;
        }).when(diskSpaceManager).deleteRepositoryDirectory(any());

        RepositoryInfo result = repositoryManagementService.cloneOrUpdateRepository(repositoryUrl);

        assertEquals(repositoryInfo, result);
        verify(diskSpaceManager).deleteRepositoryDirectory(any());
        verify(repositoryCloner).clone(repositoryData);
    }
}

