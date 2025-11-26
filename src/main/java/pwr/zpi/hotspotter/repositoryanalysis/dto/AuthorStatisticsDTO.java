package pwr.zpi.hotspotter.repositoryanalysis.dto;

import java.time.LocalDate;
import java.util.Set;

public record AuthorStatisticsDTO(
        String name,
        Set<String> emails,

        LocalDate firstCommitDate,
        LocalDate lastCommitDate,
        Boolean isActive,

        Integer daysSinceLastCommit,
        Integer daysSinceFirstCommit,

        Integer commits,
        Integer linesAdded,
        Integer linesDeleted,

        Integer existingFilesModified,
        Integer filesAsLeadAuthor
) { }
