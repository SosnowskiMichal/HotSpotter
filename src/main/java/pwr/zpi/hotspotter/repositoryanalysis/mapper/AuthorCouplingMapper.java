package pwr.zpi.hotspotter.repositoryanalysis.mapper;

import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.coupling.model.AuthorCoupling;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.coupling.model.CoupledAuthor;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.coupling.repository.AuthorCouplingRepository.*;
import pwr.zpi.hotspotter.repositoryanalysis.dto.AuthorCouplingDTO;

import java.util.List;

@Component
public class AuthorCouplingMapper {

    public AuthorCouplingDTO toDTO(AuthorCouplingDataProjection projection) {
        if (projection == null) return null;

        List<AuthorCouplingDTO.CoupledAuthorDTO> coupledAuthorsDTOs = projection.getCoupledAuthors().stream()
                .map(this::toCoupledAuthorDTO)
                .toList();

        return new AuthorCouplingDTO(
                projection.getAuthor(),
                projection.getFilesChanged(),
                projection.getTotalChanges(),
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
