package todo.kanban.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer;

@Configuration
public class WebSocketSecurityConfig extends AbstractSecurityWebSocketMessageBrokerConfigurer {

  @Override
  protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
    messages
        .simpDestMatchers("/app/**")
        .authenticated()
        .simpSubscribeDestMatchers("/topic/tasks")
        .authenticated()
        .anyMessage()
        .authenticated();
  }

  @Override
  protected boolean sameOriginDisabled() {
    return true;
  }
}
