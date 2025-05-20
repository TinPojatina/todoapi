package todo.kanban.service;

import todo.kanban.dto.TaskDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import todo.kanban.websocket.WebSocketMessage;

@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public void notifyTaskCreated(TaskDTO.Response task) {
        WebSocketMessage message = new WebSocketMessage(com.kanban.websocket.WebSocketMessageType.CREATED, task);
        messagingTemplate.convertAndSend("/topic/tasks", message);
    }

    public void notifyTaskUpdated(TaskDTO.Response task) {
        WebSocketMessage message = new WebSocketMessage(com.kanban.websocket.WebSocketMessageType.UPDATED, task);
        messagingTemplate.convertAndSend("/topic/tasks", message);
    }

    public void notifyTaskDeleted(Long taskId) {
        WebSocketMessage message = new WebSocketMessage(com.kanban.websocket.WebSocketMessageType.DELETED, taskId);
        messagingTemplate.convertAndSend("/topic/tasks", message);
    }
}