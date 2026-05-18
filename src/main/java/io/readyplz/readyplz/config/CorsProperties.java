package io.readyplz.readyplz.config;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "app.cors")
@Validated
public class CorsProperties {

	@NotEmpty(message = "app.cors.allowed-origin-patterns must list at least one pattern")
	private List<String> allowedOriginPatterns = new ArrayList<>();

	public List<String> getAllowedOriginPatterns() {
		return allowedOriginPatterns;
	}

	public void setAllowedOriginPatterns(List<String> allowedOriginPatterns) {
		this.allowedOriginPatterns = allowedOriginPatterns;
	}
}
