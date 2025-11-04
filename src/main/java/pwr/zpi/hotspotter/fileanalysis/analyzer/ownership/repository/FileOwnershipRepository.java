package pwr.zpi.hotspotter.fileanalysis.analyzer.ownership.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import pwr.zpi.hotspotter.fileanalysis.analyzer.ownership.model.FileOwnership;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileOwnershipRepository extends MongoRepository<FileOwnership, String> {

    List<FileOwnership> findAllByAnalysisId(String analysisId);

    Optional<FileOwnership> findByAnalysisIdAndFilePath(String analysisId, String filePath);

    long countAllByAnalysisId(String analysisId);

    void deleteAllByAnalysisId(String analysisId);

}
