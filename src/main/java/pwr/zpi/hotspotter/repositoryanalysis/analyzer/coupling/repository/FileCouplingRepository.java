package pwr.zpi.hotspotter.repositoryanalysis.analyzer.coupling.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.coupling.model.CoupledFile;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.coupling.model.FileCoupling;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileCouplingRepository extends MongoRepository<FileCoupling, String> {

    List<FileCoupling> findAllByAnalysisId(String analysisId);

    Optional<FileCoupling> findByAnalysisIdAndFilePath(String analysisId, String filePath);

    long countAllByAnalysisId(String analysisId);

    void deleteAllByAnalysisId(String analysisId);


    interface FileCouplingDataProjection {
        String getFilePath();
        List<CoupledFile> getCoupledFiles();
    }


    List<FileCouplingDataProjection> findAllCouplingDataByAnalysisId(String analysisId);

}
