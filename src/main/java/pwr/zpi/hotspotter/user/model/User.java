package pwr.zpi.hotspotter.user.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import pwr.zpi.hotspotter.user.model.analysispreferences.UserAnalysisPreferences;

import java.time.LocalDateTime;

@Data
@Document(collection = "users")
public class User {

    @Id
    private String id;
    @Indexed(unique = true)
    private String email;
    private String password; // null for Google users
    private String name;
    private AuthProvider provider;
    private String providerId; // Google user ID
    private Role role = Role.USER;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UserAnalysisPreferences analysisPreferences;

    public User() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        this.analysisPreferences = new UserAnalysisPreferences();
    }

    public enum AuthProvider {
        LOCAL,
        GOOGLE
    }

    public enum Role {
        USER,
        ADMIN
    }
}


