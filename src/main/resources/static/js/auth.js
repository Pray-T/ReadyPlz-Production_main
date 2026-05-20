// JWT 인증 관련 JavaScript (HttpOnly 쿠키 기반)

class AuthManager {
    constructor() {
        this.username = localStorage.getItem('username');
        this.webSocketConnected = false;
        this.stompClient = null;
        this.wsReconnectDelayMs = 5000;
        this.wsReconnectTimer = null;
    }

    async login(username, password) {
        try {
            const response = await apiClient.postJson('/api/auth/login', { username, password }, { auth: false, retry: false, autoRedirectOnAuthError: false });
            const data = await response.json();

            if (response.ok) {
                this.setSession(data.username);
                return { success: true, message: data.message };
            }
            return { success: false, message: data.message };
        } catch (error) {
            console.error('로그인 오류:', error);
            return { success: false, message: '로그인 중 오류가 발생했습니다.' };
        }
    }

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
                this.setSession(data.username);
                return { success: true, message: data.message };
            }
            return { success: false, message: data.message };
        } catch (error) {
            console.error('회원가입 오류:', error);
            return { success: false, message: '회원가입 중 오류가 발생했습니다.' };
        }
    }

    async refreshToken() {
        try {
            const response = await apiClient.postJson('/api/auth/refresh', {}, { auth: false, retry: false, autoRedirectOnAuthError: false });
            const data = await response.json();

            if (response.ok) {
                this.setSession(data.username);
                return { success: true, message: data.message };
            }
            this.clearSession();
            return { success: false, message: data.message };
        } catch (error) {
            console.error('토큰 갱신 오류:', error);
            this.clearSession();
            return { success: false, message: '토큰 갱신 중 오류가 발생했습니다.' };
        }
    }

    async logout() {
        try {
            await apiClient.postJson('/api/auth/logout', {}, { auth: true, retry: false });
        } catch (error) {
            console.error('로그아웃 오류:', error);
        }
        this.disconnectWebSocket();
        this.clearSession();
        try { sessionStorage.setItem('skipSilentRefresh', '1'); } catch (e) {}
        window.location.href = '/';
    }

    setSession(username) {
        this.username = username;
        if (username) {
            localStorage.setItem('username', username);
        }
    }

    clearSession() {
        this.username = null;
        localStorage.removeItem('username');
    }

    getAuthHeaders() {
        return { 'Content-Type': 'application/json' };
    }

    async authenticatedRequest(url, options = {}) {
        try {
            return await apiClient.request(url, { auth: true, ...options });
        } catch (error) {
            console.error('인증된 요청 오류:', error);
            return null;
        }
    }

    isAuthenticated() {
        return !!this.username;
    }

    getUsername() {
        return this.username;
    }

    connectWebSocket() {
        if (!this.isAuthenticated()) {
            return;
        }
        if (!this.username) {
            return;
        }
        if (this.webSocketConnected) {
            return;
        }

        const socket = new SockJS('/ws-nearby-gamers');
        this.stompClient = Stomp.over(socket);

        this.stompClient.connect({}, (frame) => {
            console.log('Connected to WebSocket: ' + frame);
            this.webSocketConnected = true;
            if (this.wsReconnectTimer) {
                clearTimeout(this.wsReconnectTimer);
                this.wsReconnectTimer = null;
            }

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
    }

    startWebSocket() {
        this.connectWebSocket();
    }
}

window.authManager = new AuthManager();
const authManager = window.authManager;

document.addEventListener('DOMContentLoaded', async function() {
    if (!authManager.isAuthenticated()) {
        const skip = (() => { try { return sessionStorage.getItem('skipSilentRefresh') === '1'; } catch (e) { return false; } })();
        if (skip) {
            try { sessionStorage.removeItem('skipSilentRefresh'); } catch (e) {}
        } else {
            await authManager.refreshToken();
        }
    }

    if (authManager.isAuthenticated()) {
        try {
            const response = await apiClient.get('/api/auth/validate', { auth: true, autoRedirectOnAuthError: false });
            if (!response.ok) {
                authManager.clearSession();
            }
        } catch (e) {
            authManager.clearSession();
        }
    }

    updateAuthUI();

    if (authManager.isAuthenticated()) {
        authManager.startWebSocket();
    }
});

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

async function handleLogin(event) {
    event.preventDefault();
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    const result = await authManager.login(username, password);
    if (result.success) {
        updateAuthUI();
        window.location.href = '/';
    } else {
        alert(result.message);
    }
}

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

async function handleLogout() {
    await authManager.logout();
}

async function navigateWithAuth(url) {
    window.location.href = url;
}

document.addEventListener('click', function(e) {
    if (e.target.tagName === 'A' && authManager.isAuthenticated()) {
        const href = e.target.getAttribute('href');
        if (href && !href.startsWith('http') && !href.startsWith('#') && !href.startsWith('javascript:')) {
            if (href.startsWith('/css/') ||
                href.startsWith('/js/') ||
                href.startsWith('/images/') ||
                href.startsWith('/static/') ||
                href === '/' ||
                href === '/members/loginForm' ||
                href === '/members/register' ||
                href.startsWith('/api/auth/')) {
                return;
            }
            e.preventDefault();
            navigateWithAuth(href);
        }
    }
});
