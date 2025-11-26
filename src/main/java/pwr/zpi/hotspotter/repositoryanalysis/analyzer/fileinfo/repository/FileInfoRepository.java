package pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo.repository;

import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo.dto.FileTypeMetrics;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo.dto.LineStatistics;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo.model.FileInfo;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileInfoRepository extends MongoRepository<FileInfo, String> {

    List<FileInfo> findAllByAnalysisId(String analysisId);

    Optional<FileInfo> findByAnalysisIdAndFilePath(String analysisId, String filePath);

    int countAllByAnalysisId(String analysisId);

    void deleteAllByAnalysisId(String analysisId);


    @Aggregation(pipeline = {
            "{ $match: { analysisId: ?0 } }",
            "{ $group: { " +
            " _id: null, " +
            "  codeLines: { $sum: '$codeLines' }, " +
            "  commentLines: { $sum: '$commentLines' }, " +
            "  blankLines: { $sum: '$blankLines' } " +
            "} }"
    })
    LineStatistics getLineStatisticsByAnalysisId(String analysisId);

    @Aggregation(pipeline = {
            "{ $match: { analysisId: ?0, fileType: { $ne: null } } }",
            "{ $group: { " +
            "  _id: '$fileType', " +
            "  files: { $sum: 1 }, " +
            "  codeLines: { $sum: '$codeLines' }, " +
            "  commentLines: { $sum: '$commentLines' }, " +
            "  blankLines: { $sum: '$blankLines' } " +
            "} }",
            "{ $project: { " +
            "  _id: 0, " +
            "  fileType: '$_id', " +
            "  files: 1, " +
            "  codeLines: 1, " +
            "  commentLines: 1, " +
            "  blankLines: 1 " +
            "} }",
            "{ $sort: { files: -1 } }"
    })
    List<FileTypeMetrics> getFileTypeMetricsByAnalysisId(String analysisId);


    interface FilePathNameProjection {
        String getFilePath();
        String getFileName();
    }

    interface FileTypeProjection {
        String getFilePath();
        String getFileType();
    }

    interface FileCodeAgeProjection {
        String getFilePath();
        Integer getCodeAgeDays();
    }

    interface HotspotProjection {
        String getFilePath();
        Integer getCodeLines();
        Integer getCommitsInHotspotAnalysisPeriod();
    }


    List<FilePathNameProjection> findAllPathNamesByAnalysisId(String analysisId);

    List<FileTypeProjection> findAllTypesByAnalysisId(String analysisId);

    List<FileCodeAgeProjection> findAllCodeAgesByAnalysisId(String analysisId);

    List<HotspotProjection> findAllHotspotsByAnalysisId(String analysisId);

}
