package pwr.zpi.hotspotter.user.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
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

    public User() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
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


