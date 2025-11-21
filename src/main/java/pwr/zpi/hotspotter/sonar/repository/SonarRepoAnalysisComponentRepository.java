package pwr.zpi.hotspotter.sonar.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import pwr.zpi.hotspotter.sonar.model.repoanalysis.SonarRepoAnalysisComponent;

import java.util.Optional;

@Repository
public interface SonarRepoAnalysisComponentRepository extends MongoRepository<SonarRepoAnalysisComponent, String> {

    Optional<SonarRepoAnalysisComponent> findByRepoAnalysisIdAndPath(String repoAnalysisId, String path);

}
