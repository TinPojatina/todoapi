package todo.kanban.mapper;

import org.springframework.stereotype.Component;
import todo.kanban.dto.TaskDTO;
import todo.kanban.model.Task;
import todo.kanban.model.TaskPriority;
import todo.kanban.model.TaskStatus;
import todo.kanban.model.User;

@Component
public class Mapper {

  public Task toEntity(TaskDTO.Request dto, User createdBy) {
    return Task.builder()
        .title(dto.getTitle())
        .description(dto.getDescription())
        .status(dto.getStatus() != null ? dto.getStatus() : TaskStatus.TO_DO)
        .priority(dto.getPriority() != null ? dto.getPriority() : TaskPriority.MED)
        .createdBy(createdBy)
        .build();
  }

  public TaskDTO.Response toDto(Task task) {
    TaskDTO.Response.ResponseBuilder responseBuilder =
        TaskDTO.Response.builder()
            .id(task.getId())
            .title(task.getTitle())
            .description(task.getDescription())
            .status(task.getStatus())
            .priority(task.getPriority())
            .createdAt(task.getCreatedAt())
            .updatedAt(task.getUpdatedAt())
            .version(task.getVersion());

    if (task.getCreatedBy() != null) {
      responseBuilder
          .createdBy(task.getCreatedBy().getId())
          .createdByUsername(task.getCreatedBy().getUsername());
    }

    if (task.getAssignedTo() != null) {
      responseBuilder
          .assignedTo(task.getAssignedTo().getId())
          .assignedToUsername(task.getAssignedTo().getUsername());
    }

    return responseBuilder.build();
  }

  public void updateEntityFromDto(TaskDTO.Request dto, Task task) {
    if (dto.getTitle() != null) {
      task.setTitle(dto.getTitle());
    }
    if (dto.getDescription() != null) {
      task.setDescription(dto.getDescription());
    }
    if (dto.getStatus() != null) {
      task.setStatus(dto.getStatus());
    }
    if (dto.getPriority() != null) {
      task.setPriority(dto.getPriority());
    }
  }
}
