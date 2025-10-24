package pwr.zpi.hotspotter.repositorymanagement.service.storage;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.repositorymanagement.config.RepositoryManagementConfig;
import pwr.zpi.hotspotter.repositorymanagement.model.RepositoryInfo;
import pwr.zpi.hotspotter.repositorymanagement.repository.RepositoryInfoRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
@Component
@AllArgsConstructor
public class DiskSpaceManager {

    private static final int MAX_CLEANUP_ATTEMPTS = 10;

    private final RepositoryInfoRepository repository;
    private final RepositoryManagementConfig config;

    public boolean ensureEnoughFreeSpace() {
        if (hasEnoughFreeSpace()) {
            return true;
        }

        log.warn("Insufficient disk space, initiating cleanup...");

        int attempts = 0;
        while (!hasEnoughFreeSpace() && attempts < MAX_CLEANUP_ATTEMPTS) {
            attempts++;
            boolean cleanupSuccess = cleanupRepositories();

            if (!cleanupSuccess) {
                log.warn("Cleanup attempt {} failed, stopping further attempts.", attempts);
                break;
            }
        }

        if (!hasEnoughFreeSpace()) {
            log.warn("Not enough free space after {} cleanup attempts.", attempts);
            return false;
        }

        log.info("Successfully freed up space after {} cleanup attempts.", attempts);
        return true;
    }

    private boolean hasEnoughFreeSpace() {
        try {
            Path baseDirectory = Path.of(config.getBaseDirectory());
            FileStore fileStore = Files.getFileStore(baseDirectory);
            long usableSpace = fileStore.getUsableSpace();
            long requiredSpace = config.getMinFreeSpaceInBytes();

            log.debug("Usable space: {} bytes, Required space: {} bytes",
                    FileUtils.byteCountToDisplaySize(usableSpace),
                    FileUtils.byteCountToDisplaySize(requiredSpace));

            return usableSpace >= requiredSpace;

        } catch (IOException e) {
            log.error("Error checking free disk space: {}", e.getMessage(), e);
            return false;
        }
    }

    private boolean cleanupRepositories() {
        log.info("Starting repository cleanup using strategy: {}", config.getCleanupStrategy());

        try {
            List<RepositoryInfo> repositories = getRepositoriesOrderedByStrategy();

            if (repositories.isEmpty()) {
                log.warn("No repositories available for cleanup.");
                return false;
            }

            RepositoryInfo toRemove = repositories.getFirst();
            logRemovedRepository(toRemove);

            File localPath = new File(toRemove.getLocalPath());
            deleteRepositoryDirectory(localPath);
            repository.delete(toRemove);
            return true;

        } catch (Exception e) {
            log.error("Error during repository cleanup: {}", e.getMessage(), e);
            return false;
        }
    }

    private List<RepositoryInfo> getRepositoriesOrderedByStrategy() {
        return switch (config.getCleanupStrategy()) {
            case LEAST_RECENTLY_USED -> repository.findAllByOrderByLastAccessedAtAsc();
            case LEAST_FREQUENTLY_USED -> repository.findAllByOrderByAccessCountAsc();
        };
    }

    private void logRemovedRepository(RepositoryInfo toRemove) {
        log.info("Removing repository: {} (last accessed: {}, access count: {}, size: {})",
                toRemove.getLocalPath(),
                toRemove.getLastAccessedAt(),
                toRemove.getAccessCount(),
                FileUtils.byteCountToDisplaySize(toRemove.getSizeInBytes() != null ? toRemove.getSizeInBytes() : 0L));
    }

    public void deleteRepositoryDirectory(File directory) {
        try {
            if (directory.exists()) {
                FileUtils.deleteDirectory(directory);
                log.info("Successfully removed repository at {}", directory);
            }

        } catch (IOException e) {
            log.error("Error removing repository at {}: {}", directory, e.getMessage(), e);
        }
    }

}
