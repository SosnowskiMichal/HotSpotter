package pwr.zpi.hotspotter.repositorymanagement.operation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.repositorymanagement.config.RepositoryManagementConfig;
import pwr.zpi.hotspotter.repositorymanagement.exception.RepositoryCloneException;
import pwr.zpi.hotspotter.repositorymanagement.model.RepositoryInfo;
import pwr.zpi.hotspotter.repositorymanagement.repository.RepositoryInfoRepository;
import pwr.zpi.hotspotter.repositorymanagement.parser.RepositoryUrlParser;
import pwr.zpi.hotspotter.repositorymanagement.storage.DiskSpaceManager;

import java.io.File;
import java.nio.file.Path;

@Slf4j
@Component
@RequiredArgsConstructor
public class RepositoryCloner {

    private final DiskSpaceManager diskSpaceManager;
    private final RepositoryManagementConfig repositoryManagementConfig;
    private final RepositoryInfoRepository repositoryInfoRepository;

    public RepositoryInfo clone(RepositoryUrlParser.RepositoryData repositoryData) {
        String repositoryUrl = repositoryData.repositoryUrl();
        Path localPath = getLocalRepositoryPath(repositoryData);

        log.info("Cloning repository from URL {} to {}", repositoryUrl, localPath);

        if (!diskSpaceManager.ensureEnoughFreeSpace()) {
            diskSpaceManager.deleteRepositoryDirectory(localPath.toFile());
            throw new RepositoryCloneException("Insufficient disk space or failed cleanup.");
        }

        if (!createLocalDirectory(localPath)) {
            throw new RepositoryCloneException("Failed to create local directory for repository.");
        }

        if (!cleanLocalDirectory(localPath)) {
            diskSpaceManager.deleteRepositoryDirectory(localPath.toFile());
            throw new RepositoryCloneException("Failed to cleanup local directory for repository.");
        }

        try {
            cloneRepository(repositoryUrl, localPath);
        } catch (GitAPIException e) {
            log.error("Git clone failed for URL {}: {}", repositoryUrl, e.getMessage(), e);
            diskSpaceManager.deleteRepositoryDirectory(localPath.toFile());
            throw new RepositoryCloneException("Git clone failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during clone for URL {}: {}", repositoryUrl, e.getMessage(), e);
            diskSpaceManager.deleteRepositoryDirectory(localPath.toFile());
            throw new RepositoryCloneException("Unexpected error during clone: " + e.getMessage());
        }

        if (!isValidGitRepository(localPath)) {
            log.error("Invalid git repository after clone: {}", localPath);
            diskSpaceManager.deleteRepositoryDirectory(localPath.toFile());
            throw new RepositoryCloneException("Invalid git repository after clone.");
        }

        log.info("Successfully cloned repository {} to {}", repositoryUrl, localPath);
        return createAndSaveRepositoryInfo(repositoryData, localPath);
    }

    private Path getLocalRepositoryPath(RepositoryUrlParser.RepositoryData repositoryData) {
        return Path.of(repositoryManagementConfig.getBaseDirectory(), repositoryData.getPath());
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

    private boolean cleanLocalDirectory(Path localPath) {
        try {
            FileUtils.cleanDirectory(localPath.toFile());
            return true;
        } catch (Exception e) {
            log.error("Error cleaning directory {}: {}", localPath, e.getMessage(), e);
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
