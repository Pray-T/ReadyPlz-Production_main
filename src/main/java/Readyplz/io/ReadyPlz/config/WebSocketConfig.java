package Readyplz.io.ReadyPlz.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.lang.NonNull;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	private final ChannelInterceptor webSocketAuthChannelInterceptor;

	@Autowired
	public WebSocketConfig(ChannelInterceptor webSocketAuthChannelInterceptor) {
		this.webSocketAuthChannelInterceptor = webSocketAuthChannelInterceptor;
	}

	@Override
	public void configureMessageBroker(@NonNull MessageBrokerRegistry registry) {
		registry.enableSimpleBroker("/queue", "/topic");
		registry.setApplicationDestinationPrefixes("/app");
		registry.setUserDestinationPrefix("/user");
	}

	@Override
	public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
		registry.addEndpoint("/ws-nearby-gamers").setAllowedOriginPatterns("*").withSockJS();
	}

	@Override
	public void configureClientInboundChannel(@NonNull ChannelRegistration registration) {
		registration.interceptors(webSocketAuthChannelInterceptor);
	}
}


