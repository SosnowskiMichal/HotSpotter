package pwr.zpi.hotspotter.sonar.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import pwr.zpi.hotspotter.sonar.model.SonarAnalysisStatus;

@Repository
public interface SonarAnalysisStatusRepository extends MongoRepository<SonarAnalysisStatus, String> {
}
