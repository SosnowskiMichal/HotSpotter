package pwr.zpi.hotspotter.repositoryanalysis.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FileDataDTO(
    FileInfoDTO info,
    FileCouplingDTO coupling,
    FileKnowledgeDTO knowledge,
    SonarAnalysisResultDTO staticAnalysis
) { }
