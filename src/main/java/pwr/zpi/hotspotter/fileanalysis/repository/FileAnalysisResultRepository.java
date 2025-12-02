package pwr.zpi.hotspotter.fileanalysis.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import pwr.zpi.hotspotter.fileanalysis.model.FileAnalysisResult;

import java.util.Optional;

@Repository
public interface FileAnalysisResultRepository extends MongoRepository<FileAnalysisResult, String> {

    Optional<FileAnalysisResult> findByAnalysisIdAndFilePath(String analysisId, String filePath);

    boolean existsByAnalysisIdAndFilePath(String analysisId, String filePath);

    void deleteByAnalysisId(String analysisId);

}
