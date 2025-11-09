package pwr.zpi.hotspotter.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pwr.zpi.hotspotter.authentication.annotation.CurrentUser;
import pwr.zpi.hotspotter.user.model.User;
import pwr.zpi.hotspotter.user.model.analysispreferences.UserAnalysisPreferences;
import pwr.zpi.hotspotter.user.service.UserService;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(@CurrentUser User user) {
        return ResponseEntity.ok(new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                user.getProvider(),
                user.getAnalysisPreferences()
        ));
    }

    @GetMapping("/preferences/analysis")
    public ResponseEntity<UserAnalysisPreferences> getAnalysisPreferences(@CurrentUser User user) {
        return ResponseEntity.ok(user.getAnalysisPreferences());
    }

    @PutMapping("/preferences/analysis")
    public ResponseEntity<UserAnalysisPreferences> updateAnalysisPreferences(@CurrentUser User user, @RequestBody UserAnalysisPreferences userAnalysisPreferences) {
        user.setAnalysisPreferences(userAnalysisPreferences);
        userService.updateUser(user);

        return ResponseEntity.ok(user.getAnalysisPreferences());
    }

    public record UserProfileResponse(
            String id,
            String email,
            String name,
            User.Role role,
            User.AuthProvider provider,
            UserAnalysisPreferences analysisPreferences
    ) {}
}
