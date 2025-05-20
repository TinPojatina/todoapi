package todo.kanban.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  @Override
  public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
    converters.stream()
        .filter(MappingJackson2HttpMessageConverter.class::isInstance)
        .map(MappingJackson2HttpMessageConverter.class::cast)
        .forEach(
            converter -> {
              List<MediaType> mediaTypes = new ArrayList<>(converter.getSupportedMediaTypes());
              mediaTypes.add(new MediaType("application", "merge-patch+json"));
              converter.setSupportedMediaTypes(mediaTypes);
            });
  }
}
