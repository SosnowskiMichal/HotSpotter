package pwr.zpi.hotspotter.repositoryanalysis.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import pwr.zpi.hotspotter.repositoryanalysis.model.AnalysisInfo;

import java.util.List;

@Repository
public interface AnalysisInfoRepository extends MongoRepository<AnalysisInfo, String> {

    List<AnalysisInfo> findByRepositoryUrl(String repositoryUrl);

}
