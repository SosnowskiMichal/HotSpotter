package pwr.zpi.hotspotter.repositoryanalysis.analyzer.activitytrends.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.activitytrends.model.ActivityTrends;

public interface ActivityTrendsRepository extends MongoRepository<ActivityTrends, String> { }
