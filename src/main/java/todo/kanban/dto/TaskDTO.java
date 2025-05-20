package todo.kanban.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import todo.kanban.model.TaskPriority;
import todo.kanban.model.TaskStatus;
import todo.kanban.validation.EnumValidator;

public class TaskDTO {

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Request {
    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    private String title;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @EnumValidator(
        enumClass = TaskStatus.class,
        message = "Invalid task status. Allowed values are: {allowedValues}")
    private TaskStatus status;

    @EnumValidator(
        enumClass = TaskPriority.class,
        message = "Invalid task priority. Allowed values are: {allowedValues}")
    private TaskPriority priority;

    private Long assignedTo;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Response {
    private Long id;
    private String title;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;
    private Long createdBy;
    private String createdByUsername;
    private Long assignedTo;
    private String assignedToUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;
  }
}
