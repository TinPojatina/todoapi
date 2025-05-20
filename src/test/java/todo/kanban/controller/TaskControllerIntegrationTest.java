package todo.kanban.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import todo.kanban.dto.AuthDTO;
import todo.kanban.dto.TaskDTO;
import todo.kanban.model.TaskPriority;
import todo.kanban.model.TaskStatus;
import todo.kanban.security.JwtUtils;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@ContextConfiguration(initializers = TaskControllerIntegrationTest.TestContainersInitializer.class)
class TaskControllerIntegrationTest {

  // Static container to be used during normal test execution
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:15-alpine")
          .withDatabaseName("kanban_test")
          .withUsername("test")
          .withPassword("test");
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JwtUtils jwtUtils;

  private String getAuthToken() throws Exception {
    AuthDTO.RegisterRequest registerRequest = new AuthDTO.RegisterRequest();
    registerRequest.setUsername("testuser");
    registerRequest.setPassword("password");
    registerRequest.setEmail("test@example.com");

    // Try to register or login
    try {
      MvcResult result =
          mockMvc
              .perform(
                  post("/api/auth/register")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(objectMapper.writeValueAsString(registerRequest)))
              .andReturn();

      AuthDTO.TokenResponse response =
          objectMapper.readValue(
              result.getResponse().getContentAsString(), AuthDTO.TokenResponse.class);
      return response.getToken();
    } catch (Exception e) {
      // User might already exist, try login
      AuthDTO.LoginRequest loginRequest = new AuthDTO.LoginRequest();
      loginRequest.setUsername("testuser");
      loginRequest.setPassword("password");

      MvcResult result =
          mockMvc
              .perform(
                  post("/api/auth/login")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(objectMapper.writeValueAsString(loginRequest)))
              .andReturn();

      AuthDTO.TokenResponse response =
          objectMapper.readValue(
              result.getResponse().getContentAsString(), AuthDTO.TokenResponse.class);
      return response.getToken();
    }
  }

  @Test
  void crudOperationsTest() throws Exception {
    String token = getAuthToken();

    // 1. Create a task
    TaskDTO.Request createRequest = new TaskDTO.Request();
    createRequest.setTitle("Test Task");
    createRequest.setDescription("Test Description");
    createRequest.setStatus(TaskStatus.TO_DO);
    createRequest.setPriority(TaskPriority.HIGH);

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/tasks")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title", is(createRequest.getTitle())))
            .andExpect(jsonPath("$.description", is(createRequest.getDescription())))
            .andExpect(jsonPath("$.status", is(createRequest.getStatus().toString())))
            .andExpect(jsonPath("$.priority", is(createRequest.getPriority().toString())))
            .andExpect(jsonPath("$.id", notNullValue()))
            .andReturn();

    String createResponse = createResult.getResponse().getContentAsString();
    TaskDTO.Response createdTask = objectMapper.readValue(createResponse, TaskDTO.Response.class);

    // 2. Get the task by ID
    mockMvc
        .perform(
            get("/api/tasks/{id}", createdTask.getId()).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(createdTask.getId().intValue())))
        .andExpect(jsonPath("$.title", is(createdTask.getTitle())));

    // 3. Update the task
    TaskDTO.Request updateRequest = new TaskDTO.Request();
    updateRequest.setTitle("Updated Task");
    updateRequest.setDescription("Updated Description");
    updateRequest.setStatus(TaskStatus.IN_PROGRESS);
    updateRequest.setPriority(TaskPriority.LOW);

    mockMvc
        .perform(
            put("/api/tasks/{id}", createdTask.getId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(createdTask.getId().intValue())))
        .andExpect(jsonPath("$.title", is(updateRequest.getTitle())))
        .andExpect(jsonPath("$.description", is(updateRequest.getDescription())))
        .andExpect(jsonPath("$.status", is(updateRequest.getStatus().toString())))
        .andExpect(jsonPath("$.priority", is(updateRequest.getPriority().toString())));

    // 4. Patch the task
    String patchJson = "{\"status\": \"DONE\"}";

    mockMvc
        .perform(
            patch("/api/tasks/{id}", createdTask.getId())
                .header("Authorization", "Bearer " + token)
                .contentType("application/merge-patch+json")
                .content(patchJson))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(createdTask.getId().intValue())))
        .andExpect(jsonPath("$.title", is(updateRequest.getTitle())))
        .andExpect(jsonPath("$.status", is("DONE")));

    // 5. List tasks
    mockMvc
        .perform(get("/api/tasks").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
        .andExpect(
            jsonPath(
                "$.content[?(@.id == " + createdTask.getId() + ")].title",
                hasItem(updateRequest.getTitle())));

    // 6. Delete the task
    mockMvc
        .perform(
            delete("/api/tasks/{id}", createdTask.getId())
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isNoContent());

    // 7. Verify the task is deleted
    mockMvc
        .perform(
            get("/api/tasks/{id}", createdTask.getId()).header("Authorization", "Bearer " + token))
        .andExpect(status().isNotFound());
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
