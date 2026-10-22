package io.readyplz.readyplz.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class RestApiExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
		return validationErrorResponse(ex.getBindingResult());
	}

	@ExceptionHandler(BindException.class)
	public ResponseEntity<Map<String, Object>> handleBind(BindException ex) {
		return validationErrorResponse(ex.getBindingResult());
	}

	private static ResponseEntity<Map<String, Object>> validationErrorResponse(BindingResult bindingResult) {
		String message = bindingResult.getFieldErrors().stream()
				.findFirst()
				.map(FieldError::getDefaultMessage)
				.filter(msg -> msg != null && !msg.isBlank())
				.orElse("입력값을 확인해주세요.");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", message));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
		String message = ex.getMessage() != null && !ex.getMessage().isBlank()
				? ex.getMessage()
				: "잘못된 요청입니다.";
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", message));
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
		log.warn("요청 처리 불가: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "요청을 처리할 수 없습니다."));
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<Map<String, Object>> handleAuthentication(AuthenticationException ex) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "인증이 필요합니다."));
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "접근 권한이 없습니다."));
	}

	// Exception 핸들러보다 구체적인 타입만 둔다. 본문에는 고정 문구만 넣는다.
	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<Map<String, Object>> handleNoResourceFound(NoResourceFoundException ex) {
		log.debug("없는 정적 자원 요청 ({})", ex.getClass().getSimpleName());
		return fixedMessage(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다.");
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<Map<String, Object>> handleMessageNotReadable(HttpMessageNotReadableException ex) {
		log.debug("읽을 수 없는 요청 본문 ({})", ex.getClass().getSimpleName());
		return fixedMessage(HttpStatus.BAD_REQUEST, "요청 본문을 확인할 수 없습니다.");
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<Map<String, Object>> handleMissingParameter(MissingServletRequestParameterException ex) {
		log.debug("필수 파라미터 누락 ({})", ex.getClass().getSimpleName());
		return fixedMessage(HttpStatus.BAD_REQUEST, "필수 항목이 누락되었습니다.");
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<Map<String, Object>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
		log.debug("허용되지 않은 HTTP 메서드 ({})", ex.getClass().getSimpleName());
		return fixedMessage(HttpStatus.METHOD_NOT_ALLOWED, "허용되지 않은 메서드입니다.");
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
		log.error("처리되지 않은 서버 오류", ex);
		Map<String, Object> body = new HashMap<>();
		body.put("message", "서버 오류가 발생했습니다.");
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
	}

	private static ResponseEntity<Map<String, Object>> fixedMessage(HttpStatus status, String message) {
		return ResponseEntity.status(status).body(Map.of("message", message));
	}
}
