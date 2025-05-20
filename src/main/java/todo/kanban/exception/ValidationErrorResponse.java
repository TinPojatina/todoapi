package todo.kanban.exception;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ValidationErrorResponse {
  private int status;
  private String message;
  private Map<String, String> errors;
  private LocalDateTime timestamp;
}
