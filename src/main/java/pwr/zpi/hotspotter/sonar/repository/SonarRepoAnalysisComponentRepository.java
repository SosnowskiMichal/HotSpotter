package pwr.zpi.hotspotter.sonar.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import pwr.zpi.hotspotter.sonar.model.repoanalysis.SonarRepoAnalysisComponent;

import java.util.Optional;

@Repository
public interface SonarRepoAnalysisComponentRepository extends MongoRepository<SonarRepoAnalysisComponent, String> {

    Optional<SonarRepoAnalysisComponent> findByRepoAnalysisIdAndPath(String repoAnalysisId, String path);

}
