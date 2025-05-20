package todo.kanban.service;

import todo.kanban.dto.TaskDTO;
import todo.kanban.exception.ResourceNotFoundException;
import todo.kanban.mapper.Mapper;
import todo.kanban.model.Task;
import todo.kanban.model.TaskPriority;
import todo.kanban.model.TaskStatus;
import todo.kanban.model.User;
import todo.kanban.repository.TaskRepository;
import todo.kanban.repository.UserRepository;
import todo.kanban.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final Mapper taskMapper;
    private final WebSocketService webSocketService;

    public Page<TaskDTO.Response> getAllTasks(TaskStatus status, Pageable pageable) {
        if (status != null) {
            return taskRepository.findByStatus(status, pageable)
                    .map(taskMapper::toDto);
        }
        return taskRepository.findAll(pageable)
                .map(taskMapper::toDto);
    }

    public TaskDTO.Response getTaskById(Long id) {
        Task task = findTaskById(id);
        return taskMapper.toDto(task);
    }

    @Transactional
    public TaskDTO.Response createTask(TaskDTO.Request taskRequest) {
        User currentUser = getCurrentUser();

        Task task = taskMapper.toEntity(taskRequest, currentUser);

        if (taskRequest.getAssignedTo() != null) {
            User assignedUser = userRepository.findById(taskRequest.getAssignedTo())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + taskRequest.getAssignedTo()));
            task.setAssignedTo(assignedUser);
        }

        Task savedTask = taskRepository.save(task);
        TaskDTO.Response response = taskMapper.toDto(savedTask);

        webSocketService.notifyTaskCreated(response);

        return response;
    }

    @Transactional
    public TaskDTO.Response updateTask(Long id, TaskDTO.Request taskRequest) {
        Task task = findTaskById(id);

        taskMapper.updateEntityFromDto(taskRequest, task);

        if (taskRequest.getAssignedTo() != null) {
            User assignedUser = userRepository.findById(taskRequest.getAssignedTo())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + taskRequest.getAssignedTo()));
            task.setAssignedTo(assignedUser);
        }

        Task updatedTask = taskRepository.save(task);
        TaskDTO.Response response = taskMapper.toDto(updatedTask);

        webSocketService.notifyTaskUpdated(response);

        return response;
    }

    @Transactional
    public TaskDTO.Response patchTask(Long id, Map<String, Object> updates) {
        Task task = findTaskById(id);

        // Handle updates based on the patch
        if (updates.containsKey("title")) {
            task.setTitle((String) updates.get("title"));
        }

        if (updates.containsKey("description")) {
            task.setDescription((String) updates.get("description"));
        }

        if (updates.containsKey("status")) {
            task.setStatus(TaskStatus.valueOf((String) updates.get("status")));
        }

        if (updates.containsKey("priority")) {
            task.setPriority(TaskPriority.valueOf((String) updates.get("priority")));
        }

        if (updates.containsKey("assignedTo")) {
            Long assignedToId = ((Number) updates.get("assignedTo")).longValue();
            User assignedUser = userRepository.findById(assignedToId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + assignedToId));
            task.setAssignedTo(assignedUser);
        }

        Task updatedTask = taskRepository.save(task);
        TaskDTO.Response response = taskMapper.toDto(updatedTask);

        webSocketService.notifyTaskUpdated(response);

        return response;
    }

    @Transactional
    public void deleteTask(Long id) {
        Task task = findTaskById(id);
        taskRepository.delete(task);
        webSocketService.notifyTaskDeleted(id);
    }

    private Task findTaskById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
    }

    private User getCurrentUser() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userDetails.getUsername()));
    }
}