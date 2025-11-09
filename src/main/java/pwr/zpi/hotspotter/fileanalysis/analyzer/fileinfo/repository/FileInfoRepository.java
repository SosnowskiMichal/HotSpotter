package pwr.zpi.hotspotter.fileanalysis.analyzer.fileinfo.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import pwr.zpi.hotspotter.fileanalysis.analyzer.fileinfo.model.FileInfo;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileInfoRepository extends MongoRepository<FileInfo, String> {

    List<FileInfo> findAllByAnalysisId(String analysisId);

    Optional<FileInfo> findByAnalysisIdAndFilePath(String analysisId, String filePath);

    long countAllByAnalysisId(String analysisId);

    void deleteAllByAnalysisId(String analysisId);

}
