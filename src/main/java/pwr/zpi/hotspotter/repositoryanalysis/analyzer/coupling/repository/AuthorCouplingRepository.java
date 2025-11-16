package pwr.zpi.hotspotter.repositoryanalysis.analyzer.coupling.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.coupling.model.AuthorCoupling;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuthorCouplingRepository extends MongoRepository<AuthorCoupling, String> {

    List<AuthorCoupling> findAllByAnalysisId(String analysisId);

    Optional<AuthorCoupling> findByAnalysisIdAndAuthor(String analysisId, String author);

    long countAllByAnalysisId(String analysisId);

    void deleteAllByAnalysisId(String analysisId);

}
