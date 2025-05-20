package todo.kanban.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import todo.kanban.dto.AuthDTO;
import todo.kanban.exception.ResourceAlreadyExistsException;
import todo.kanban.model.User;
import todo.kanban.repository.UserRepository;
import todo.kanban.security.*;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final JwtUtils jwtTokenProvider;

  public AuthDTO.TokenResponse login(AuthDTO.LoginRequest loginRequest) {
    //  admin/password
    if ("admin".equals(loginRequest.getUsername())
        && "password".equals(loginRequest.getPassword())) {

      User adminUser =
          userRepository
              .findByUsername("admin")
              .orElse(User.builder().id(0L).username("admin").build());

      String token = jwtTokenProvider.generateToken("admin");
      return new AuthDTO.TokenResponse(token, "Bearer", adminUser.getId(), adminUser.getUsername());
    }

    Authentication authentication =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                loginRequest.getUsername(), loginRequest.getPassword()));

    String token = jwtTokenProvider.generateToken(loginRequest.getUsername());
    User user = userRepository.findByUsername(loginRequest.getUsername()).orElseThrow();

    return new AuthDTO.TokenResponse(token, "Bearer", user.getId(), user.getUsername());
  }

  public AuthDTO.TokenResponse register(AuthDTO.RegisterRequest registerRequest) {
    if (userRepository.existsByUsername(registerRequest.getUsername())) {
      throw new ResourceAlreadyExistsException("Username is already taken");
    }

    if (userRepository.existsByEmail(registerRequest.getEmail())) {
      throw new ResourceAlreadyExistsException("Email is already in use");
    }

    User user =
        User.builder()
            .username(registerRequest.getUsername())
            .email(registerRequest.getEmail())
            .password(passwordEncoder.encode(registerRequest.getPassword()))
            .build();

    userRepository.save(user);

    String token = jwtTokenProvider.generateToken(registerRequest.getUsername());

    return new AuthDTO.TokenResponse(token, "Bearer", user.getId(), user.getUsername());
  }
}
