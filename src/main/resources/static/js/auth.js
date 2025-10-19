// JWT 인증 관련 JavaScript

class AuthManager {
    constructor() {
        this.accessToken = null; // 메모리에만 유지
        this.username = localStorage.getItem('username');
        // WebSocket 상태
        this.stompClient = null;
        this.webSocketConnected = false;
        this.wsReconnectDelayMs = 5000;
        this.wsReconnectTimer = null;
    }

    // 로그인
    async login(username, password) {
        try {
            const response = await apiClient.postJson('/api/auth/login', { username, password }, { auth: false, retry: false, autoRedirectOnAuthError: false });

            const data = await response.json();
            
            if (response.ok) {
                this.setTokens(data.accessToken, data.username);
                return { success: true, message: data.message };
            } else {
                return { success: false, message: data.message };
            }
        } catch (error) {
            console.error('로그인 오류:', error);
            return { success: false, message: '로그인 중 오류가 발생했습니다.' };
        }
    }

    // 회원가입
    async register(username, email, password, passwordConfirm, nickname, country) {
        try {
            const response = await apiClient.postJson('/api/auth/register', {
                username,
                email,
                password,
                passwordConfirm,
                nickname,
                country
            }, { auth: false, retry: false, autoRedirectOnAuthError: false });

            const data = await response.json();
            
            if (response.ok) {
                this.setTokens(data.accessToken, data.username);
                return { success: true, message: data.message };
            } else {
                return { success: false, message: data.message };
            }
        } catch (error) {
            console.error('회원가입 오류:', error);
            return { success: false, message: '회원가입 중 오류가 발생했습니다.' };
        }
    }

    // 토큰 갱신
    async refreshToken() {
        try {
            const response = await apiClient.postJson('/api/auth/refresh', {}, { auth: false, retry: false, autoRedirectOnAuthError: false });

            const data = await response.json();
            
            if (response.ok) {
                this.setTokens(data.accessToken, data.username);
                return { success: true, message: data.message };
            } else {
                this.clearTokens();
                return { success: false, message: data.message };
            }
        } catch (error) {
            console.error('토큰 갱신 오류:', error);
            this.clearTokens();
            return { success: false, message: '토큰 갱신 중 오류가 발생했습니다.' };
        }
    }

    // 로그아웃
    async logout() {
        try {
            await apiClient.postJson('/api/auth/logout', {}, { auth: true, retry: false });
        } catch (error) {
            console.error('로그아웃 오류:', error);
        }
        // WebSocket 연결 해제
        this.disconnectWebSocket();

        this.clearTokens();
        try { sessionStorage.setItem('skipSilentRefresh', '1'); } catch (e) {}
        window.location.href = '/';
    }

    // 토큰 설정
    setTokens(accessToken, username) {
        this.accessToken = accessToken;
        this.username = username;

        if (username) {
            localStorage.setItem('username', username);
        }
    }

    // 토큰 삭제
    clearTokens() {
        this.accessToken = null;
        this.username = null;
        
        localStorage.removeItem('username');
    }

    // 인증된 요청 헤더 가져오기
    getAuthHeaders() {
        return {
            'Authorization': `Bearer ${this.accessToken}`,
            'Content-Type': 'application/json'
        };
    }

    // 인증된 요청 보내기 (토큰 검증 포함)
    async authenticatedRequest(url, options = {}) {
        try {
            const response = await apiClient.request(url, { auth: true, ...options });
            return response;
        } catch (error) {
            console.error('인증된 요청 오류:', error);
            return null;
        }
    }

    // 인증 상태 확인
    isAuthenticated() {
        return !!this.accessToken;
    }

    // 사용자명 가져오기
    getUsername() {
        return this.username;
    }

    // =========================
    // WebSocket 관리 (STOMP)
    // =========================
    connectWebSocket() {
        if (!this.isAuthenticated()) {
            return;
        }
        if (!this.username) {
            console.log('WebSocket: username not set.');
            return;
        }
        if (this.webSocketConnected) {
            return;
        }

        const socket = new SockJS('/ws-nearby-gamers');
        this.stompClient = Stomp.over(socket);

        const headers = {};
        headers['Authorization'] = `Bearer ${this.accessToken}`;

        this.stompClient.connect(headers, (frame) => {
            console.log('Connected to WebSocket: ' + frame);
            this.webSocketConnected = true;
            if (this.wsReconnectTimer) {
                clearTimeout(this.wsReconnectTimer);
                this.wsReconnectTimer = null;
            }

            // 사용자 큐 구독
            this.stompClient.subscribe(`/user/${this.username}/queue/notifications`, (notification) => {
                try {
                    const payload = JSON.parse(notification.body);
                    this.handleIncomingNotification(payload);
                } catch (e) {
                    console.error('알림 파싱 오류:', e);
                }
            });
        }, (error) => {
            console.error('STOMP error: ' + error);
            this.webSocketConnected = false;
            this.scheduleWebSocketReconnect();
        });
    }

