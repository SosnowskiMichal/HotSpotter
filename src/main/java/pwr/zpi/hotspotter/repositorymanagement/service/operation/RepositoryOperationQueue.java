package pwr.zpi.hotspotter.repositorymanagement.service.operation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.repositorymanagement.service.RepositoryManagementService;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

@Slf4j
@Component
public class RepositoryOperationQueue {

    private final ConcurrentHashMap<String, Lock> repositoryLocks = new ConcurrentHashMap<>();

    public RepositoryManagementService.RepositoryOperationResult executeOperation(
            String repositoryUrl,
            Supplier<RepositoryManagementService.RepositoryOperationResult> operation) {

        Lock lock = repositoryLocks.computeIfAbsent(repositoryUrl, url -> new ReentrantLock());

        log.debug("Acquiring lock for repository URL: {}", repositoryUrl);
        lock.lock();

        try {
            log.debug("Lock acquired for repository URL: {}", repositoryUrl);
            return operation.get();

        } finally {
            log.debug("Releasing lock for repository URL: {}", repositoryUrl);
            lock.unlock();

            if (lock.tryLock()) {
                try {
                    repositoryLocks.remove(repositoryUrl, lock);
                } finally {
                    lock.unlock();
                }
            }
        }
    }

}
