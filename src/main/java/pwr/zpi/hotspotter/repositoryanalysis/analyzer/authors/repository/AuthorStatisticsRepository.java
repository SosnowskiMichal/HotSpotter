package pwr.zpi.hotspotter.repositoryanalysis.analyzer.authors.repository;

import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.authors.model.AuthorStatistics;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuthorStatisticsRepository extends MongoRepository<AuthorStatistics, String> {

    List<AuthorStatistics> findAllByAnalysisId(String analysisId);

    Optional<AuthorStatistics> findByAnalysisIdAndName(String analysisId, String name);

    int countAllByAnalysisId(String analysisId);

    int countAllByAnalysisIdAndIsActive(String analysisId, Boolean isActive);

    default int countActiveAuthorsByAnalysisId(String analysisId) {
        return countAllByAnalysisIdAndIsActive(analysisId, true);
    }

    @Aggregation(pipeline = {
            "{ $match: { analysisId: ?0 } }",
            "{ $group: { _id: null, totalCommits: { $sum: '$commits' } } }"
    })
    int sumCommitsByAnalysisId(String analysisId);

    void deleteAllByAnalysisId(String analysisId);

}
