package pwr.zpi.hotspotter.repositoryanalysis.mapper;

import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo.model.FileInfo;
import pwr.zpi.hotspotter.repositoryanalysis.dto.FileCodeAgeDTO;
import pwr.zpi.hotspotter.repositoryanalysis.dto.FileInfoDTO;
import pwr.zpi.hotspotter.repositoryanalysis.dto.FilePathNameDTO;
import pwr.zpi.hotspotter.repositoryanalysis.dto.HotspotDTO;

@Component
public class FileInfoMapper {

    public FileInfoDTO toDTO(FileInfo fileInfo) {
        if (fileInfo == null) return null;

        return new FileInfoDTO(
                fileInfo.getFilePath(),
                fileInfo.getFileName(),
                fileInfo.getFileType(),
                fileInfo.getFileSize(),

                fileInfo.getTotalLines(),
                fileInfo.getCodeLines(),
                fileInfo.getCommentLines(),
                fileInfo.getBlankLines(),

                fileInfo.getTotalCommits(),
                fileInfo.getCommitsLastMonth(),
                fileInfo.getCommitsLastYear(),

                fileInfo.getFirstCommitDate(),
                fileInfo.getLastCommitDate(),

                fileInfo.getCodeAgeDays(),
                fileInfo.getCodeAgeMonths()
        );
    }

    public FilePathNameDTO toPathNameDTO(FileInfo fileInfo) {
        if (fileInfo == null) return null;

        return new FilePathNameDTO(
                fileInfo.getFilePath(),
                fileInfo.getFileName()
        );
    }

    public FileCodeAgeDTO toCodeAgeDTO(FileInfo fileInfo, Double normalizedValue) {
        if (fileInfo == null) return null;

        return new FileCodeAgeDTO(
                fileInfo.getFilePath(),
                fileInfo.getFileName(),
                fileInfo.getCodeAgeDays(),
                normalizedValue
        );
    }

    public HotspotDTO toHotspotDTO(FileInfo fileInfo, Double normalizedValue) {
        if (fileInfo == null) return null;

        return new HotspotDTO(
                fileInfo.getFilePath(),
                fileInfo.getFileName(),
                fileInfo.getCommitsInHotspotAnalysisPeriod(),
                fileInfo.getCodeLines(),
                normalizedValue
        );
    }

}
