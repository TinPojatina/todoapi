package todo.kanban.Mapper;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import todo.kanban.dto.TaskDTO;
import todo.kanban.mapper.Mapper;
import todo.kanban.model.Task;
import todo.kanban.model.TaskPriority;
import todo.kanban.model.TaskStatus;
import todo.kanban.model.User;

class MapperTest {

  private final Mapper mapper = new Mapper();

  @Test
  void toEntity() {
    // Given
    TaskDTO.Request dto = new TaskDTO.Request();
    dto.setTitle("Test Task");
    dto.setDescription("Test Description");
    dto.setStatus(TaskStatus.IN_PROGRESS);
    dto.setPriority(TaskPriority.HIGH);

    User user = new User();
    user.setId(1L);
    user.setUsername("testuser");

    // When
    Task task = mapper.toEntity(dto, user);

    // Then
    assertEquals(dto.getTitle(), task.getTitle());
    assertEquals(dto.getDescription(), task.getDescription());
    assertEquals(dto.getStatus(), task.getStatus());
    assertEquals(dto.getPriority(), task.getPriority());
    assertEquals(user, task.getCreatedBy());
  }

  @Test
  void toDto() {
    // Given
    Task task = new Task();
    task.setId(1L);
    task.setTitle("Test Task");
    task.setDescription("Test Description");
    task.setStatus(TaskStatus.DONE);
    task.setPriority(TaskPriority.LOW);

    User createdBy = new User();
    createdBy.setId(1L);
    task.setCreatedBy(createdBy);

    User assignedTo = new User();
    assignedTo.setId(2L);
    task.setAssignedTo(assignedTo);

    // When
    TaskDTO.Response dto = mapper.toDto(task);

    // Then
    assertEquals(task.getId(), dto.getId());
    assertEquals(task.getTitle(), dto.getTitle());
    assertEquals(task.getDescription(), dto.getDescription());
    assertEquals(task.getStatus(), dto.getStatus());
    assertEquals(task.getPriority(), dto.getPriority());
    assertEquals(task.getCreatedBy().getId(), dto.getCreatedBy());
    assertEquals(task.getAssignedTo().getId(), dto.getAssignedTo());
  }

  @Test
  void updateEntityFromDto() {
    // Given
    Task task = new Task();
    task.setTitle("Initial Title");
    task.setDescription("Initial Description");
    task.setStatus(TaskStatus.TO_DO);
    task.setPriority(TaskPriority.MED);

    TaskDTO.Request dto = new TaskDTO.Request();
    dto.setTitle("Updated Title");
    dto.setDescription("Updated Description");
    dto.setStatus(TaskStatus.IN_PROGRESS);
    dto.setPriority(TaskPriority.HIGH);

    // When
    mapper.updateEntityFromDto(dto, task);

    // Then
    assertEquals(dto.getTitle(), task.getTitle());
    assertEquals(dto.getDescription(), task.getDescription());
    assertEquals(dto.getStatus(), task.getStatus());
    assertEquals(dto.getPriority(), task.getPriority());
  }
}
