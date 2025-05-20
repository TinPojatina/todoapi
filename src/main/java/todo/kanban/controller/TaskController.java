package todo.kanban.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import todo.kanban.dto.TaskDTO;
import todo.kanban.model.TaskPriority;
import todo.kanban.model.TaskStatus;
import todo.kanban.service.TaskService;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Slf4j
public class TaskController {

  private final TaskService taskService;

  /** Get all tasks with optional filtering, sorting and pagination */
  @GetMapping
  public ResponseEntity<Page<TaskDTO.Response>> getAllTasks(
      @RequestParam(required = false) TaskStatus status,
      Pageable pageable,
      HttpServletRequest request) {

    return ResponseEntity.ok(taskService.getAllTasks(pageable, request));
  }

  /** Search tasks by multiple criteria */
  @GetMapping("/search")
  public ResponseEntity<Page<TaskDTO.Response>> searchTasks(
      @RequestParam(required = false) String title,
      @RequestParam(required = false) TaskStatus status,
      @RequestParam(required = false) TaskPriority priority,
      @RequestParam(required = false) Long assignedTo,
      @RequestParam(required = false, defaultValue = "0") Integer page,
      @RequestParam(required = false, defaultValue = "10") Integer size,
      @RequestParam(required = false, defaultValue = "updatedAt") String sortBy,
      @RequestParam(required = false, defaultValue = "desc") String sortDir) {

    log.debug(
        "REST request to search tasks - title: {}, status: {}, priority: {}, assignedTo: {}",
        title,
        status,
        priority,
        assignedTo);

    Sort.Direction direction =
        "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
    Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

    return ResponseEntity.ok(
        taskService.searchTasks(title, status, priority, assignedTo, pageable));
  }

  /** Get tasks assigned to a specific user */
  @GetMapping("/assigned/{userId}")
  public ResponseEntity<Page<TaskDTO.Response>> getTasksAssignedToUser(
      @PathVariable Long userId,
      @RequestParam(required = false, defaultValue = "0") Integer page,
      @RequestParam(required = false, defaultValue = "10") Integer size,
      @RequestParam(required = false, defaultValue = "updatedAt") String sortBy,
      @RequestParam(required = false, defaultValue = "desc") String sortDir) {

    log.debug("REST request to get tasks assigned to user: {}", userId);

    Sort.Direction direction =
        "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
    Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

    return ResponseEntity.ok(taskService.getTasksAssignedToUser(userId, pageable));
  }

  /** Get tasks created by a specific user */
  @GetMapping("/created/{userId}")
  public ResponseEntity<Page<TaskDTO.Response>> getTasksCreatedByUser(
      @PathVariable Long userId,
      @RequestParam(required = false, defaultValue = "0") Integer page,
      @RequestParam(required = false, defaultValue = "10") Integer size,
      @RequestParam(required = false, defaultValue = "updatedAt") String sortBy,
      @RequestParam(required = false, defaultValue = "desc") String sortDir) {

    log.debug("REST request to get tasks created by user: {}", userId);

    Sort.Direction direction =
        "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
    Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

    return ResponseEntity.ok(taskService.getTasksCreatedByUser(userId, pageable));
  }

  /** Get a task by ID */
  @GetMapping("/{id}")
  public ResponseEntity<TaskDTO.Response> getTaskById(@PathVariable Long id) {
    log.debug("REST request to get task: {}", id);
    return ResponseEntity.ok(taskService.getTaskById(id));
  }

  /** Create a new task */
  @PostMapping
  public ResponseEntity<TaskDTO.Response> createTask(
      @Valid @RequestBody TaskDTO.Request taskRequest) {
    log.debug("REST request to create task: {}", taskRequest);
    TaskDTO.Response createdTask = taskService.createTask(taskRequest);
    return new ResponseEntity<>(createdTask, HttpStatus.CREATED);
  }

  /** Update a task */
  @PutMapping("/{id}")
  public ResponseEntity<TaskDTO.Response> updateTask(
      @PathVariable Long id, @Valid @RequestBody TaskDTO.Request taskRequest) {

    log.debug("REST request to update task: {}", id);
    return ResponseEntity.ok(taskService.updateTask(id, taskRequest));
  }

  /** Partially update a task */
  @PatchMapping(value = "/{id}", consumes = "application/merge-patch+json")
  public ResponseEntity<TaskDTO.Response> partialUpdateTask(
      @PathVariable Long id, @RequestBody Map<String, Object> updates) {

    log.debug("REST request to patch task: {}", id);
    return ResponseEntity.ok(taskService.patchTask(id, updates));
  }

  /** Delete a task */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
    log.debug("REST request to delete task: {}", id);
    taskService.deleteTask(id);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }
}
