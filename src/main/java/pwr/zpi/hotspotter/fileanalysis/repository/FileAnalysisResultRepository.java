package pwr.zpi.hotspotter.fileanalysis.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import pwr.zpi.hotspotter.fileanalysis.model.FileAnalysisResult;

import java.util.Optional;

@Repository
public interface FileAnalysisResultRepository extends MongoRepository<FileAnalysisResult, String> {

    Optional<FileAnalysisResult> findByAnalysisIdAndFilePath(String analysisId, String filePath);

    boolean existsByAnalysisIdAndFilePath(String analysisId, String filePath);

    boolean existsByAnalysisIdAndFilePathAndStatus(String analysisId, String filePath, FileAnalysisResult.FileAnalysisStatus status);

    default boolean isAnalysisCompleted(String analysisId, String filePath) {
        return existsByAnalysisIdAndFilePathAndStatus(analysisId, filePath, FileAnalysisResult.FileAnalysisStatus.COMPLETED);
    }

    void deleteByAnalysisId(String analysisId);

}
