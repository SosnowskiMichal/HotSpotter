package pwr.zpi.hotspotter.sonar.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import pwr.zpi.hotspotter.sonar.model.repoanalysis.SonarRepoAnalysisResult;

import java.util.Optional;

@Repository
public interface SonarRepoAnalysisRepository extends MongoRepository<SonarRepoAnalysisResult, String> {
    Optional<SonarRepoAnalysisResult> findByRepoAnalysisId(String repoAnalysisId);
}
