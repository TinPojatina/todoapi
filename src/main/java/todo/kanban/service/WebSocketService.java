package todo.kanban.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import todo.kanban.dto.TaskDTO;
import todo.kanban.websocket.WebSocketMessage;
import todo.kanban.websocket.WebSocketMessageType;

@Service
@RequiredArgsConstructor
public class WebSocketService {

  private final SimpMessagingTemplate messagingTemplate;

  public void notifyTaskCreated(TaskDTO.Response task) {
    WebSocketMessage message = new WebSocketMessage(WebSocketMessageType.CREATED, task);
    messagingTemplate.convertAndSend("/topic/tasks", message);
  }

  public void notifyTaskUpdated(TaskDTO.Response task) {
    WebSocketMessage message = new WebSocketMessage(WebSocketMessageType.UPDATED, task);
    messagingTemplate.convertAndSend("/topic/tasks", message);
  }

  public void notifyTaskDeleted(Long taskId) {
    WebSocketMessage message = new WebSocketMessage(WebSocketMessageType.DELETED, taskId);
    messagingTemplate.convertAndSend("/topic/tasks", message);
  }
}
