package pwr.zpi.hotspotter.unit.repositorymanagement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import pwr.zpi.hotspotter.repositorymanagement.model.RepositoryInfo;
import pwr.zpi.hotspotter.repositorymanagement.operation.RepositoryOperationQueue;

import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepositoryOperationQueueTest {

    private RepositoryOperationQueue repositoryOperationQueue;

    @BeforeEach
    void setUp() {
        repositoryOperationQueue = new RepositoryOperationQueue();
    }

    @Test
    void executesOperationSuccessfullyWhenLockIsAcquired() {
        String repositoryUrl = "https://example.com/repo.git";
        RepositoryInfo expectedInfo = mock(RepositoryInfo.class);
        Supplier<RepositoryInfo> operation = () -> expectedInfo;

        RepositoryInfo result = repositoryOperationQueue.executeOperation(repositoryUrl, operation);

        assertEquals(expectedInfo, result);
    }

    @Test
    void releasesLockAfterOperationExecution() {
        String repositoryUrl = "https://example.com/repo.git";
        Supplier<RepositoryInfo> operation = mock(Supplier.class);
        when(operation.get()).thenReturn(mock(RepositoryInfo.class));

        repositoryOperationQueue.executeOperation(repositoryUrl, operation);

        Lock lock = repositoryOperationQueue.getRepositoryLocks().get(repositoryUrl);
        assertNull(lock);
    }

    @Test
    void removesLockWhenNoOtherThreadsAreWaiting() {
        String repositoryUrl = "https://example.com/repo.git";
        Supplier<RepositoryInfo> operation = mock(Supplier.class);
        when(operation.get()).thenReturn(mock(RepositoryInfo.class));

        repositoryOperationQueue.executeOperation(repositoryUrl, operation);

        assertNull(repositoryOperationQueue.getRepositoryLocks().get(repositoryUrl));
    }
}
