package pwr.zpi.hotspotter.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pwr.zpi.hotspotter.user.model.User;
import pwr.zpi.hotspotter.user.repository.UserRepository;
import pwr.zpi.hotspotter.common.exceptions.ObjectNotFoundException;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public User loadUserEntityByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ObjectNotFoundException("User not found with email: " + email));
    }

    public User loadUserEntityById(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ObjectNotFoundException("User not found with id: " + id));
    }

    public User updateUser(User user) {
        return userRepository.save(user);
    }
}
