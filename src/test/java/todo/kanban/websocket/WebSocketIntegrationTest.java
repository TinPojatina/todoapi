package todo.kanban.websocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import todo.kanban.dto.AuthDTO;
import todo.kanban.dto.TaskDTO;
import todo.kanban.model.TaskPriority;
import todo.kanban.model.TaskStatus;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@ContextConfiguration(initializers = WebSocketIntegrationTest.TestContainersInitializer.class)
class WebSocketIntegrationTest {

  // Static container to be used during normal test execution
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:15-alpine")
          .withDatabaseName("kanban_test")
          .withUsername("test")
          .withPassword("test");
  @LocalServerPort private int port;
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  private WebSocketStompClient stompClient;

  @BeforeEach
  void setup() {
    this.stompClient =
        new WebSocketStompClient(
            new SockJsClient(List.of(new WebSocketTransport(new StandardWebSocketClient()))));
    this.stompClient.setMessageConverter(new MappingJackson2MessageConverter());
  }

  @Test
  void testWebSocketNotifications() throws Exception {
    String token = getAuthToken();
    CompletableFuture<WebSocketMessage> completableFuture = new CompletableFuture<>();

    // Connect to WebSocket
    StompSession session =
        stompClient
            .connectAsync("ws://localhost:" + port + "/ws", new StompSessionHandlerAdapter() {})
            .get(5, TimeUnit.SECONDS);

    // Subscribe to the tasks topic
    session.subscribe(
        "/topic/tasks",
        new StompFrameHandler() {
          @Override
          public @NonNull Type getPayloadType(@NonNull StompHeaders headers) {
            return WebSocketMessage.class;
          }

          @Override
          public void handleFrame(@NonNull StompHeaders headers, Object payload) {
            completableFuture.complete((WebSocketMessage) payload);
          }
        });

    // Create a task via REST API
    TaskDTO.Request createRequest = new TaskDTO.Request();
    createRequest.setTitle("WebSocket Test Task");
    createRequest.setDescription("Testing WebSocket notifications");
    createRequest.setStatus(TaskStatus.TO_DO);
    createRequest.setPriority(TaskPriority.HIGH);

    mockMvc.perform(
        post("/api/tasks")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createRequest)));

    // Wait for and verify the WebSocket message
    WebSocketMessage message = completableFuture.get(5, TimeUnit.SECONDS);
    assertNotNull(message);
    assertEquals(WebSocketMessageType.CREATED, message.getType());

    // Close the session
    session.disconnect();
  }

  private String getAuthToken() throws Exception {
    AuthDTO.RegisterRequest registerRequest = new AuthDTO.RegisterRequest();
    registerRequest.setUsername("wsuser");
    registerRequest.setPassword("password");
    registerRequest.setEmail("ws@example.com");

    // Try to register or login
    try {
      MvcResult result =
          mockMvc
              .perform(
                  post("/api/auth/register")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(objectMapper.writeValueAsString(registerRequest)))
              .andReturn();

      String responseContent = result.getResponse().getContentAsString();
      AuthDTO.TokenResponse response =
          objectMapper.readValue(responseContent, AuthDTO.TokenResponse.class);
      return response.getToken();
    } catch (Exception e) {
      // User might already exist, try login
      AuthDTO.LoginRequest loginRequest = new AuthDTO.LoginRequest();
      loginRequest.setUsername("wsuser");
      loginRequest.setPassword("password");

      MvcResult result =
          mockMvc
              .perform(
                  post("/api/auth/login")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(objectMapper.writeValueAsString(loginRequest)))
              .andReturn();

      String responseContent = result.getResponse().getContentAsString();
      AuthDTO.TokenResponse response =
          objectMapper.readValue(responseContent, AuthDTO.TokenResponse.class);
      return response.getToken();
    }
  }

  static class TestContainersInitializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(@NotNull ConfigurableApplicationContext context) {
      // Skip real container initialization if AOT generation is in progress
      if (Boolean.getBoolean("spring.aot.processing")) {
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
            context,
            "spring.datasource.url=jdbc:postgresql://localhost:5432/test",
            "spring.datasource.username=test",
            "spring.datasource.password=test");
        return;
      }

      // Normal test container setup
      postgres.start();
      TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
          context,
          "spring.datasource.url=" + postgres.getJdbcUrl(),
          "spring.datasource.username=" + postgres.getUsername(),
          "spring.datasource.password=" + postgres.getPassword());
    }
  }
}
