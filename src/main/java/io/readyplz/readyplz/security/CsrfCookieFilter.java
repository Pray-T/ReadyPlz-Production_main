package io.readyplz.readyplz.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Spring Security 6 deferred CSRF에서 {@code XSRF-TOKEN} 쿠키가 GET에도 내려가도록 토큰을 강제 로드한다.
 */
public final class CsrfCookieFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		Object csrfAttr = request.getAttribute(CsrfToken.class.getName());
		if (csrfAttr instanceof CsrfToken csrfToken) {
			csrfToken.getToken();
		}
		filterChain.doFilter(request, response);
	}
}
