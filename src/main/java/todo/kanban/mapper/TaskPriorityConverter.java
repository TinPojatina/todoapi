package todo.kanban.mapper;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.sql.SQLException;
import org.postgresql.util.PGobject;
import todo.kanban.model.TaskPriority;

@Converter(autoApply = true)
public class TaskPriorityConverter implements AttributeConverter<TaskPriority, Object> {
  @Override
  public Object convertToDatabaseColumn(TaskPriority attribute) {
    if (attribute == null) {
      return null;
    }
    try {
      PGobject pgObject = new PGobject();
      pgObject.setType("task_priority");
      pgObject.setValue(attribute.name());
      return pgObject;
    } catch (SQLException e) {
      throw new RuntimeException("Error converting TaskPriority to database column", e);
    }
  }

  @Override
  public TaskPriority convertToEntityAttribute(Object dbData) {
    if (dbData == null) {
      return null;
    }

    String value;
    if (dbData instanceof PGobject) {
      value = ((PGobject) dbData).getValue();
    } else {
      value = dbData.toString();
    }

    return TaskPriority.valueOf(value);
  }
}
