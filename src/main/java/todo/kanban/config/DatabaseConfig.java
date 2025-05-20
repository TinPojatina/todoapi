package todo.kanban.config;

import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class DatabaseConfig {

  @Bean
  public DataSource dataSource(Environment env) {
    String url = env.getProperty("spring.datasource.url");
    String username = env.getProperty("spring.datasource.username");
    String password = env.getProperty("spring.datasource.password");

    DataSourceBuilder<?> dataSourceBuilder = DataSourceBuilder.create();
    dataSourceBuilder.url(url);
    dataSourceBuilder.username(username);
    dataSourceBuilder.password(password);

    int maxRetries = 10;
    int retryInterval = 5;

    for (int i = 0; i < maxRetries; i++) {
      try {
        DataSource dataSource = dataSourceBuilder.build();
        dataSource.getConnection().close();
        return dataSource;
      } catch (Exception e) {
        if (i < maxRetries - 1) {
          System.out.println(
              "Failed to connect to database, retrying in "
                  + retryInterval
                  + " seconds... (Attempt "
                  + (i + 1)
                  + " of "
                  + maxRetries
                  + ")");
          try {
            TimeUnit.SECONDS.sleep(retryInterval);
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
          }
        } else {
          throw new RuntimeException(
              "Failed to connect to database after " + maxRetries + " attempts", e);
        }
      }
    }
    return null;
  }
}
