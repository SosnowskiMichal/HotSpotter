package pwr.zpi.hotspotter.repositoryanalysis.dto;

import java.time.LocalDate;

public record FileInfoDTO(
        String path,
        String name,
        String type,
        String size,

        Integer totalLines,
        Integer codeLines,
        Integer commentLines,
        Integer blankLines,

        Integer totalCommits,
        Integer commitsLastMonth,
        Integer commitsLastYear,

        LocalDate firstCommitDate,
        LocalDate lastCommitDate,

        Integer codeAgeDays,
        Integer codeAgeMonths
) { }
