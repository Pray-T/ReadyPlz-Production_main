package io.readyplz.readyplz.dto.notification;

/**
 * STOMP 실시간 알림 페이로드.
 * 프론트엔드(auth.js) 계약: type, message, data
 */
public record NotificationDTO(String type, String message, String data) {
}
