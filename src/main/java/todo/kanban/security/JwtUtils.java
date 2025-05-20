package todo.kanban.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Key;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import todo.kanban.service.UserDetailsServiceImpl;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtUtils {

  private final UserDetailsServiceImpl userDetailsService;

  @Value("${spring.security.jwt.secret:LongSecureRandomSecretForJWTAuthentication2025}")
  private String jwtSecret;

  @Value("${spring.security.jwt.expiration:86400000}")
  private long jwtExpirationMs;

  private Key signingKey;

  @PostConstruct
  public void init() {
    // Initialize signing key once at startup
    signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
    log.info("JWT signing key initialized");
  }

  public String generateToken(String username) {
    log.info("Generating JWT token for user: {}", username);
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

    String token =
        Jwts.builder()
            .setSubject(username)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(signingKey, SignatureAlgorithm.HS256)
            .compact();

    log.info("JWT token generated with expiration: {}", expiryDate);
    return token;
  }

  public String getUsernameFromToken(String token) {
    try {
      Claims claims =
          Jwts.parserBuilder().setSigningKey(signingKey).build().parseClaimsJws(token).getBody();

      String username = claims.getSubject();
      log.debug("Username extracted from token: {}", username);
      return username;
    } catch (Exception e) {
      log.error("Could not extract username from token", e);
      return null;
    }
  }

  public boolean validateToken(String token) {
    try {
      Jwts.parserBuilder().setSigningKey(signingKey).build().parseClaimsJws(token);

      log.debug("Token validated successfully");
      return true;
    } catch (Exception e) {
      log.error("Token validation failed", e);
      return false;
    }
  }

  public Authentication getAuthentication(String token) {
    log.debug("Getting authentication for token");
    UserDetails userDetails = userDetailsService.loadUserByUsername(getUsernameFromToken(token));
    log.debug("User details loaded for: {}", userDetails.getUsername());
    return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
  }

  public String resolveToken(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");
    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
      String token = bearerToken.substring(7);
      log.debug(
          "Token resolved from request: {}",
          token.substring(0, Math.min(10, token.length())) + "...");
      return token;
    }
    return null;
  }
}
