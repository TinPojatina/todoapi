package todo.kanban.mapper;

import todo.kanban.dto.TaskDTO;
import todo.kanban.model.Task;
import todo.kanban.model.TaskPriority;
import todo.kanban.model.TaskStatus;
import todo.kanban.model.User;
import org.springframework.stereotype.Component;

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
        return TaskDTO.Response.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .createdBy(task.getCreatedBy() != null ? task.getCreatedBy().getId() : null)
                .assignedTo(task.getAssignedTo() != null ? task.getAssignedTo().getId() : null)
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .version(task.getVersion())
                .build();
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