package io.readyplz.readyplz.config;

/**
 * JWT HMAC 서명용 비밀 문자열 해석 방식.
 */
public enum JwtSecretEncoding {

	/**
	 * {@code jwt.secret} 문자열을 UTF-8 바이트로 사용 (기존 배포 호환).
	 */
	UTF8,

	/**
	 * Standard Base64로 디코딩한 바이트를 사용 (권장: 환경변수에 랜덤 32바이트 이상을 Base64로 저장).
	 */
	BASE64
}
