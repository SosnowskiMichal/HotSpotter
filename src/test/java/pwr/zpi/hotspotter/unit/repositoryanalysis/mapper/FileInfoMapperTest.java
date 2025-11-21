package pwr.zpi.hotspotter.unit.repositoryanalysis.mapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo.model.FileInfo;
import pwr.zpi.hotspotter.repositoryanalysis.dto.FileInfoDTO;
import pwr.zpi.hotspotter.repositoryanalysis.mapper.FileInfoMapper;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class FileInfoMapperTest {

    @InjectMocks
    private FileInfoMapper mapper;

    @Test
    void returnsNullWhenFileInfoIsNull() {
        assertThat(mapper.toDTO(null)).isNull();
    }

    @Test
    void mapsAllFieldsCorrectly() {
        FileInfo fileInfo = getFileInfo();
        FileInfoDTO dto = mapper.toDTO(fileInfo);

        assertThat(dto.path()).isEqualTo("src/Main.java");
        assertThat(dto.name()).isEqualTo("Main.java");
        assertThat(dto.type()).isEqualTo("java");
        assertThat(dto.size()).isEqualTo(String.valueOf(1234L));

        assertThat(dto.totalLines()).isEqualTo(200);
        assertThat(dto.codeLines()).isEqualTo(150);
        assertThat(dto.commentLines()).isEqualTo(30);
        assertThat(dto.blankLines()).isEqualTo(20);

        assertThat(dto.totalCommits()).isEqualTo(10);
        assertThat(dto.commitsLastMonth()).isEqualTo(3);
        assertThat(dto.commitsLastYear()).isEqualTo(7);

        assertThat(dto.firstCommitDate()).isEqualTo(LocalDate.of(2020, 1, 1));
        assertThat(dto.lastCommitDate()).isEqualTo(LocalDate.of(2024, 1, 1));

        assertThat(dto.codeAgeDays()).isEqualTo(1000);
        assertThat(dto.codeAgeMonths()).isEqualTo(33);
    }

    private static FileInfo getFileInfo() {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setFilePath("src/Main.java");
        fileInfo.setFileName("Main.java");
        fileInfo.setFileType("java");
        fileInfo.setFileSize(String.valueOf(1234L));

        fileInfo.setTotalLines(200);
        fileInfo.setCodeLines(150);
        fileInfo.setCommentLines(30);
        fileInfo.setBlankLines(20);

        fileInfo.setTotalCommits(10);
        fileInfo.setCommitsLastMonth(3);
        fileInfo.setCommitsLastYear(7);

        fileInfo.setFirstCommitDate(LocalDate.of(2020, 1, 1));
        fileInfo.setLastCommitDate(LocalDate.of(2024, 1, 1));

        fileInfo.setCodeAgeDays(1000);
        fileInfo.setCodeAgeMonths(33);

        return fileInfo;
    }
}
