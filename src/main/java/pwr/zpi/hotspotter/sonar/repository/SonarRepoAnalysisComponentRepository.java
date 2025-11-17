package pwr.zpi.hotspotter.sonar.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import pwr.zpi.hotspotter.sonar.model.repoanalysis.SonarRepoAnalysisComponent;

public interface SonarRepoAnalysisComponentRepository extends MongoRepository<SonarRepoAnalysisComponent, String> {}
