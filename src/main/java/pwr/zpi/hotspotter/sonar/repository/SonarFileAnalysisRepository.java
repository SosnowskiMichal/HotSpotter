package pwr.zpi.hotspotter.sonar.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import pwr.zpi.hotspotter.sonar.model.fileanalysis.SonarFileAnalysisResult;

public interface SonarFileAnalysisRepository extends MongoRepository<SonarFileAnalysisResult, String> { }
