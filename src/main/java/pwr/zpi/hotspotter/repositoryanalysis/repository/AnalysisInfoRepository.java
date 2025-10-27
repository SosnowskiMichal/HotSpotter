package pwr.zpi.hotspotter.repositoryanalysis.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import pwr.zpi.hotspotter.repositoryanalysis.model.AnalysisInfo;

import java.util.List;

public interface AnalysisInfoRepository extends MongoRepository<AnalysisInfo, String> {

    List<AnalysisInfo> findByRepositoryUrl(String repositoryUrl);

}
