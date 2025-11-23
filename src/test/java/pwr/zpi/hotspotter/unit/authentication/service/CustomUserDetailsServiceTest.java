package pwr.zpi.hotspotter.unit.authentication.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import pwr.zpi.hotspotter.authentication.service.CustomUserDetailsService;
import pwr.zpi.hotspotter.common.exceptions.ObjectNotFoundException;
import pwr.zpi.hotspotter.user.model.User;
import pwr.zpi.hotspotter.user.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    private User sampleUser() {
        User u = new User();
        u.setId(String.valueOf(5));
        u.setEmail("test@ex.com");
        u.setPassword("secret");
        u.setRole(User.Role.ADMIN);
        return u;
    }

    @Test
    void loadUserByUsername_shouldReturnUserDetails() {
        User user = sampleUser();
        when(userRepository.findByEmail("test@ex.com")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("test@ex.com");

        assertThat(details.getUsername()).isEqualTo("test@ex.com");
        assertThat(details.getPassword()).isEqualTo("secret");
        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void loadUserByUsername_shouldThrowWhenUserNotFound() {
        when(userRepository.findByEmail("missing@ex.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("missing@ex.com"))
                .isInstanceOf(ObjectNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void loadUserByUsername_shouldUseEmptyPasswordIfNull() {
        User user = sampleUser();
        user.setPassword(null);
        when(userRepository.findByEmail("test@ex.com")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("test@ex.com");

        assertThat(details.getPassword()).isEqualTo("");
    }
}
