// 중앙 집중식 API 클라이언트
// - Authorization 헤더 자동 첨부
// - 401/403 시 자동 토큰 갱신 후 1회 재시도
// - 갱신 실패 시 토큰 정리 및 로그인 페이지로 리다이렉트(옵션으로 비활성화 가능)

class ApiClient {
	constructor(authManagerInstance) {
		this.authManager = authManagerInstance;
		this.isRefreshing = false;
		this.refreshPromise = null;
	}

	async request(url, options = {}) {
		const {
			auth = true, // 인증이 필요한 요청 여부(쿠키 전송)
			retry = true, // 401/403 시 자동 재시도 여부(갱신 후 1회)
			autoRedirectOnAuthError = true, // 갱신 실패 시 로그인 페이지 이동 여부
			headers = {},
			...rest
		} = options;

		const finalHeaders = new Headers(headers || {});

		// Authorization 헤더는 기본적으로 사용하지 않음(쿠키 기반). 필요 시 호출부에서 명시적으로 설정.

		// 기본 Content-Type 처리: body가 FormData가 아니고 명시 안 되었을 때 JSON으로 강제하지 않음
		// 호출부에서 postJson/postForm 사용 권장

		const doFetch = async () => {
			return fetch(url, {
				credentials: 'include',
				...rest,
				headers: finalHeaders
			});
		};

		let response = await doFetch();

		if ((response.status === 401 || response.status === 403) && auth) {
			if (!retry) {
				return response;
			}

			// 동시 갱신 제어: refresh 요청은 단일 진행, 나머지는 대기
			try {
				await this.refreshTokenOnce();
			} catch (e) {
				// 갱신 자체가 예외 발생
				this.handleAuthFailure(autoRedirectOnAuthError);
				return response;
			}

			// 갱신 결과 확인
			if (!this.authManager || !this.authManager.accessToken) {
				this.handleAuthFailure(autoRedirectOnAuthError);
				return response;
			}

			// 새로운 토큰으로 Authorization 갱신 후 1회 재시도
			finalHeaders.set('Authorization', `Bearer ${this.authManager.accessToken}`);
			response = await fetch(url, {
				credentials: 'include',
				...rest,
				headers: finalHeaders
			});

			if (response.status === 401 || response.status === 403) {
				this.handleAuthFailure(autoRedirectOnAuthError);
			}
		}

		return response;
	}

	async refreshTokenOnce() {
		if (!this.authManager) {
			throw new Error('No refresh token available');
		}

		if (this.isRefreshing) {
			// 진행 중인 갱신 대기
			return this.refreshPromise;
		}

		this.isRefreshing = true;
		this.refreshPromise = (async () => {
			try {
				const result = await this.authManager.refreshToken();
				if (!result || !result.success) {
					throw new Error('Refresh failed');
				}
				return result;
			} finally {
				this.isRefreshing = false;
				this.refreshPromise = null;
			}
		})();

		return this.refreshPromise;
	}

	handleAuthFailure(autoRedirect) {
		try {
			if (this.authManager) this.authManager.clearTokens();
		} finally {
			if (autoRedirect) {
				try {
					const currentPath = window.location && window.location.pathname;
					if (currentPath === '/members/loginForm') {
						return; // 이미 로그인 폼이면 추가 리다이렉트 방지
					}
				} catch (e) { /* no-op */ }
				window.location.href = '/members/loginForm';
			}
		}
	}

	// 편의 메서드들
	async get(url, options = {}) {
		return this.request(url, { method: 'GET', ...options });
	}

	async postJson(url, data, options = {}) {
		const headers = new Headers(options.headers || {});
		if (!headers.has('Content-Type')) {
			headers.set('Content-Type', 'application/json');
		}
		return this.request(url, {
			method: 'POST',
			headers,
			body: data !== undefined ? JSON.stringify(data) : undefined,
			...options
		});
	}

	async postForm(url, dataObj, options = {}) {
		const headers = new Headers(options.headers || {});
		if (!headers.has('Content-Type')) {
			headers.set('Content-Type', 'application/x-www-form-urlencoded');
		}
		const body = new URLSearchParams(dataObj || {}).toString();
		return this.request(url, {
			method: 'POST',
			headers,
			body,
			...options
		});
	}
}

// 전역 인스턴스 노출 (authManager는 auth.js에서 생성됨)
// auth.js보다 늦게 로드되도록 스크립트 순서를 보장해야 함
window.apiClient = new ApiClient(window.authManager);


