package pwr.zpi.hotspotter.sonar.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import pwr.zpi.hotspotter.sonar.model.analysisstatus.SonarAnalysisState;
import pwr.zpi.hotspotter.sonar.model.analysisstatus.SonarAnalysisStatus;

import java.util.Optional;

@Repository
public interface SonarAnalysisStatusRepository extends MongoRepository<SonarAnalysisStatus, String> {
    Optional<SonarAnalysisStatus> findFirstByProjectKeyOrderByStartTimeDesc(String projectKey);
}
