package io.readyplz.readyplz.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "jwt")
@Validated
public class JwtProperties {

	private static final int MIN_KEY_BYTES = 32;

	@NotBlank
	private String secret;

	@NotNull
	private JwtSecretEncoding secretEncoding = JwtSecretEncoding.UTF8;

	@Positive
	private long accessTokenValidity;

	@Positive
	private long refreshTokenValidity;

	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}

	public JwtSecretEncoding getSecretEncoding() {
		return secretEncoding;
	}

	public void setSecretEncoding(JwtSecretEncoding secretEncoding) {
		this.secretEncoding = secretEncoding;
	}

	public long getAccessTokenValidity() {
		return accessTokenValidity;
	}

	public void setAccessTokenValidity(long accessTokenValidity) {
		this.accessTokenValidity = accessTokenValidity;
	}

	public long getRefreshTokenValidity() {
		return refreshTokenValidity;
	}

	public void setRefreshTokenValidity(long refreshTokenValidity) {
		this.refreshTokenValidity = refreshTokenValidity;
	}

	public static int getMinKeyBytes() {
		return MIN_KEY_BYTES;
	}
}
