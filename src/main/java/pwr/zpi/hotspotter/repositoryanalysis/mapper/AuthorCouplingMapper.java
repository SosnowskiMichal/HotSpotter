package pwr.zpi.hotspotter.repositoryanalysis.mapper;

import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.coupling.model.AuthorCoupling;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.coupling.model.CoupledAuthor;
import pwr.zpi.hotspotter.repositoryanalysis.dto.AuthorCouplingDTO;

import java.util.List;

@Component
public class AuthorCouplingMapper {

    public AuthorCouplingDTO toDTO(AuthorCoupling authorCoupling) {
        if (authorCoupling == null) return null;

        List<AuthorCouplingDTO.CoupledAuthorDTO> coupledAuthorsDTOs = authorCoupling.getCoupledAuthors().stream()
                .map(this::toCoupledAuthorDTO)
                .toList();

        return new AuthorCouplingDTO(
                authorCoupling.getAuthor(),
                authorCoupling.getFilesChanged(),
                authorCoupling.getTotalChanges(),
                coupledAuthorsDTOs
        );
    }

    private AuthorCouplingDTO.CoupledAuthorDTO toCoupledAuthorDTO(CoupledAuthor coupledAuthor) {
        return new AuthorCouplingDTO.CoupledAuthorDTO(
                coupledAuthor.getAuthor(),
                coupledAuthor.getSharedFilesChanged(),
                coupledAuthor.getSharedChanges(),
                coupledAuthor.getCouplingPercentage()
        );
    }

}
