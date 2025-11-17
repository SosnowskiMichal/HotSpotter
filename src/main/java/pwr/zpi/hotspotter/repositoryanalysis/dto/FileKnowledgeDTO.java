package pwr.zpi.hotspotter.repositoryanalysis.dto;

import pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge.model.KnowledgeRisk;

import java.util.List;

public record FileKnowledgeDTO(
    Integer totalLinesAdded,
    String leadAuthor,
    Double leadAuthorPercentage,
    Integer authors,
    Integer activeAuthors,
    Double knowledgeLoss,
    KnowledgeRisk knowledgeRisk,
    List<AuthorContributionDTO> contributions
) {
    public record AuthorContributionDTO(
        String name,
        Integer linesAdded,
        Integer commits,
        Double percentage
    ) { }
}
