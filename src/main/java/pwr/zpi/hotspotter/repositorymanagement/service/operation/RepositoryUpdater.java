package pwr.zpi.hotspotter.repositorymanagement.service.operation;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.repositorymanagement.model.RepositoryInfo;
import pwr.zpi.hotspotter.repositorymanagement.repository.RepositoryInfoRepository;
import pwr.zpi.hotspotter.repositorymanagement.service.RepositoryOperationResult;
import pwr.zpi.hotspotter.repositorymanagement.service.command.CommandExecutor;
import pwr.zpi.hotspotter.repositorymanagement.service.storage.DiskSpaceManager;

import java.nio.file.Path;

@Slf4j
@Component
@AllArgsConstructor
public class RepositoryUpdater {

    private static final int UPDATE_PROCESS_MONITORING_INTERVAL = 10;

    private final CommandExecutor commandExecutor;
    private final DiskSpaceManager diskSpaceManager;
    private final RepositoryInfoRepository repository;

    public RepositoryOperationResult update(RepositoryInfo repositoryInfo) {
        log.info("Updating existing repository at {}", repositoryInfo.getLocalPath());

        Path localPath = Path.of(repositoryInfo.getLocalPath());
        ProcessBuilder pb = createProcessBuilder(localPath);
        CommandExecutor.CommandResult result = commandExecutor.executeCommand(pb, UPDATE_PROCESS_MONITORING_INTERVAL);

        if (!result.success()) {
            log.error("Error updating existing repository at {}, exit code: {}", localPath, result.exitCode());
            diskSpaceManager.deleteRepositoryDirectory(localPath.toFile());
            repository.delete(repositoryInfo);
            return RepositoryOperationResult.failure("Git pull failed with exit code: " + result.exitCode());
        }

        log.info("Successfully updated existing repository at {}", localPath);
        updateRepositoryMetadata(repositoryInfo, localPath);
        return RepositoryOperationResult.success("Repository updated successfully.", repositoryInfo);
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
        repository.save(repositoryInfo);
    }

}
