package pwr.zpi.hotspotter.repositoryanalysis.mapper;

import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo.model.FileInfo;
import pwr.zpi.hotspotter.repositoryanalysis.dto.FileInfoDTO;

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

}
