package pwr.zpi.hotspotter.repositorymanagement.service.operation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.repositorymanagement.model.RepositoryInfo;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

@Slf4j
@Component
public class RepositoryOperationQueue {

    private final ConcurrentHashMap<String, Lock> repositoryLocks = new ConcurrentHashMap<>();

    public RepositoryInfo executeOperation(
            String repositoryUrl,
            Supplier<RepositoryInfo> operation) {

        Lock lock = repositoryLocks.computeIfAbsent(repositoryUrl, _ -> new ReentrantLock());
        lock.lock();

        try {
            return operation.get();

        } finally {
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
