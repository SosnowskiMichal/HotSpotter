package pwr.zpi.hotspotter.repositorymanagement.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pwr.zpi.hotspotter.repositorymanagement.config.RepositoryManagementConfig;
import pwr.zpi.hotspotter.repositorymanagement.model.RepositoryInfo;
import pwr.zpi.hotspotter.repositorymanagement.repository.RepositoryInfoRepository;
import pwr.zpi.hotspotter.repositorymanagement.service.operation.RepositoryCloner;
import pwr.zpi.hotspotter.repositorymanagement.service.operation.RepositoryUpdater;
import pwr.zpi.hotspotter.repositorymanagement.service.parser.RepositoryUrlParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
public class RepositoryManagementService {

    private final RepositoryInfoRepository repository;
    private final RepositoryManagementConfig config;
    private final RepositoryUrlParser urlParser;
    private final RepositoryCloner cloner;
    private final RepositoryUpdater updater;

    @Transactional
    public RepositoryOperationResult cloneOrUpdateRepository(String repositoryUrl) {
        log.info("Processing repository request for URL: {}", repositoryUrl);

        try {
            RepositoryUrlParser.RepositoryData repositoryData = urlParser.parse(repositoryUrl);
            Path localPath = getLocalRepositoryPath(repositoryData);
            Optional<RepositoryInfo> existingRepository = repository.findByRemoteUrl(repositoryUrl);

            if (existingRepository.isPresent() && Files.exists(localPath)) {
                return updater.update(existingRepository.get());
            } else {
                return cloner.clone(repositoryUrl, localPath);
            }

        } catch (Exception e) {
            log.error("Error processing repository URL {}: {}", repositoryUrl, e.getMessage(), e);
            return RepositoryOperationResult.failure("Error: " + e.getMessage());
        }
    }

    private Path getLocalRepositoryPath(RepositoryUrlParser.RepositoryData repositoryData) {
        return Path.of(config.getBaseDirectory(), repositoryData.getPath());
    }

}
