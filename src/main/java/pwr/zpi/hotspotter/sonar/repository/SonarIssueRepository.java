package pwr.zpi.hotspotter.sonar.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import pwr.zpi.hotspotter.sonar.model.fileanalysis.SonarIssue;

import java.util.List;

@Repository
public interface SonarIssueRepository extends MongoRepository<SonarIssue, String> {
    List<SonarIssue> findAllByRepoAnalysisIdAndPath(String repoAnalysisId, String filePath);
}
