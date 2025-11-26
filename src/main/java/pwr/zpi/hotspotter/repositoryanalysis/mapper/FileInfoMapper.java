package pwr.zpi.hotspotter.repositoryanalysis.mapper;

import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo.model.FileInfo;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo.repository.FileInfoRepository.*;
import pwr.zpi.hotspotter.repositoryanalysis.dto.*;

@Component
public class FileInfoMapper {

    public FileInfoDTO toDTO(FileInfo fileInfo) {
        if (fileInfo == null) return null;

        return new FileInfoDTO(
                fileInfo.getFilePath(),
                fileInfo.getFileName(),
                fileInfo.getFileType(),
                fileInfo.getFileSize(),
                fileInfo.getFileUrl(),

                fileInfo.getTotalLines(),
                fileInfo.getCodeLines(),
                fileInfo.getCommentLines(),
                fileInfo.getBlankLines(),

                fileInfo.getTotalCommits(),
                fileInfo.getCommitsLastMonth(),
                fileInfo.getCommitsLastYear(),

                fileInfo.getFirstCommitDate(),
                fileInfo.getLastCommitDate(),
                fileInfo.getCodeAgeDays()
        );
    }

    public RepositoryItemDTO toRepositoryItemDTO(RepositoryItemProjection projection) {
        if (projection == null) return null;

        return new RepositoryItemDTO(
                projection.getFilePath(),
                projection.getFileName(),
                "file"
        );
    }

    public FileTypeDTO toTypeDTO(FileTypeProjection projection) {
        if (projection == null) return null;

        return new FileTypeDTO(
                projection.getFilePath(),
                projection.getFileType()
        );
    }

    public FileCodeAgeDTO toCodeAgeDTO(FileCodeAgeProjection projection, Double normalizedValue) {
        if (projection == null) return null;

        return new FileCodeAgeDTO(
                projection.getFilePath(),
                projection.getCodeAgeDays(),
                normalizedValue
        );
    }

    public HotspotDTO toHotspotDTO(HotspotProjection projection, Double normalizedValue) {
        if (projection == null) return null;

        return new HotspotDTO(
                projection.getFilePath(),
                normalizedValue
        );
    }

}
