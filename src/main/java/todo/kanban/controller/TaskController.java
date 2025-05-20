package todo.kanban.controller;

import todo.kanban.dto.TaskDTO;
import todo.kanban.model.TaskStatus;
import todo.kanban.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    public ResponseEntity<Page<TaskDTO.Response>> getAllTasks(
            @RequestParam(required = false) TaskStatus status,
            @PageableDefault(size = 10) Pageable pageable) {

        return ResponseEntity.ok(taskService.getAllTasks(status, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskDTO.Response> getTaskById(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.getTaskById(id));
    }

    @PostMapping
    public ResponseEntity<TaskDTO.Response> createTask(@Valid @RequestBody TaskDTO.Request taskRequest) {
        TaskDTO.Response createdTask = taskService.createTask(taskRequest);
        return new ResponseEntity<>(createdTask, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskDTO.Response> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody TaskDTO.Request taskRequest) {

        return ResponseEntity.ok(taskService.updateTask(id, taskRequest));
    }

    @PatchMapping(value = "/{id}", consumes = "application/merge-patch+json")
    public ResponseEntity<TaskDTO.Response> partialUpdateTask(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates) {

        return ResponseEntity.ok(taskService.patchTask(id, updates));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}