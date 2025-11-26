package pwr.zpi.hotspotter.repositoryanalysis.dto;

import pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge.model.KnowledgeRisk;

public record FileKnowledgeLossRiskDTO(
        String path,
        KnowledgeRisk knowledgeRisk,
        Double knowledgeLoss,
        Double normalizedValue
) { }
