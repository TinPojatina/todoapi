package todo.kanban.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import todo.kanban.dto.TaskDTO;
import todo.kanban.exception.ConflictException;
import todo.kanban.exception.IllegalOperationException;
import todo.kanban.exception.ResourceNotFoundException;
import todo.kanban.mapper.Mapper;
import todo.kanban.model.Task;
import todo.kanban.model.TaskPriority;
import todo.kanban.model.TaskStatus;
import todo.kanban.model.User;
import todo.kanban.repository.TaskRepository;
import todo.kanban.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

  private static final List<String> VALID_SORT_PROPERTIES =
      Arrays.asList("id", "title", "status", "priority", "createdAt", "updatedAt", "version");
  private final TaskRepository taskRepository;
  private final UserRepository userRepository;
  private final Mapper taskMapper;
  private final WebSocketService webSocketService;

  /**
   * Get all tasks with sorting, pagination, and filtering
   *
   * @param pageable Pagination parameters
   * @param request HTTP request for logging
   * @return Page of task DTOs
   */
  @Cacheable(value = "tasksList", key = "#pageable.pageNumber + #pageable.pageSize")
  public Page<TaskDTO.Response> getAllTasks(Pageable pageable, HttpServletRequest request) {
    log.info("Getting all tasks");

    // Create a new pageable with fixed sorting by creation date (newest first)
    pageable =
        PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            Sort.by(Sort.Direction.DESC, "createdAt"));
    log.debug("Using pageable: {}", pageable);

    try {
      Page<Task> tasks = taskRepository.findAll(pageable);
      log.info("Found {} tasks", tasks.getTotalElements());
      return tasks.map(taskMapper::toDto);
    } catch (Exception e) {
      log.error("Error retrieving tasks: {}", e.getMessage(), e);
      throw e;
    }
  }

  /**
   * Search tasks by criteria
   *
   * @param title Optional title search term
   * @param status Optional status filter
   * @param priority Optional priority filter
   * @param assignedTo Optional user assignment filter
   * @param pageable Pagination parameters
   * @return Page of tasks matching criteria
   */
  public Page<TaskDTO.Response> searchTasks(
      String title, TaskStatus status, TaskPriority priority, Long assignedTo, Pageable pageable) {
    log.info(
        "Searching tasks with title: {}, status: {}, priority: {}, assignedTo: {}",
        title,
        status,
        priority,
        assignedTo);

    pageable = validateAndAdjustPageable(pageable);

    Specification<Task> spec = Specification.where(null);

    if (title != null && !title.isBlank()) {
      spec =
          spec.and(
              (root, query, cb) ->
                  cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase() + "%"));
    }

    if (status != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
    }

    if (priority != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("priority"), priority));
    }

    if (assignedTo != null) {
      spec =
          spec.and(
              (root, query, cb) -> {
                if (assignedTo == 0) {
                  return cb.isNull(root.get("assignedTo"));
                } else {
                  return cb.equal(root.get("assignedTo").get("id"), assignedTo);
                }
              });
    }

    Page<Task> tasks = taskRepository.findAll(spec, pageable);
    log.info("Found {} tasks matching criteria", tasks.getTotalElements());

    return tasks.map(taskMapper::toDto);
  }

  /**
   * Get a task by ID
   *
   * @param id Task ID
   * @return Task DTO
   */
  @Cacheable(value = "tasks", key = "#id")
  public TaskDTO.Response getTaskById(Long id) {
    log.info("Getting task by id: {}", id);
    Task task = findTaskById(id);
    return taskMapper.toDto(task);
  }

  /**
   * Create a new task
   *
   * @param taskRequest Task creation request
   * @return Created task DTO
   */
  @Transactional
  public TaskDTO.Response createTask(TaskDTO.Request taskRequest) {
    log.info("Creating task: {}", taskRequest);
    validateTaskRequest(taskRequest, null);

    try {
      User currentUser = getCurrentUser();
      log.info("Current user: {}", currentUser.getUsername());

      Task task = taskMapper.toEntity(taskRequest, currentUser);

      // Set default values if not provided
      if (task.getStatus() == null) {
        task.setStatus(TaskStatus.TO_DO);
      }

      if (task.getPriority() == null) {
        task.setPriority(TaskPriority.MED);
      }

      // Handle assigned user if specified
      if (taskRequest.getAssignedTo() != null) {
        User assignedUser =
            userRepository
                .findById(taskRequest.getAssignedTo())
                .orElseThrow(
                    () ->
                        new ResourceNotFoundException(
                            "User not found with id: " + taskRequest.getAssignedTo()));
        task.setAssignedTo(assignedUser);
        log.info("Assigned task to user: {}", assignedUser.getUsername());
      }

      Task savedTask = taskRepository.save(task);
      log.info("Task saved with id: {}", savedTask.getId());

      TaskDTO.Response response = taskMapper.toDto(savedTask);
      webSocketService.notifyTaskCreated(response);

      return response;
    } catch (OptimisticLockingFailureException e) {
      log.error("Optimistic locking failure when creating task", e);
      throw new ConflictException(
          "Task creation failed due to concurrent modification. Please try again.");
    } catch (Exception e) {
      log.error("Error creating task", e);
      throw e;
    }
  }

  /**
   * Update a task
   *
   * @param id Task ID
   * @param taskRequest Task update request
   * @return Updated task DTO
   */
  @Transactional
  @CachePut(value = "tasks", key = "#id")
  @CacheEvict(value = "tasksList", allEntries = true)
  public TaskDTO.Response updateTask(Long id, TaskDTO.Request taskRequest) {
    log.info("Updating task with id: {}, request: {}", id, taskRequest);
    try {
      Task task = findTaskById(id);
      log.info("Found task to update: {}", task.getTitle());

      // Validate request data and task status transition
      validateTaskRequest(taskRequest, task);

      taskMapper.updateEntityFromDto(taskRequest, task);

      if (taskRequest.getAssignedTo() != null) {
        User assignedUser =
            userRepository
                .findById(taskRequest.getAssignedTo())
                .orElseThrow(
                    () ->
                        new ResourceNotFoundException(
                            "User not found with id: " + taskRequest.getAssignedTo()));
        task.setAssignedTo(assignedUser);
        log.info("Updated assigned user to: {}", assignedUser.getUsername());
      } else if (task.getAssignedTo() != null) {
        // Clear assignment if null is explicitly provided
        task.setAssignedTo(null);
        log.info("Cleared task assignment");
      }

      Task updatedTask = taskRepository.save(task);
      log.info("Task updated successfully");

      TaskDTO.Response response = taskMapper.toDto(updatedTask);
      webSocketService.notifyTaskUpdated(response);

      return response;
    } catch (OptimisticLockingFailureException e) {
      log.error("Optimistic locking failure when updating task", e);
      throw new ConflictException(
          "Task was updated by another user. Please refresh and try again.");
    } catch (Exception e) {
      log.error("Error updating task: {}", e.getMessage(), e);
      throw e;
    }
  }

  /**
   * Partially update a task
   *
   * @param id Task ID
   * @param updates Map of fields to update
   * @return Updated task DTO
   */
  @Transactional
  @CachePut(value = "tasks", key = "#id")
  @CacheEvict(value = "tasksList", allEntries = true)
  public TaskDTO.Response patchTask(Long id, Map<String, Object> updates) {
    log.info("Patching task with id: {}, updates: {}", id, updates);

    try {
      Task task = findTaskById(id);
      log.info("Found task to patch: {}", task.getTitle());

      // Validate updates
      validatePatchUpdates(updates, task);

      // Handle updates based on the patch
      if (updates.containsKey("title") && updates.get("title") != null) {
        String title = (String) updates.get("title");
        task.setTitle(title);
        log.info("Updated title to: {}", task.getTitle());
      }

      if (updates.containsKey("description")) {
        task.setDescription((String) updates.get("description"));
        log.info("Updated description");
      }

      if (updates.containsKey("status") && updates.get("status") != null) {
        TaskStatus newStatus = TaskStatus.valueOf((String) updates.get("status"));

        // Check if status transition is valid
        if (isValidStatusTransition(task.getStatus(), newStatus)) {
          throw new IllegalOperationException(
              "Invalid status transition from " + task.getStatus() + " to " + newStatus);
        }

        task.setStatus(newStatus);
        log.info("Updated status to: {}", task.getStatus());
      }

      if (updates.containsKey("priority") && updates.get("priority") != null) {
        task.setPriority(TaskPriority.valueOf((String) updates.get("priority")));
        log.info("Updated priority to: {}", task.getPriority());
      }

      if (updates.containsKey("assignedTo")) {
        if (updates.get("assignedTo") == null) {
          task.setAssignedTo(null);
          log.info("Removed assignment");
        } else {
          Long assignedToId = ((Number) updates.get("assignedTo")).longValue();
          User assignedUser =
              userRepository
                  .findById(assignedToId)
                  .orElseThrow(
                      () ->
                          new ResourceNotFoundException("User not found with id: " + assignedToId));
          task.setAssignedTo(assignedUser);
          log.info("Updated assigned user to: {}", assignedUser.getUsername());
        }
      }

      Task updatedTask = taskRepository.save(task);
      log.info("Task patched successfully");

      TaskDTO.Response response = taskMapper.toDto(updatedTask);
      webSocketService.notifyTaskUpdated(response);

      return response;
    } catch (OptimisticLockingFailureException e) {
      log.error("Optimistic locking failure when patching task", e);
      throw new ConflictException(
          "Task was updated by another user. Please refresh and try again.");
    } catch (Exception e) {
      log.error("Error patching task: {}", e.getMessage(), e);
      throw e;
    }
  }

  /**
   * Delete a task
   *
   * @param id Task ID
   */
  @Transactional
  @CacheEvict(
      value = {"tasks", "tasksList"},
      allEntries = true)
  public void deleteTask(Long id) {
    log.info("Deleting task with id: {}", id);
    try {
      Task task = findTaskById(id);
      log.info("Found task to delete: {}", task.getTitle());

      // Check if the task can be deleted (e.g., not in certain states)
      if (task.getStatus() == TaskStatus.IN_PROGRESS) {
        throw new IllegalOperationException(
            "Cannot delete a task that is in progress. Please move it to another status first.");
      }

      taskRepository.delete(task);
      log.info("Task deleted successfully");

      webSocketService.notifyTaskDeleted(id);
    } catch (OptimisticLockingFailureException e) {
      log.error("Optimistic locking failure when deleting task", e);
      throw new ConflictException(
          "Task was updated by another user. Please refresh and try again.");
    } catch (IllegalOperationException e) {
      log.error("Illegal operation: {}", e.getMessage());
      throw e;
    } catch (Exception e) {
      log.error("Error deleting task: {}", e.getMessage(), e);
      throw e;
    }
  }

  /**
   * Get tasks assigned to a specific user
   *
   * @param userId User ID
   * @param pageable Pagination parameters
   * @return Page of tasks assigned to the user
   */
  public Page<TaskDTO.Response> getTasksAssignedToUser(Long userId, Pageable pageable) {
    log.info("Getting tasks assigned to user with id: {}", userId);

    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

    pageable = validateAndAdjustPageable(pageable);

    Page<Task> tasks = taskRepository.findByAssignedToId(userId, pageable);
    log.info("Found {} tasks assigned to user {}", tasks.getTotalElements(), user.getUsername());

    return tasks.map(taskMapper::toDto);
  }

  /**
   * Get tasks created by a specific user
   *
   * @param userId User ID
   * @param pageable Pagination parameters
   * @return Page of tasks created by the user
   */
  public Page<TaskDTO.Response> getTasksCreatedByUser(Long userId, Pageable pageable) {
    log.info("Getting tasks created by user with id: {}", userId);

    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

    pageable = validateAndAdjustPageable(pageable);

    Page<Task> tasks = taskRepository.findByCreatedById(userId, pageable);
    log.info("Found {} tasks created by user {}", tasks.getTotalElements(), user.getUsername());

    return tasks.map(taskMapper::toDto);
  }

  /**
   * Find task by ID
   *
   * @param id Task ID
   * @return Task entity
   * @throws ResourceNotFoundException if task not found
   */
  private Task findTaskById(Long id) {
    return taskRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
  }

  /**
   * Get the current authenticated user
   *
   * @return User entity
   * @throws ResourceNotFoundException if user not found
   */
  private User getCurrentUser() {
    UserDetails userDetails =
        (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    return userRepository
        .findByUsername(userDetails.getUsername())
        .orElseThrow(
            () -> new ResourceNotFoundException("User not found: " + userDetails.getUsername()));
  }

  /**
   * Validate task request
   *
   * @param taskRequest Task request to validate
   * @param existingTask Existing task (null for create operations)
   * @throws IllegalArgumentException if validation fails
   */
  private void validateTaskRequest(TaskDTO.Request taskRequest, Task existingTask) {
    if (taskRequest.getTitle() != null
        && (taskRequest.getTitle().length() < 3 || taskRequest.getTitle().length() > 100)) {
      throw new IllegalArgumentException("Title must be between 3 and 100 characters");
    }

    if (taskRequest.getDescription() != null && taskRequest.getDescription().length() > 1000) {
      throw new IllegalArgumentException("Description cannot exceed 1000 characters");
    }

    // Validate status transition for existing tasks
    if (existingTask != null
        && taskRequest.getStatus() != null
        && isValidStatusTransition(existingTask.getStatus(), taskRequest.getStatus())) {
      throw new IllegalOperationException(
          "Invalid status transition from "
              + existingTask.getStatus()
              + " to "
              + taskRequest.getStatus());
    }
  }

  /**
   * Validate patch updates
   *
   * @param updates Map of fields to update
   * @param existingTask Existing task
   * @throws IllegalArgumentException if validation fails
   */
  private void validatePatchUpdates(Map<String, Object> updates, Task existingTask) {
    if (updates.containsKey("title") && updates.get("title") != null) {
      String title = (String) updates.get("title");
      if (title.length() < 3 || title.length() > 100) {
        throw new IllegalArgumentException("Title must be between 3 and 100 characters");
      }
    }

    if (updates.containsKey("description") && updates.get("description") != null) {
      String description = (String) updates.get("description");
      if (description.length() > 1000) {
        throw new IllegalArgumentException("Description cannot exceed 1000 characters");
      }
    }

    if (updates.containsKey("status") && updates.get("status") != null) {
      try {
        TaskStatus newStatus = TaskStatus.valueOf((String) updates.get("status"));

        if (isValidStatusTransition(existingTask.getStatus(), newStatus)) {
          throw new IllegalOperationException(
              "Invalid status transition from " + existingTask.getStatus() + " to " + newStatus);
        }
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("Invalid task status: " + updates.get("status"));
      }
    }

    if (updates.containsKey("priority") && updates.get("priority") != null) {
      try {
        TaskPriority.valueOf((String) updates.get("priority"));
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("Invalid task priority: " + updates.get("priority"));
      }
    }
  }

  /**
   * Check if status transition is valid
   *
   * @param currentStatus Current task status
   * @param newStatus New task status
   * @return true if transition is valid, false otherwise
   */
  private boolean isValidStatusTransition(TaskStatus currentStatus, TaskStatus newStatus) {
    if (currentStatus == newStatus) {
      return false;
    }

    switch (currentStatus) {
      case TO_DO:
        return newStatus != TaskStatus.IN_PROGRESS;
      case IN_PROGRESS:
        return newStatus != TaskStatus.TO_DO && newStatus != TaskStatus.DONE;
      case DONE:
        return newStatus != TaskStatus.IN_PROGRESS;
      default:
        return true;
    }
  }

  /**
   * Validate and adjust pageable request
   *
   * @param pageable Original pageable request
   * @return Validated and adjusted pageable
   */
  private Pageable validateAndAdjustPageable(Pageable pageable) {
    Sort sort = pageable.getSort();

    // Check if the sort properties are valid
    boolean hasInvalid =
        sort.stream()
            .map(Sort.Order::getProperty)
            .anyMatch(
                property -> !VALID_SORT_PROPERTIES.contains(property.replaceAll("[\\[\\]\"]", "")));

    if (hasInvalid || sort.isEmpty()) {
      log.debug(
          "Invalid sort properties detected or sort is empty. Using default sort: updatedAt,DESC");
      Sort defaultSort = Sort.by(Sort.Direction.DESC, "updatedAt");
      return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), defaultSort);
    }

    return pageable;
  }
}
