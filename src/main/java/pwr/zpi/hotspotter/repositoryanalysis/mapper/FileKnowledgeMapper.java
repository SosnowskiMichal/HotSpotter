package pwr.zpi.hotspotter.repositoryanalysis.mapper;

import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge.model.AuthorContribution;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge.model.FileKnowledge;
import pwr.zpi.hotspotter.repositoryanalysis.dto.FileKnowledgeDTO;
import pwr.zpi.hotspotter.repositoryanalysis.dto.FileKnowledgeLossRiskDTO;
import pwr.zpi.hotspotter.repositoryanalysis.dto.FileLeadAuthorDTO;

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

    public FileKnowledgeLossRiskDTO toKnowledgeLossRiskDTO(FileKnowledge fileKnowledge, double normalizedValue) {
        if (fileKnowledge == null) return null;

        return new FileKnowledgeLossRiskDTO(
                fileKnowledge.getFilePath(),
                fileKnowledge.getFileName(),
                fileKnowledge.getKnowledgeRisk(),
                fileKnowledge.getKnowledgeLoss(),
                normalizedValue
        );
    }

    public FileLeadAuthorDTO toLeadAuthorDTO(FileKnowledge fileKnowledge) {
        if (fileKnowledge == null) return null;

        return new FileLeadAuthorDTO(
                fileKnowledge.getFilePath(),
                fileKnowledge.getFileName(),
                fileKnowledge.getLeadAuthor(),
                fileKnowledge.getLeadAuthorKnowledgePercentage()
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
