package todo.kanban.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import todo.kanban.dto.TaskDTO;
import todo.kanban.service.TaskService;

@Controller
@RequiredArgsConstructor
public class TaskGraphQLController {

  private final TaskService taskService;

  @QueryMapping
  public Page<TaskDTO.Response> tasks(@Argument Integer page, @Argument Integer size) {
    HttpServletRequest request =
        ((ServletRequestAttributes)
                Objects.requireNonNull(RequestContextHolder.getRequestAttributes()))
            .getRequest();

    Pageable pageable = PageRequest.of(page != null ? page : 0, size != null ? size : 10);

    return taskService.getAllTasks(pageable, request);
  }

  @QueryMapping
  public TaskDTO.Response task(@Argument Long id) {
    return taskService.getTaskById(id);
  }

  @MutationMapping
  public TaskDTO.Response createTask(@Argument TaskInput input) {
    TaskDTO.Request request = convertInputToRequest(input);
    return taskService.createTask(request);
  }

  @MutationMapping
  public TaskDTO.Response updateTask(@Argument Long id, @Argument TaskInput input) {
    TaskDTO.Request request = convertInputToRequest(input);
    return taskService.updateTask(id, request);
  }

  @MutationMapping
  public Boolean deleteTask(@Argument Long id) {
    taskService.deleteTask(id);
    return true;
  }

  private TaskDTO.Request convertInputToRequest(TaskInput input) {
    return TaskDTO.Request.builder()
        .title(input.title())
        .description(input.description())
        .status(input.status())
        .priority(input.priority())
        .assignedTo(input.assignedTo())
        .build();
  }
}
