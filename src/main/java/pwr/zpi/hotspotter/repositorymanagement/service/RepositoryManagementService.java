package pwr.zpi.hotspotter.repositorymanagement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pwr.zpi.hotspotter.repositorymanagement.config.RepositoryManagementConfig;
import pwr.zpi.hotspotter.repositorymanagement.model.RepositoryInfo;
import pwr.zpi.hotspotter.repositorymanagement.repository.RepositoryInfoRepository;
import pwr.zpi.hotspotter.repositorymanagement.service.operation.RepositoryCloner;
import pwr.zpi.hotspotter.repositorymanagement.service.operation.RepositoryOperationQueue;
import pwr.zpi.hotspotter.repositorymanagement.service.operation.RepositoryUpdater;
import pwr.zpi.hotspotter.repositorymanagement.service.parser.RepositoryUrlParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RepositoryManagementService {

    private final RepositoryInfoRepository repositoryInfoRepository;
    private final RepositoryManagementConfig repositoryManagementConfig;
    private final RepositoryUrlParser repositoryUrlParser;
    private final RepositoryCloner repositoryCloner;
    private final RepositoryUpdater repositoryUpdater;
    private final RepositoryOperationQueue repositoryOperationQueue;

    public RepositoryOperationResult cloneOrUpdateRepository(String repositoryUrl) {
        log.info("Processing repository request for URL: {}", repositoryUrl);
        return repositoryOperationQueue.executeOperation(repositoryUrl, () -> processRepository(repositoryUrl));
    }

    private RepositoryOperationResult processRepository(String repositoryUrl) {
        try {
            RepositoryUrlParser.RepositoryData repositoryData = repositoryUrlParser.parse(repositoryUrl);
            Path localPath = getLocalRepositoryPath(repositoryData);
            Optional<RepositoryInfo> existingRepository = repositoryInfoRepository.findByRemoteUrl(repositoryUrl);

            if (existingRepository.isPresent() && Files.exists(localPath)) {
                return repositoryUpdater.update(existingRepository.get());
            } else {
                return repositoryCloner.clone(repositoryData, localPath);
            }

        } catch (Exception e) {
            log.error("Error processing repository URL {}: {}", repositoryUrl, e.getMessage(), e);
            return RepositoryOperationResult.failure("Error: " + e.getMessage());
        }
    }

    private Path getLocalRepositoryPath(RepositoryUrlParser.RepositoryData repositoryData) {
        return Path.of(repositoryManagementConfig.getBaseDirectory(), repositoryData.getPath());
    }

    public record RepositoryOperationResult(boolean success, String message, RepositoryInfo repositoryInfo) {

        public static RepositoryOperationResult success(String message, RepositoryInfo repositoryInfo) {
            return new RepositoryOperationResult(true, message, repositoryInfo);
        }

        public static RepositoryOperationResult failure(String message) {
            return new RepositoryOperationResult(false, message, null);
        }

        public String getLocalPath() {
            return repositoryInfo != null ? repositoryInfo.getLocalPath() : null;
        }

    }

}
