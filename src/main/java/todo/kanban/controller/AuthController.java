package todo.kanban.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import todo.kanban.dto.AuthDTO;
import todo.kanban.service.AuthService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @PostMapping("/login")
  public ResponseEntity<AuthDTO.TokenResponse> login(
      @Valid @RequestBody AuthDTO.LoginRequest loginRequest) {
    return ResponseEntity.ok(authService.login(loginRequest));
  }

  @PostMapping("/register")
  public ResponseEntity<AuthDTO.TokenResponse> register(
      @Valid @RequestBody AuthDTO.RegisterRequest registerRequest) {
    return ResponseEntity.ok(authService.register(registerRequest));
  }
}
