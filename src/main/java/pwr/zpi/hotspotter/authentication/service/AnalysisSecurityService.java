package pwr.zpi.hotspotter.authentication.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import pwr.zpi.hotspotter.repositoryanalysis.model.AnalysisInfo;
import pwr.zpi.hotspotter.repositoryanalysis.repository.AnalysisInfoRepository;
import pwr.zpi.hotspotter.user.model.User;
import pwr.zpi.hotspotter.user.service.UserService;

@Service("analysisSecurity")
@RequiredArgsConstructor
public class AnalysisSecurityService {

    private final AnalysisInfoRepository analysisInfoRepository;
    private final UserService userService;

    public boolean canRead(Authentication authentication, String analysisId) {
        AnalysisInfo analysis = analysisInfoRepository.findById(analysisId).orElse(null);
        if (analysis == null) {
            return true;
        }

        if (analysis.isPublic()) {
            return true;
        }

        if (authentication == null || !authentication.isAuthenticated() ||
                authentication instanceof AnonymousAuthenticationToken) {
            return false;
        }

        String email = authentication.getName();
        User user = userService.loadUserEntityByEmail(email);

        if (user.getRole() == User.Role.ADMIN) {
            return true;
        }

        return analysis.isOwnedByUser(user);
    }
}
