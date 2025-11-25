package pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge.model.FileKnowledge;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge.model.KnowledgeRisk;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileKnowledgeRepository extends MongoRepository<FileKnowledge, String> {

    List<FileKnowledge> findAllByAnalysisId(String analysisId);

    Optional<FileKnowledge> findByAnalysisIdAndFilePath(String analysisId, String filePath);

    long countAllByAnalysisId(String analysisId);

    void deleteAllByAnalysisId(String analysisId);


    interface FileKnowledgeLossRiskProjection {
        String getFilePath();
        String getFileName();
        KnowledgeRisk getKnowledgeRisk();
        Double getKnowledgeLoss();
    }

    interface FileLeadAuthorProjection {
        String getFilePath();
        String getFileName();
        String getLeadAuthor();
        Double getLeadAuthorKnowledgePercentage();
    }


    List<FileKnowledgeLossRiskProjection> findAllKnowledgeLossRiskByAnalysisId(String analysisId);

    List<FileLeadAuthorProjection> findAllLeadAuthorsByAnalysisId(String analysisId);

}