    scheduleWebSocketReconnect() {
        if (this.wsReconnectTimer || !this.isAuthenticated()) {
            return;
        }
        this.wsReconnectTimer = setTimeout(() => {
            this.wsReconnectTimer = null;
            if (this.isAuthenticated()) {
                this.connectWebSocket();
            }
        }, this.wsReconnectDelayMs);
    }

    disconnectWebSocket() {
        if (this.wsReconnectTimer) {
            clearTimeout(this.wsReconnectTimer);
            this.wsReconnectTimer = null;
        }
        if (this.stompClient) {
            try {
                this.stompClient.disconnect(() => {
                    this.webSocketConnected = false;
                    this.stompClient = null;
                });
            } catch (e) {
                this.webSocketConnected = false;
                this.stompClient = null;
            }
        }
        this.webSocketConnected = false;
    }

    handleIncomingNotification(notification) {
        const notificationList = document.getElementById('notification-list');
        if (notificationList) {
            const listItem = document.createElement('li');
            listItem.textContent = `[${notification.type}] ${notification.message}`;
            if (notification.data) {
                listItem.textContent += ` (플레이어: ${notification.data})`;
            }
            notificationList.appendChild(listItem);
        } else {
            alert(`[${notification.type}] ${notification.message}${notification.data ? ' (플레이어: ' + notification.data + ')' : ''}`);
        }
        console.log('Received notification: ', notification);
    }

    startWebSocket() {
        this.connectWebSocket();
    }
}

// 전역 인증 매니저 인스턴스
window.authManager = new AuthManager();
const authManager = window.authManager;

// 페이지 로드 시 인증 상태 확인
document.addEventListener('DOMContentLoaded', async function() {
    // Silent refresh: 메모리 토큰이 없으면 쿠키 기반 리프레시 시도
    if (!authManager.isAuthenticated()) {
        const skip = (() => { try { return sessionStorage.getItem('skipSilentRefresh') === '1'; } catch(e){ return false; } })();
        if (skip) {
            try { sessionStorage.removeItem('skipSilentRefresh'); } catch (e) {}
        } else {
            await authManager.refreshToken();
        }
    }

    // 토큰 유효성 검증 (선택)
    if (authManager.isAuthenticated()) {
        try {
            const response = await apiClient.get('/api/auth/validate', { auth: true, autoRedirectOnAuthError: false });
            if (!response.ok) {
                authManager.clearTokens();
            }
        } catch (e) {
            authManager.clearTokens();
        }
    }
    
    updateAuthUI();

    // 인증된 경우 WebSocket 연결 시도 (AuthManager 내부 username 사용)
    if (authManager.isAuthenticated()) {
        authManager.startWebSocket();
    }
});

// 인증 UI 업데이트
function updateAuthUI() {
    const authContainer = document.getElementById('auth-container');
    const userContainer = document.getElementById('user-container');
    
    if (authManager.isAuthenticated()) {
        if (authContainer) authContainer.style.display = 'none';
        if (userContainer) {
            userContainer.style.display = 'block';
            const usernameElement = userContainer.querySelector('.username');
            if (usernameElement) {
                usernameElement.textContent = authManager.getUsername();
            }
        }
    } else {
        if (authContainer) authContainer.style.display = 'block';
        if (userContainer) userContainer.style.display = 'none';
    }
}

// 로그인 폼 처리
async function handleLogin(event) {
    event.preventDefault();
    
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    
    const result = await authManager.login(username, password);
    
    if (result.success) {
        updateAuthUI();
        // 토큰을 URL에 노출하지 않고 홈으로 이동
        window.location.href = '/';
    } else {
        alert(result.message);
    }
}

// 회원가입 폼 처리
async function handleRegister(event) {
    event.preventDefault();
    
    const username = document.getElementById('username').value;
    const email = document.getElementById('email').value;
    const password = document.getElementById('password').value;
    const passwordConfirm = document.getElementById('passwordConfirm').value;
    const nickname = document.getElementById('nickname').value;
    const country = document.getElementById('country').value; 
    
    const result = await authManager.register(username, email, password, passwordConfirm, nickname, country);
    
    if (result.success) {
        updateAuthUI();
        window.location.href = '/';
    } else {
        alert(result.message);
    }
}

// 로그아웃 처리
async function handleLogout() {
    await authManager.logout();
}

// 페이지 이동 시 토큰을 URL에 포함하지 않음
async function navigateWithAuth(url) {
    window.location.href = url;
}

// 모든 링크 클릭 시 JWT 토큰 포함
document.addEventListener('click', function(e) {
    if (e.target.tagName === 'A' && authManager.isAuthenticated()) {
        const href = e.target.getAttribute('href');
        if (href && !href.startsWith('http') && !href.startsWith('#') && !href.startsWith('javascript:')) {
            // 정적 리소스는 JWT 토큰 없이 직접 접근
            if (href.startsWith('/css/') || 
                href.startsWith('/js/') || 
                href.startsWith('/images/') || 
                href.startsWith('/static/') ||
                href === '/' ||
                href === '/members/loginForm' ||
                href === '/members/register' ||
                href.startsWith('/api/auth/')) {
                return; // 기본 동작 허용
            }
            
            e.preventDefault();
            navigateWithAuth(href);
        }
    }
}); 