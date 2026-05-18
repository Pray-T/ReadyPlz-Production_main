package io.readyplz.readyplz.util;

import io.readyplz.readyplz.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

@Slf4j
@Component
public class JwtTokenUtil {

	public static final String CLAIM_ROLES = "roles";
	public static final String CLAIM_TYPE = "type";
	public static final String TOKEN_TYPE_REFRESH = "refresh";

	private final SecretKey signingKey;
	private final long accessTokenValidity;
	private final long refreshTokenValidity;

	public JwtTokenUtil(JwtProperties jwtProperties) {
		this.signingKey = buildSigningKey(jwtProperties);
		this.accessTokenValidity = jwtProperties.getAccessTokenValidity();
		this.refreshTokenValidity = jwtProperties.getRefreshTokenValidity();
	}

	private static SecretKey buildSigningKey(JwtProperties props) {
		byte[] raw = decodeSecretBytes(props);
		if (raw.length < JwtProperties.getMinKeyBytes()) {
			throw new IllegalStateException(
					"jwt.secret must yield at least " + JwtProperties.getMinKeyBytes()
							+ " bytes for HS256 (256-bit minimum); got " + raw.length);
		}
		return Keys.hmacShaKeyFor(raw);
	}

	private static byte[] decodeSecretBytes(JwtProperties props) {
		String trimmed = props.getSecret().trim();
		if (trimmed.isEmpty()) {
			throw new IllegalStateException("jwt.secret must not be blank");
		}
		try {
			return switch (props.getSecretEncoding()) {
				case UTF8 -> trimmed.getBytes(StandardCharsets.UTF_8);
				case BASE64 -> Base64.getDecoder().decode(trimmed);
			};
		} catch (IllegalArgumentException e) {
			throw new IllegalStateException("jwt.secret could not be decoded as " + props.getSecretEncoding(), e);
		}
	}

	private Claims extractAllClaims(String token) {
		try {
			return Jwts.parser()
					.verifyWith(signingKey)
					.build()
					.parseSignedClaims(token)
					.getPayload();
		} catch (JwtException e) {
			log.error("JWT 토큰 파싱 실패: {}", e.getMessage());
			throw e;
		}
	}

	public String extractUsername(String token) {
		return extractClaim(token, Claims::getSubject);
	}

	public Date extractExpiration(String token) {
		return extractClaim(token, Claims::getExpiration);
	}

	public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
		final Claims claims = extractAllClaims(token);
		return claimsResolver.apply(claims);
	}

	private Boolean isTokenExpired(String token) {
		return extractExpiration(token).before(new Date());
	}

	@NonNull
	public String generateAccessToken(UserDetails userDetails) {
		Map<String, Object> claims = new HashMap<>();
		claims.put(CLAIM_ROLES, userDetails.getAuthorities());
		return createToken(claims, userDetails.getUsername(), accessTokenValidity);
	}

	@NonNull
	public String generateRefreshToken(UserDetails userDetails) {
		Map<String, Object> claims = new HashMap<>();
		claims.put(CLAIM_TYPE, TOKEN_TYPE_REFRESH);
		return createToken(claims, userDetails.getUsername(), refreshTokenValidity);
	}

	@NonNull
	private String createToken(Map<String, Object> claims, String subject, Long validity) {
		return Objects.requireNonNull(Jwts.builder()
				.claims(claims)
				.subject(subject)
				.issuedAt(new Date(System.currentTimeMillis()))
				.expiration(new Date(System.currentTimeMillis() + validity))
				.signWith(signingKey, Jwts.SIG.HS256)
				.compact());
	}

	public Boolean validateToken(String token, UserDetails userDetails) {
		final String username = extractUsername(token);
		return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
	}

	public Boolean isRefreshToken(String token) {
		try {
			Claims claims = extractAllClaims(token);
			return TOKEN_TYPE_REFRESH.equals(claims.get(CLAIM_TYPE));
		} catch (Exception e) {
			return false;
		}
	}
}
