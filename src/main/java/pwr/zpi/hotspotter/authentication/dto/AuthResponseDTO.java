package pwr.zpi.hotspotter.authentication.dto;

import lombok.Data;
import pwr.zpi.hotspotter.user.model.User;

@Data
public class AuthResponseDTO {
    private String userId;
    private String email;
    private String name;
    private String role;

    public AuthResponseDTO(User user) {
        this.userId = user.getId();
        this.email = user.getEmail();
        this.name = user.getName();
        this.role = user.getRole().name();
    }
}
