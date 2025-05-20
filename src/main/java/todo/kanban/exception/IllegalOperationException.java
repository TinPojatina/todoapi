package todo.kanban.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when an operation is not allowed by business rules For example, invalid task
 * status transitions
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class IllegalOperationException extends RuntimeException {
  public IllegalOperationException(String message) {
    super(message);
  }
}
