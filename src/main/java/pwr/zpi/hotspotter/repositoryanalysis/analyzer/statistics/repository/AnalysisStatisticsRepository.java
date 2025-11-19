package pwr.zpi.hotspotter.repositoryanalysis.analyzer.statistics.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.statistics.model.AnalysisStatistics;

@Repository
public interface AnalysisStatisticsRepository extends MongoRepository<AnalysisStatistics, String> { }
