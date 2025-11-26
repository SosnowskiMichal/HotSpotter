package pwr.zpi.hotspotter.repositoryanalysis.mapper;

import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge.model.AuthorContribution;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge.model.FileKnowledge;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge.repository.FileKnowledgeRepository.*;
import pwr.zpi.hotspotter.repositoryanalysis.dto.FileKnowledgeDTO;
import pwr.zpi.hotspotter.repositoryanalysis.dto.FileKnowledgeLossRiskDTO;
import pwr.zpi.hotspotter.repositoryanalysis.dto.FileLeadAuthorDTO;

import java.util.List;

@Component
public class FileKnowledgeMapper {

    private static final int CONTRIBUTIONS_MAX_SIZE = 20;

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

    public FileKnowledgeDTO toReducedDTO(FileKnowledge fileKnowledge) {
        if (fileKnowledge == null) return null;

        List<FileKnowledgeDTO.AuthorContributionDTO> contributionsDTOs = fileKnowledge.getAuthorContributions().stream()
                .limit(CONTRIBUTIONS_MAX_SIZE)
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

    public FileKnowledgeLossRiskDTO toKnowledgeLossRiskDTO(FileKnowledgeLossRiskProjection projection, double normalizedValue) {
        if (projection == null) return null;

        return new FileKnowledgeLossRiskDTO(
                projection.getFilePath(),
                projection.getKnowledgeRisk(),
                projection.getKnowledgeLoss(),
                normalizedValue
        );
    }

    public FileLeadAuthorDTO toLeadAuthorDTO(FileLeadAuthorProjection projection) {
        if (projection == null) return null;

        return new FileLeadAuthorDTO(
                projection.getFilePath(),
                projection.getLeadAuthor()
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
