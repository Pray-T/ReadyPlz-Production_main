package io.readyplz.readyplz.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.DeferredCsrfToken;

/**
 * JWT(STATELESS) 환경에서는 요청마다 인증이 다시 적용되어
 * {@code CsrfAuthenticationStrategy}가 {@code saveToken(null)}로 XSRF 쿠키를 지운다.
 * 삭제는 무시하고, 실제 토큰 저장만 위임한다.
 */
public final class NonClearingCookieCsrfTokenRepository implements CsrfTokenRepository {

	private final CookieCsrfTokenRepository delegate = CookieCsrfTokenRepository.withHttpOnlyFalse();

	@Override
	public CsrfToken generateToken(HttpServletRequest request) {
		return this.delegate.generateToken(request);
	}

	@Override
	public void saveToken(CsrfToken token, HttpServletRequest request, HttpServletResponse response) {
		if (token == null) {
			return;
		}
		this.delegate.saveToken(token, request, response);
	}

	@Override
	public CsrfToken loadToken(HttpServletRequest request) {
		return this.delegate.loadToken(request);
	}

	@Override
	public DeferredCsrfToken loadDeferredToken(HttpServletRequest request, HttpServletResponse response) {
		return this.delegate.loadDeferredToken(request, response);
	}
}
