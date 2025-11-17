package pwr.zpi.hotspotter.repositoryanalysis.mapper;

import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge.model.AuthorContribution;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge.model.FileKnowledge;
import pwr.zpi.hotspotter.repositoryanalysis.dto.FileKnowledgeDTO;

import java.util.List;

@Component
public class FileKnowledgeMapper {

    public FileKnowledgeDTO toDTO(FileKnowledge fileKnowledge) {
        if (fileKnowledge == null) return null;

        List<FileKnowledgeDTO.AuthorContributionDTO> contributionsDTOs = fileKnowledge.getAuthorContributions().stream()
                .map(this::toAuthorContributionDTO)
                .toList();

        return new FileKnowledgeDTO(
                fileKnowledge.getLinesAdded(),
                fileKnowledge.getLeadAuthor(),
                fileKnowledge.getLeadAuthorKnowledgePercentage(),
                fileKnowledge.getAuthors(),
                fileKnowledge.getActiveAuthors(),
                fileKnowledge.getKnowledgeLoss(),
                fileKnowledge.getKnowledgeRisk(),
                contributionsDTOs
        );
    }

    private FileKnowledgeDTO.AuthorContributionDTO toAuthorContributionDTO(AuthorContribution contribution) {
        return new FileKnowledgeDTO.AuthorContributionDTO(
                contribution.getName(),
                contribution.getLinesAdded(),
                contribution.getCommits(),
                contribution.getContributionPercentage()
        );
    }

}
