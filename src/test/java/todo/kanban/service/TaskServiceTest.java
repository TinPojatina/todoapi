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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Mapper taskMapper;

    @Mock
    private WebSocketService webSocketService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private TaskService taskService;

    @Test
    void getAllTasks_whenStatusIsNull_returnAllTasks() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Task> tasks = List.of(new Task(), new Task());
        Page<Task> taskPage = new PageImpl<>(tasks, pageable, tasks.size());

        when(taskRepository.findAll(pageable)).thenReturn(taskPage);
        when(taskMapper.toDto(any(Task.class))).thenReturn(new TaskDTO.Response());

        // When
        Page<TaskDTO.Response> result = taskService.getAllTasks(null, pageable);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        verify(taskRepository).findAll(pageable);
        verify(taskMapper, times(2)).toDto(any(Task.class));
    }

    @Test
    void getAllTasks_whenStatusIsProvided_returnFilteredTasks() {
        // Given
        TaskStatus status = TaskStatus.TO_DO;
        Pageable pageable = PageRequest.of(0, 10);
        List<Task> tasks = List.of(new Task());
        Page<Task> taskPage = new PageImpl<>(tasks, pageable, tasks.size());

        when(taskRepository.findByStatus(status, pageable)).thenReturn(taskPage);
        when(taskMapper.toDto(any(Task.class))).thenReturn(new TaskDTO.Response());

        // When
        Page<TaskDTO.Response> result = taskService.getAllTasks(status, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(taskRepository).findByStatus(status, pageable);
        verify(taskMapper).toDto(any(Task.class));
    }

    @Test
    void getTaskById_whenTaskExists_returnTask() {
        // Given
        Long taskId = 1L;
        Task task = new Task();
        TaskDTO.Response expectedResponse = new TaskDTO.Response();

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(taskMapper.toDto(task)).thenReturn(expectedResponse);

        // When
        TaskDTO.Response result = taskService.getTaskById(taskId);

        // Then
        assertNotNull(result);
        assertEquals(expectedResponse, result);
        verify(taskRepository).findById(taskId);
        verify(taskMapper).toDto(task);
    }

    @Test
    void getTaskById_whenTaskDoesNotExist_throwResourceNotFoundException() {
        // Given
        Long taskId = 1L;
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> taskService.getTaskById(taskId));
        verify(taskRepository).findById(taskId);
    }

    @Test
    void createTask_whenValidRequest_createAndReturnTask() {
        // Given
        User currentUser = new User();
        currentUser.setUsername("testuser");

        TaskDTO.Request request = new TaskDTO.Request();
        request.setTitle("Test Task");
        request.setStatus(TaskStatus.TO_DO);
        request.setPriority(TaskPriority.MED);

        Task createdTask = new Task();
        TaskDTO.Response expectedResponse = new TaskDTO.Response();

        mockSecurityContext(currentUser.getUsername());
        when(userRepository.findByUsername(currentUser.getUsername())).thenReturn(Optional.of(currentUser));
        when(taskMapper.toEntity(request, currentUser)).thenReturn(createdTask);
        when(taskRepository.save(createdTask)).thenReturn(createdTask);
        when(taskMapper.toDto(createdTask)).thenReturn(expectedResponse);

        // When
        TaskDTO.Response result = taskService.createTask(request);

        // Then
        assertNotNull(result);
        assertEquals(expectedResponse, result);
        verify(taskRepository).save(createdTask);
        verify(webSocketService).notifyTaskCreated(expectedResponse);
    }

    @Test
    void updateTask_whenTaskExists_updateAndReturnTask() {
        // Given
        Long taskId = 1L;
        Task existingTask = new Task();

        TaskDTO.Request request = new TaskDTO.Request();
        request.setTitle("Updated Task");

        Task updatedTask = new Task();
        TaskDTO.Response expectedResponse = new TaskDTO.Response();

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(existingTask));
        doNothing().when(taskMapper).updateEntityFromDto(request, existingTask);
        when(taskRepository.save(existingTask)).thenReturn(updatedTask);
        when(taskMapper.toDto(updatedTask)).thenReturn(expectedResponse);

        // When
        TaskDTO.Response result = taskService.updateTask(taskId, request);

        // Then
        assertNotNull(result);
        assertEquals(expectedResponse, result);
        verify(taskRepository).save(existingTask);
        verify(webSocketService).notifyTaskUpdated(expectedResponse);
    }

    @Test
    void patchTask_whenTaskExists_partialUpdateAndReturnTask() {
        // Given
        Long taskId = 1L;
        Task existingTask = new Task();

        Map<String, Object> updates = Map.of(
                "title", "Patched Title",
                "status", "IN_PROGRESS"
        );

        Task updatedTask = new Task();
        TaskDTO.Response expectedResponse = new TaskDTO.Response();

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(existingTask));
        when(taskRepository.save(existingTask)).thenReturn(updatedTask);
        when(taskMapper.toDto(updatedTask)).thenReturn(expectedResponse);

        // When
        TaskDTO.Response result = taskService.patchTask(taskId, updates);

        // Then
        assertNotNull(result);
        assertEquals(expectedResponse, result);
        verify(taskRepository).save(existingTask);
        verify(webSocketService).notifyTaskUpdated(expectedResponse);
    }

    @Test
    void deleteTask_whenTaskExists_deleteAndNotify() {
        // Given
        Long taskId = 1L;
        Task existingTask = new Task();

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(existingTask));

        // When
        taskService.deleteTask(taskId);

        // Then
        verify(taskRepository).delete(existingTask);
        verify(webSocketService).notifyTaskDeleted(taskId);
    }

    private void mockSecurityContext(String username) {
        when(userDetails.getUsername()).thenReturn(username);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }
}