package todo.kanban.Security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;
import todo.kanban.security.JwtUtils;
import todo.kanban.service.UserDetailsServiceImpl;

@ExtendWith(MockitoExtension.class)
class JwtUtilsTest {

  @Mock private UserDetailsServiceImpl userDetailsService;

  @InjectMocks private JwtUtils jwtUtils;

  @Test
  void generateTokenAndValidateToken() {
    // Set up JWT properties
    ReflectionTestUtils.setField(jwtUtils, "jwtSecret", "testSecret123456789012345678901234567890");
    ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 60000L); // 1 minute

    // Generate a token
    String token = jwtUtils.generateToken("testuser");

    // Verify token is valid
    assertTrue(jwtUtils.validateToken(token));

    // Verify username is correctly extracted
    assertEquals("testuser", jwtUtils.getUsernameFromToken(token));
  }

  @Test
  void getAuthentication() {
    // Set up JWT properties
    ReflectionTestUtils.setField(jwtUtils, "jwtSecret", "testSecret123456789012345678901234567890");
    ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 60000L); // 1 minute

    // Generate a token
    String token = jwtUtils.generateToken("testuser");

    // Set up user details
    UserDetails userDetails = new User("testuser", "password", Collections.emptyList());
    when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);

    // Verify authentication
    assertNotNull(jwtUtils.getAuthentication(token));
    assertEquals("testuser", jwtUtils.getAuthentication(token).getName());
  }
}
