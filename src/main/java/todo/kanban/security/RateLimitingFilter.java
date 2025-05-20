package todo.kanban.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

  private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String requestURI = request.getRequestURI();
    if (requestURI.contains("/swagger-ui")
        || requestURI.contains("/v3/api-docs")
        || requestURI.contains("/actuator")) {
      filterChain.doFilter(request, response);
      return;
    }

    String clientIP = getClientIP(request);
    Bucket bucket = buckets.computeIfAbsent(clientIP, this::createNewBucket);

    if (bucket.tryConsume(1)) {
      filterChain.doFilter(request, response);
    } else {
      log.warn("Rate limit exceeded for IP: {}", clientIP);
      response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
      response.setContentType("application/json");
      response
          .getWriter()
          .write("{\"status\":429,\"message\":\"Rate limit exceeded. Try again later.\"}");
    }
  }

  private Bucket createNewBucket(String ip) {
    // 100 requests per minute
    Bandwidth limit = Bandwidth.classic(100, Refill.greedy(100, Duration.ofMinutes(1)));
    return Bucket.builder().addLimit(limit).build();
  }

  private String getClientIP(HttpServletRequest request) {
    String xForwardedForHeader = request.getHeader("X-Forwarded-For");
    if (xForwardedForHeader != null) {
      return xForwardedForHeader.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}
