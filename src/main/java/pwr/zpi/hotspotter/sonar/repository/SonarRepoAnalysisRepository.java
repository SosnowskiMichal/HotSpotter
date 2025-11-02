package pwr.zpi.hotspotter.sonar.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import pwr.zpi.hotspotter.sonar.model.repoanalysis.SonarRepoAnalysisResult;

public interface SonarRepoAnalysisRepository extends MongoRepository<SonarRepoAnalysisResult, String> {
}
