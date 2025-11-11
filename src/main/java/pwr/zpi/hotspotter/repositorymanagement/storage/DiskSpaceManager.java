package pwr.zpi.hotspotter.repositorymanagement.storage;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class DiskSpaceManager {

    private final RepositoryInfoRepository repositoryInfoRepository;
    private final RepositoryManagementConfig repositoryManagementConfig;

    @PostConstruct
    public void init() {
        try {
            Path baseDirectory = Path.of(repositoryManagementConfig.getBaseDirectory());
            if (!Files.exists(baseDirectory)) {
                Files.createDirectories(baseDirectory);
                log.info("Created base directory: {}", baseDirectory);
            }

        } catch (IOException e) {
            log.error("Failed to create base directory: {}", e.getMessage(), e);
        }
    }

    public boolean ensureEnoughFreeSpace() {
        if (hasEnoughFreeSpace()) return true;
        log.info("Insufficient disk space, initiating cleanup...");

        boolean cleanupSuccess = cleanupRepositories();

        if (!cleanupSuccess) {
            log.warn("Repositories cleanup failed");
            return false;
        }

        log.info("Successfully freed up space after cleanup");
        return true;
    }

    private boolean hasEnoughFreeSpace() {
        try {
            long usableSpace = getUsableSpace();
            long requiredSpace = repositoryManagementConfig.getMinFreeSpaceInBytes();

            log.debug("Usable space: {} bytes, Required space: {} bytes",
                    FileUtils.byteCountToDisplaySize(usableSpace),
                    FileUtils.byteCountToDisplaySize(requiredSpace));

            return usableSpace >= requiredSpace;

        } catch (IOException e) {
            log.error("Error checking free disk space: {}", e.getMessage(), e);
            return false;
        }
    }

    private long getUsableSpace() throws IOException {
        Path baseDirectory = Path.of(repositoryManagementConfig.getBaseDirectory());
        FileStore fileStore = Files.getFileStore(baseDirectory);
        return fileStore.getUsableSpace();
    }

    private boolean cleanupRepositories() {
        log.info("Starting repositories cleanup using strategy: {}", repositoryManagementConfig.getCleanupStrategy());

        try {
            List<RepositoryInfo> repositories = getRepositoriesOrderedByStrategy();

            if (repositories.isEmpty()) {
                log.warn("No repositories available for cleanup.");
                return false;
            }

            long spaceNeeded = repositoryManagementConfig.getMinFreeSpaceInBytes() - getUsableSpace();
            long spaceFreed = 0L;

            for (RepositoryInfo repositoryInfo : repositories) {
                if (spaceFreed >= spaceNeeded) break;

                long repositorySize = repositoryInfo.getSizeInBytes() != null ? repositoryInfo.getSizeInBytes() : 0L;
                File localPath = new File(repositoryInfo.getLocalPath());
                if (deleteRepositoryDirectory(localPath)) {
                    repositoryInfoRepository.delete(repositoryInfo);
                    logRemovedRepository(repositoryInfo);
                    spaceFreed += repositorySize;
                }
            }

            return spaceFreed >= spaceNeeded;

        } catch (IOException e) {
            log.error("Error during repository cleanup: {}", e.getMessage(), e);
            return false;
        }
    }

    private List<RepositoryInfo> getRepositoriesOrderedByStrategy() {
        return switch (repositoryManagementConfig.getCleanupStrategy()) {
            case LEAST_RECENTLY_USED -> repositoryInfoRepository.findAllByOrderByLastAccessedAtAsc();
            case LEAST_FREQUENTLY_USED -> repositoryInfoRepository.findAllByOrderByAccessCountAsc();
        };
    }

    private void logRemovedRepository(RepositoryInfo toRemove) {
        log.info("Removing repository: {} (last accessed: {}, access count: {}, size: {})",
                toRemove.getLocalPath(),
                toRemove.getLastAccessedAt(),
                toRemove.getAccessCount(),
                FileUtils.byteCountToDisplaySize(toRemove.getSizeInBytes() != null ? toRemove.getSizeInBytes() : 0L));
    }

    public boolean deleteRepositoryDirectory(File directory) {
        try {
            if (directory.exists()) {
                FileUtils.deleteDirectory(directory);
                log.info("Successfully removed repository at {}", directory);
                return true;
            }
            return false;

        } catch (IOException e) {
            log.error("Error removing repository at {}: {}", directory, e.getMessage(), e);
            return false;
        }
    }

}
