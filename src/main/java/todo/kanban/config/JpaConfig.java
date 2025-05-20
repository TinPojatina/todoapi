package todo.kanban.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import todo.kanban.model.TaskPriority;
import todo.kanban.model.TaskStatus;

@Configuration
public class JpaConfig {

  @Bean
  public Converter<String, TaskStatus> taskStatusConverter() {
    return new TaskStatusConverter();
  }

  @Bean
  public Converter<String, TaskPriority> taskPriorityConverter() {
    return new TaskPriorityConverter();
  }

  // Explicit converter classes
  private static class TaskStatusConverter implements Converter<String, TaskStatus> {
    @Override
    public TaskStatus convert(String source) {
      return TaskStatus.valueOf(source.trim().toUpperCase());
    }
  }

  private static class TaskPriorityConverter implements Converter<String, TaskPriority> {
    @Override
    public TaskPriority convert(String source) {
      return TaskPriority.valueOf(source.trim().toUpperCase());
    }
  }
}
