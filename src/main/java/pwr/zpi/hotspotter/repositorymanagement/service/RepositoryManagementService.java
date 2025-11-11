package pwr.zpi.hotspotter.repositorymanagement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.springframework.stereotype.Service;
import pwr.zpi.hotspotter.repositorymanagement.exception.InvalidRepositoryUrlException;
import pwr.zpi.hotspotter.repositorymanagement.model.RepositoryInfo;
import pwr.zpi.hotspotter.repositorymanagement.repository.RepositoryInfoRepository;
import pwr.zpi.hotspotter.repositorymanagement.operation.RepositoryCloner;
import pwr.zpi.hotspotter.repositorymanagement.operation.RepositoryOperationQueue;
import pwr.zpi.hotspotter.repositorymanagement.operation.RepositoryUpdater;
import pwr.zpi.hotspotter.repositorymanagement.parser.RepositoryUrlParser;
import pwr.zpi.hotspotter.repositorymanagement.storage.DiskSpaceManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RepositoryManagementService {

    private final RepositoryUrlParser repositoryUrlParser;
    private final RepositoryInfoRepository repositoryInfoRepository;
    private final RepositoryCloner repositoryCloner;
    private final RepositoryUpdater repositoryUpdater;
    private final RepositoryOperationQueue repositoryOperationQueue;
    private final DiskSpaceManager diskSpaceManager;

    public RepositoryInfo cloneOrUpdateRepository(String repositoryUrl) {
        log.info("Processing repository request for URL: {}", repositoryUrl);
        return repositoryOperationQueue.executeOperation(repositoryUrl, () -> performCloneOrUpdate(repositoryUrl));
    }

    private RepositoryInfo performCloneOrUpdate(String repositoryUrl) {
        try {
            RepositoryUrlParser.RepositoryData repositoryData = repositoryUrlParser.parse(repositoryUrl);
            Optional<RepositoryInfo> repositoryInfoOptional = repositoryInfoRepository.findByNameAndOwnerAndPlatform(
                    repositoryData.name(),
                    repositoryData.owner(),
                    repositoryData.platform()
            );
            RepositoryState repositoryState = determineRepositoryState(repositoryInfoOptional);

            return switch (repositoryState) {
                case NONEXISTENT -> repositoryCloner.clone(repositoryData);
                case VALID -> repositoryUpdater.update(repositoryInfoOptional.get());
                case CORRUPTED, DB_ONLY -> {
                    cleanupRepository(repositoryInfoOptional.get());
                    yield repositoryCloner.clone(repositoryData);
                }
            };

        } catch (InvalidRepositoryUrlException e) {
            log.error("Invalid repository URL {}: {}", repositoryUrl, e.getMessage(), e);
            throw e;
        }
    }

    private RepositoryState determineRepositoryState(Optional<RepositoryInfo> repositoryInfoOptional) {
        if (repositoryInfoOptional.isEmpty()) return RepositoryState.NONEXISTENT;

        RepositoryInfo repositoryInfo = repositoryInfoOptional.get();
        Path localPath = Path.of(repositoryInfo.getLocalPath());

        if (!Files.exists(localPath)) return RepositoryState.DB_ONLY;
        if (!Files.isReadable(localPath) && !Files.isWritable(localPath)) return RepositoryState.CORRUPTED;
        if (!isValidGitRepository(localPath)) return RepositoryState.CORRUPTED;

        return RepositoryState.VALID;
    }

    private boolean isValidGitRepository(Path localPath) {
        try {
            Path gitDir = localPath.resolve(".git");
            if (!Files.exists(gitDir) || !Files.isDirectory(gitDir)) {
                return false;
            }
            try (Git git = Git.open(localPath.toFile())) {
                git.getRepository().getObjectDatabase();
                return true;
            }

        } catch (Exception e) {
            return false;
        }
    }

    private void cleanupRepository(RepositoryInfo repositoryInfo) {
        repositoryInfoRepository.delete(repositoryInfo);
        diskSpaceManager.deleteRepositoryDirectory(Path.of(repositoryInfo.getLocalPath()).toFile());
    }

    private enum RepositoryState {
        VALID,
        CORRUPTED,
        DB_ONLY,
        NONEXISTENT
    }

}
