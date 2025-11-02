package pwr.zpi.hotspotter.authentication.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import pwr.zpi.hotspotter.authentication.component.CookieUtil;
import pwr.zpi.hotspotter.authentication.dto.LoginRequestDTO;
import pwr.zpi.hotspotter.authentication.dto.RegisterRequestDTO;
import pwr.zpi.hotspotter.authentication.dto.AuthResponseDTO;
import pwr.zpi.hotspotter.user.model.User;
import pwr.zpi.hotspotter.user.repository.UserRepository;
import pwr.zpi.hotspotter.authentication.service.JwtService;
import pwr.zpi.hotspotter.common.exceptions.ObjectNotFoundException;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final CookieUtil cookieUtil;

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(
            @RequestBody @Valid RegisterRequestDTO request,
            HttpServletResponse response) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already in use");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName());
        user.setProvider(User.AuthProvider.LOCAL);
        user.setRole(User.Role.USER);

        userRepository.save(user);

        String token = jwtService.generateToken(user);
        cookieUtil.addCookie(response, cookieUtil.createJwtCookie(token));

        return ResponseEntity.ok(new AuthResponseDTO(user));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(
            @RequestBody @Valid LoginRequestDTO request,
            HttpServletResponse response) {

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Invalid email or password");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ObjectNotFoundException("User with email " + request.getEmail() + " not found"));

        String token = jwtService.generateToken(user);
        cookieUtil.addCookie(response, cookieUtil.createJwtCookie(token));

        return ResponseEntity.ok(new AuthResponseDTO(user));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        cookieUtil.addCookie(response, cookieUtil.deleteJwtCookie());

        return ResponseEntity.ok(Map.of("success", true, "message", "Logged out successfully"));
    }

    @GetMapping("/validate")
    public ResponseEntity<AuthResponseDTO> validateToken(
            HttpServletRequest request) {

        Optional<String> tokenOpt = cookieUtil.getJwtFromCookies(request);

        if (tokenOpt.isEmpty()) {
            throw new BadCredentialsException("No token provided");
        }

        try {
            String token = tokenOpt.get();
            String email = jwtService.extractEmail(token);
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            return ResponseEntity.ok(new AuthResponseDTO(user));
        } catch (Exception e) {
            throw new BadCredentialsException("Invalid token");
        }
    }
}