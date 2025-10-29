package pwr.zpi.hotspotter.repositorymanagement.service.operation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.repositorymanagement.config.RepositoryManagementConfig;
import pwr.zpi.hotspotter.repositorymanagement.model.RepositoryInfo;
import pwr.zpi.hotspotter.repositorymanagement.repository.RepositoryInfoRepository;
import pwr.zpi.hotspotter.repositorymanagement.service.RepositoryManagementService;
import pwr.zpi.hotspotter.repositorymanagement.service.parser.RepositoryUrlParser;
import pwr.zpi.hotspotter.repositorymanagement.service.storage.DiskSpaceManager;

import java.io.File;
import java.nio.file.Path;

@Slf4j
@Component
@RequiredArgsConstructor
public class RepositoryCloner {

    private final DiskSpaceManager diskSpaceManager;
    private final RepositoryManagementConfig repositoryManagementConfig;
    private final RepositoryInfoRepository repositoryInfoRepository;

    public RepositoryManagementService.RepositoryOperationResult clone(RepositoryUrlParser.RepositoryData repositoryData, Path localPath) {
        String repositoryUrl = repositoryData.repositoryUrl();
        log.info("Cloning repository from URL {} to {}", repositoryUrl, localPath);

        if (!diskSpaceManager.ensureEnoughFreeSpace()) {
            diskSpaceManager.deleteRepositoryDirectory(localPath.toFile());
            return RepositoryManagementService.RepositoryOperationResult.failure("Insufficient disk space or failed cleanup.");
        }

        if (!createLocalDirectory(localPath)) {
            return RepositoryManagementService.RepositoryOperationResult.failure("Failed to create local directory for repository.");
        }

        try {
            cloneRepository(repositoryUrl, localPath);
        } catch (GitAPIException e) {
            log.error("Git clone failed for URL {}: {}", repositoryUrl, e.getMessage(), e);
            diskSpaceManager.deleteRepositoryDirectory(localPath.toFile());
            return RepositoryManagementService.RepositoryOperationResult.failure("Git clone failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during clone for URL {}: {}", repositoryUrl, e.getMessage(), e);
            diskSpaceManager.deleteRepositoryDirectory(localPath.toFile());
            return RepositoryManagementService.RepositoryOperationResult.failure("Clone failed: " + e.getMessage());
        }

        if (!isValidGitRepository(localPath)) {
            log.error("Invalid git repository after clone: {}", localPath);
            diskSpaceManager.deleteRepositoryDirectory(localPath.toFile());
            return RepositoryManagementService.RepositoryOperationResult.failure("Invalid git repository after clone.");
        }

        log.info("Successfully cloned repository {} to {}", repositoryUrl, localPath);
        RepositoryInfo repositoryInfo = createAndSaveRepositoryInfo(repositoryData, localPath);
        return RepositoryManagementService.RepositoryOperationResult.success("Repository cloned successfully.", repositoryInfo);
    }

    private boolean createLocalDirectory(Path localPath) {
        try {
            FileUtils.forceMkdir(localPath.toFile());
            log.debug("Created local directory: {}", localPath);
            return true;
        } catch (Exception e) {
            log.error("Error creating directory {}: {}", localPath, e.getMessage(), e);
            return false;
        }
    }

    private void cloneRepository(String repositoryUrl, Path localPath) throws GitAPIException {
        int logIntervalPercentage = repositoryManagementConfig.getCloneMonitoringIntervalPercentage();

        CloneCommand cloneCommand = Git.cloneRepository()
                .setURI(repositoryUrl)
                .setDirectory(localPath.toFile())
                .setProgressMonitor(new ProcessProgressMonitor(logIntervalPercentage))
                .setCloneAllBranches(false);

        try (Git _ = cloneCommand.call()) {
            log.debug("Git clone command completed successfully for URL {}", repositoryUrl);
        }
    }

    private boolean isValidGitRepository(Path localPath) {
        File gitDir = localPath.resolve(".git").toFile();
        return gitDir.exists() && gitDir.isDirectory();
    }

    private RepositoryInfo createAndSaveRepositoryInfo(RepositoryUrlParser.RepositoryData repositoryData, Path localPath) {
        long repositorySize = FileUtils.sizeOfDirectory(localPath.toFile());
        RepositoryInfo repositoryInfo = new RepositoryInfo(repositoryData, localPath.toString());
        repositoryInfo.setSizeInBytes(repositorySize);
        repositoryInfo.recordUsage();
        repositoryInfoRepository.save(repositoryInfo);
        return repositoryInfo;
    }

}
