package pwr.zpi.hotspotter.repositorymanagement.service.operation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.repositorymanagement.config.RepositoryManagementConfig;
import pwr.zpi.hotspotter.repositorymanagement.model.RepositoryInfo;
import pwr.zpi.hotspotter.repositorymanagement.repository.RepositoryInfoRepository;
import pwr.zpi.hotspotter.repositorymanagement.service.RepositoryManagementService;
import pwr.zpi.hotspotter.repositorymanagement.service.storage.DiskSpaceManager;

import java.io.IOException;
import java.nio.file.Path;

@Slf4j
@Component
@RequiredArgsConstructor
public class RepositoryUpdater {

    private final RepositoryManagementConfig repositoryManagementConfig;
    private final RepositoryInfoRepository repositoryInfoRepository;
    private final DiskSpaceManager diskSpaceManager;

    public RepositoryManagementService.RepositoryOperationResult update(RepositoryInfo repositoryInfo) {
        log.info("Updating existing repository at {}", repositoryInfo.getLocalPath());

        Path localPath = Path.of(repositoryInfo.getLocalPath());

        if (!diskSpaceManager.ensureEnoughFreeSpace()) {
            return RepositoryManagementService.RepositoryOperationResult.failure("Insufficient disk space or failed cleanup.");
        }

        try {
            PullResult result = updateRepository(localPath);

            if (!result.isSuccessful()) {
                log.error("Failed to update existing repository at {}", localPath);
                return RepositoryManagementService.RepositoryOperationResult.failure("Git pull failed.");
            }

            log.info("Successfully updated existing repository at {}", localPath);
            updateRepositoryMetadata(repositoryInfo, localPath);
            return RepositoryManagementService.RepositoryOperationResult.success("Repository updated successfully.", repositoryInfo);

        } catch (GitAPIException e) {
            log.error("Git pull failed for repository at {}: {}", localPath, e.getMessage(), e);
            return RepositoryManagementService.RepositoryOperationResult.failure("Git pull failed: " + e.getMessage());

        } catch (IOException e) {
            log.error("IO error during git pull for repository at {}: {}", localPath, e.getMessage(), e);
            return RepositoryManagementService.RepositoryOperationResult.failure("IO error during git pull: " + e.getMessage());

        } catch (Exception e) {
            log.error("Unexpected error during git pull for repository at {}: {}", localPath, e.getMessage(), e);
            return RepositoryManagementService.RepositoryOperationResult.failure("Git pull failed: " + e.getMessage());
        }
    }

    private PullResult updateRepository(Path localPath) throws GitAPIException, IOException {
        int logIntervalPercentage = repositoryManagementConfig.getUpdateMonitoringIntervalPercentage();

        try (Git git = Git.open(localPath.toFile())) {
            PullCommand pullCommand = git.pull()
                    .setProgressMonitor(new ProcessProgressMonitor(logIntervalPercentage))
                    .setRemote("origin");

            return pullCommand.call();
        }
    }

    private void updateRepositoryMetadata(RepositoryInfo repositoryInfo, Path localPath) {
        long repositorySize = FileUtils.sizeOfDirectory(localPath.toFile());
        repositoryInfo.setSizeInBytes(repositorySize);
        repositoryInfo.recordUsage();
        repositoryInfoRepository.save(repositoryInfo);
    }

}
