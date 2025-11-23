package pwr.zpi.hotspotter.unit.repositoryanalysis.mapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge.model.AuthorContribution;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge.model.FileKnowledge;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge.model.KnowledgeRisk;
import pwr.zpi.hotspotter.repositoryanalysis.dto.FileKnowledgeDTO;
import pwr.zpi.hotspotter.repositoryanalysis.mapper.FileKnowledgeMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class FileKnowledgeMapperTest {

    @InjectMocks
    private FileKnowledgeMapper mapper;

    @Test
    void returnsNullWhenInputNull() {
        assertThat(mapper.toDTO(null)).isNull();
    }

    @Test
    void mapsAllFieldsCorrectly() {
        FileKnowledge knowledge = new FileKnowledge(
                "id",
                "analysisId",
                "file/path/Main.java",
                "Main.java",
                120,
                15,
                List.of(
                        new AuthorContribution("Alice", 90, 10, 75.0),
                        new AuthorContribution("Bob", 30, 5, 25.0)
                ),
                "Alice",
                75.0,
                3,
                2,
                10.0,
                KnowledgeRisk.SINGLE_OWNER
        );

        FileKnowledgeDTO dto = mapper.toDTO(knowledge);

        assertThat(dto.totalLinesAdded()).isEqualTo(120);
        assertThat(dto.leadAuthor()).isEqualTo("Alice");
        assertThat(dto.leadAuthorPercentage()).isEqualTo(75.0);
        assertThat(dto.authors()).isEqualTo(3);
        assertThat(dto.activeAuthors()).isEqualTo(2);
        assertThat(dto.knowledgeLoss()).isEqualTo(10.0);
        assertThat(dto.knowledgeRisk()).isEqualTo(KnowledgeRisk.SINGLE_OWNER);

        assertThat(dto.contributions()).hasSize(2);
        assertThat(dto.contributions().getFirst().name()).isEqualTo("Alice");
        assertThat(dto.contributions().getFirst().linesAdded()).isEqualTo(90);
        assertThat(dto.contributions().getFirst().commits()).isEqualTo(10);
        assertThat(dto.contributions().getFirst().percentage()).isEqualTo(75.0);
    }
}
