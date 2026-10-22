package io.readyplz.readyplz.exception;

/**
 * 회원이 이미 가진 게임을 다시 넣을 때 던진다.
 * 응답 본문은 이 예외의 원문이 아니라 컨트롤러의 기존 안내 문구를 쓴다.
 */
public class AlreadyOwnedGameException extends RuntimeException {

    public AlreadyOwnedGameException(String message) {
        super(message);
    }
}
