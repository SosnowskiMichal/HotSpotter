package pwr.zpi.hotspotter.repositoryanalysis.analyzer.authors.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.authors.model.AuthorStatistics;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuthorStatisticsRepository extends MongoRepository<AuthorStatistics, String> {

    List<AuthorStatistics> findAllByAnalysisId(String analysisId);

    Optional<AuthorStatistics> findByAnalysisIdAndName(String analysisId, String name);

    long countAllByAnalysisId(String analysisId);

    void deleteAllByAnalysisId(String analysisId);

}
