package pwr.zpi.hotspotter.repositoryanalysis.analyzer.activitytrends.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.activitytrends.model.ActivityTrends;

@Repository
public interface ActivityTrendsRepository extends MongoRepository<ActivityTrends, String> { }
