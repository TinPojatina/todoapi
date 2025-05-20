package todo.kanban.websocket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {
    private com.kanban.websocket.WebSocketMessageType type;
    private Object payload;
}