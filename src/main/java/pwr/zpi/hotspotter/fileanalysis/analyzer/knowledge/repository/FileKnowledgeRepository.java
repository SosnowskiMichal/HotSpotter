package pwr.zpi.hotspotter.fileanalysis.analyzer.knowledge.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import pwr.zpi.hotspotter.fileanalysis.analyzer.knowledge.model.FileKnowledge;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileKnowledgeRepository extends MongoRepository<FileKnowledge, String> {

    List<FileKnowledge> findAllByAnalysisId(String analysisId);

    Optional<FileKnowledge> findByAnalysisIdAndFilePath(String analysisId, String filePath);

    long countAllByAnalysisId(String analysisId);

    void deleteAllByAnalysisId(String analysisId);

}
