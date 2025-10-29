package pwr.zpi.hotspotter.repositorymanagement.service.operation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.repositorymanagement.config.RepositoryManagementConfig;
import pwr.zpi.hotspotter.repositorymanagement.model.RepositoryInfo;
import pwr.zpi.hotspotter.repositorymanagement.repository.RepositoryInfoRepository;
import pwr.zpi.hotspotter.repositorymanagement.service.RepositoryManagementService;
import pwr.zpi.hotspotter.repositorymanagement.service.command.CommandExecutor;
import pwr.zpi.hotspotter.repositorymanagement.service.storage.DiskSpaceManager;

import java.nio.file.Path;

@Slf4j
@Component
@RequiredArgsConstructor
public class RepositoryUpdater {

    private final CommandExecutor commandExecutor;
    private final DiskSpaceManager diskSpaceManager;
    private final RepositoryManagementConfig repositoryManagementConfig;
    private final RepositoryInfoRepository repositoryInfoRepository;

    public RepositoryManagementService.RepositoryOperationResult update(RepositoryInfo repositoryInfo) {
        log.info("Updating existing repository at {}", repositoryInfo.getLocalPath());

        Path localPath = Path.of(repositoryInfo.getLocalPath());
        ProcessBuilder pb = createProcessBuilder(localPath);
        CommandExecutor.CommandResult result = commandExecutor.executeCommand(pb, repositoryManagementConfig.getUpdateMonitoringIntervalSeconds());

        if (!result.success()) {
            log.error("Error updating existing repository at {}, exit code: {}", localPath, result.exitCode());
            return RepositoryManagementService.RepositoryOperationResult.failure("Git pull failed with exit code: " + result.exitCode());
        }

        log.info("Successfully updated existing repository at {}", localPath);
        updateRepositoryMetadata(repositoryInfo, localPath);
        return RepositoryManagementService.RepositoryOperationResult.success("Repository updated successfully.", repositoryInfo);
    }

    private ProcessBuilder createProcessBuilder(Path localPath) {
        ProcessBuilder pb = new ProcessBuilder();
        pb.command("git", "pull", "origin");
        pb.directory(localPath.toFile());
        pb.redirectErrorStream(false);
        return pb;
    }

    private void updateRepositoryMetadata(RepositoryInfo repositoryInfo, Path localPath) {
        long repositorySize = FileUtils.sizeOfDirectory(localPath.toFile());
        repositoryInfo.setSizeInBytes(repositorySize);
        repositoryInfo.recordUsage();
        repositoryInfoRepository.save(repositoryInfo);
    }

}
