# 긍정적으로 일하고 세상에 도움이 되는 사람이 되자. <br> 안녕하세요 백엔드 개발자 최성욱입니다.

먼저 귀한 시간을 내어 저의 깃허브에 방문해 주신 것에 감사드립니다.<br/>
저는 팀의 일원으로서 팀원들과 함께 좋은 에너지와 시너지를 만들며 일하고 싶고, 동시에 혼자 고민하는 시간을 통해 전문가로서의 역량을 기르고자 하고 적은 비용으로 어떻게 하면 많은 효과를 누릴 수 있을까? 하고 생각하는 개발자입니다.


<br/>

## Profile

- 이름 : 최성욱
- 이메일 : cenmot@naver.com
- 기술 스택 : `Java 17`, `Spring Boot 3.2`, `Spring Security`, `JWT`, `Redis`, `MySQL`, `JPA`, `Thymeleaf`, `WebSocket(STOMP/SockJS)`, `Spring Mail`, `AWS(EC2, ELB, RDS)`
- 또 다른 포트폴리오:<br>
[이체시스템의 구현을 통한 MySQL의 정합성과 인덱스](https://github.com/Pray-T/BankTransferSys_Backend_Restful) <br>
[Cursor Agent를 이용하여 GitHub Copilot을 저렴하게 사용해보기](https://github.com/Pray-T/GitHub-Copilot-With-Cursor)

<br/>

---

## ReadyPlz 프로젝트

Steam 게임을 기준으로 함께 플레이할 유저를 찾고, 회원 간 메시지를 주고할 수 있는 웹 애플리케이션입니다.  
패키지 루트는 `io.readyplz.readyplz` 이며, Thymeleaf SSR과 REST API를 함께 사용합니다.

**데모**
- Live: [https://readyplz.com](https://readyplz.com) (`https://www.readyplz.com`)
- 배포 환경이 일시적으로 내려가 있으면 아래 **로컬 실행**으로 동일 흐름을 확인할 수 있습니다.

**스크린샷 · 아키텍처**

<p align="center">
  <img src="./src/main/resources/static/images/ReadyPlzBackGroundGraphic.png" alt="ReadyPlz 앱 비주얼" width="720" />
</p>

<p align="center">
  <img src="https://github.com/user-attachments/assets/74f1e165-7f42-4e01-9021-283476e37eee" alt="ReadyPlz 시스템 아키텍처" width="400" />
</p>

<p align="center"><em>시스템 아키텍처 다이어그램 (상세: <a href="./docs/01-architecture.md">docs/01-architecture.md</a>)</em></p>

**주요 기능·동작**
- Access / Refresh 이중 JWT + Redis(활성 토큰·블랙리스트 저장). 기본 만료: Access **1시간**, Refresh **7일** (`application.properties`)
- JWT 발급·갱신·로그아웃: `AuthController` (`/api/auth/**`). 로그인 폼 화면만 `LoginController` (`GET /members/loginForm`)
- Spring Security: `JwtAuthenticationFilter` + **CSRF 활성**(Cookie CSRF, `/api/**`만 예외)
- 게임 컬렉션·동일 게임 유저 조회 (Steam 데이터는 JSON → DB 사전 적재, 실시간 Steam API 호출 없음)
- 회원 1:1 메시징은 **HTTP** (`MessageController`, `POST /messages/send` 등). `MessageService.sendMessage`가 DB 저장 후 트랜잭션 **afterCommit** 시점에만 `SimpMessagingTemplate.convertAndSendToUser(수신자 username, /queue/notifications, payload)`로 실시간 알림 push. 페이로드는 `NotificationDTO{type, message, data}`(예: `type="MESSAGE"`, `data`=발신자 닉네임)이며, 브로커 발행 실패는 try/catch로 격리되어 저장·HTTP 응답에 영향 없음
- WebSocket(STOMP/SockJS): 엔드포인트 `/ws-nearby-gamers`, SimpleBroker `/queue`·`/topic`, 유저 접두사 `/user`, 쿠키 기반 JWT 연결 인증(`WebSocketConfig`, `WebSocketAuthChannelInterceptor`, principal 이름=username). Redis는 WebSocket 세션 저장용이 아니라 JWT용
- 비동기 이메일 비밀번호 재설정 (`EmailService` + `@Async`)
- ADMIN JSON 게임 적재 (`POST /admin/db/load-json-games`)
- 프로필: `ProfileController` (`/members/profile`, `confirm-nickname`, `change-password` 등)

<br/>

## 로컬 실행

### 사전 준비
- JDK 17+
- MySQL (`readyplz` 또는 dev 프로필 시 `readyplz_dev`)
- Redis (`localhost:6379`)
- JWT 시크릿: 환경 변수 `JWT_SECRET` 또는(dev) `application-local.properties`의 `jwt.secret`  
  (`application-local.properties`는 Git 제외 — `src/main/resources/application-local.properties`에 예: `jwt.secret=로컬용-충분히-긴-시크릿`)

### 1) DB 스키마 생성
운영/기본 설정은 `spring.jpa.hibernate.ddl-auto=validate` 이라 **빈 DB만 있으면 기동이 실패**합니다. 로컬 최초 1회는 스키마를 만든 뒤 validate로 되돌리세요.

```sql
CREATE DATABASE readyplz_dev
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

```bash
# 최초 1회: 엔티티 기준으로 테이블 생성
./gradlew bootRun --args='--spring.profiles.active=dev --spring.jpa.hibernate.ddl-auto=update'
```

Windows:
```bat
gradlew.bat bootRun --args="--spring.profiles.active=dev --spring.jpa.hibernate.ddl-auto=update"
```

테이블이 생성된 뒤에는 기본값(`validate`)으로 재기동합니다.

### 2) Steam 게임 JSON 준비
- 기본 위치: `src/main/resources/steam_games_data.json` (classpath, 저장소에 포함됨)
- 설정: `app.games.import.data-location=classpath:steam_games_data.json`
- 대용량을 파일로 둘 경우(dev): `app.games.import.data-location=file:${user.home}/steam_games_data.json`
- **DB 적재는 ADMIN 전용 API**로 수행합니다 (아래 3번 후).

```http
POST /admin/db/load-json-games
```

`/admin/**`은 CSRF 예외가 아니므로, 브라우저에서 로그인 후 Cookie(`accessToken` 등)와 `XSRF-TOKEN` 쿠키 값을 `X-XSRF-TOKEN` 헤더로 함께 보내면 됩니다.

```bash
# 예시 (토큰·쿠키 값은 로그인 세션에서 복사)
curl -X POST "http://localhost:8080/admin/db/load-json-games" \
  -H "Cookie: accessToken=...; refreshToken=...; XSRF-TOKEN=..." \
  -H "X-XSRF-TOKEN: <XSRF-TOKEN 쿠키와 동일 값>"
```

성공 시 게임 검색·컬렉션 기능을 사용할 수 있습니다.

### 3) ADMIN 계정 생성
회원가입 API/화면은 `ROLE_USER`만 부여합니다. 게임 JSON 적재·문의 수신을 위해 ADMIN이 필요합니다.

1. 일반 회원으로 가입 (`GET /members/registerForm` 또는 `POST /api/auth/register`)
2. MySQL에서 `ROLE_ADMIN`을 만들고 해당 회원에 연결합니다.

```sql
INSERT INTO role (name)
SELECT 'ROLE_ADMIN' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM role WHERE name = 'ROLE_ADMIN');

-- your_admin_username 을 실제 username 으로 바꿔 실행
INSERT INTO member_roles (member_id, role_id)
SELECT m.member_id, r.id
FROM members m
JOIN role r ON r.name = 'ROLE_ADMIN'
WHERE m.username = 'your_admin_username'
  AND NOT EXISTS (
    SELECT 1 FROM member_roles mr
    WHERE mr.member_id = m.member_id AND mr.role_id = r.id
  );
```

3. 해당 계정으로 다시 로그인 → `POST /admin/db/load-json-games`로 JSON 적재

### 실행 예시
```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```
Windows:
```bat
gradlew.bat bootRun --args="--spring.profiles.active=dev"
```

- 기본 포트: `8080`
- 메일(로컬): `application.properties` 기준 `localhost:1025` (실제 발송 시 SMTP 설정 필요)
- 상세 설정: `src/main/resources/application.properties`, `application-dev.properties`

**권장 확인 순서 (리뷰어)**
1. Redis·MySQL 기동 → DB 생성 → `ddl-auto=update`로 1회 기동  
2. 회원가입 → SQL로 ADMIN 승격 → 재로그인  
3. `POST /admin/db/load-json-games`로 게임 데이터 적재  
4. `/games/collection`, `/messages` 등 핵심 화면 확인  

<br/>

## 테스트

`src/test` 아래 단위/슬라이스 테스트가 있습니다. H2 in-memory + Redis/Mail 자동설정 제외(`application-test.properties`)로 로컬에서 빠르게 돌릴 수 있습니다.

| 영역 | 클래스 | 검증 포인트 |
|------|--------|-------------|
| Security | `SecurityConfigTest` | 홈·헬스 공개 접근 |
| JWT 필터 | `JwtAuthenticationFilterTest` | Redis 활성 토큰 일치/불일치 |
| 메시징 | `MessageServiceTest` | afterCommit 알림 push, 저장 실패·롤백 시 미발행 |
| 회원 | `MemberServiceTest` | 닉네임 유지/중복 거부 |
| 게임 import | `GameImportBatchServiceTest` | 배치 저장·중복 스킵 |
| 예외 | `RestApiExceptionHandlerTest` | API 오류 응답 메시지 노출 제한 |
| 지원 | `TestRedisConfig` | 테스트용 Redis 빈 |

```bash
./gradlew test
```
Windows:
```bat
gradlew.bat test
```

<br/>

---

## 상세 문서
*아래 링크를 클릭하시면 해당 상세 페이지로 이동합니다.*

### [1. 웹앱 흐름 및 아키텍처 개요](./docs/01-architecture.md)
전체적인 앱의 아키텍처 개요와 USER / ADMIN 계정 흐름·기능을 설명합니다.

### [2. 주요 기능](./docs/02-features.md)
이중 토큰(Access/Refresh), HTTP 메시징·WebSocket 연결, 비밀번호 재설정 등 핵심 기능을 소개합니다.
- [2.1 JWT Access/Refresh 이중 토큰](./docs/2.1-jwt_double_token.md)
- [2.2 Redis와 JWT](./docs/2.2-redis_jwt.md)
- [2.3 Spring Security 커스텀 인증](./docs/2.3-jwt_spring_security.md)
- [2.4 이메일 비밀번호 재설정](./docs/2.4-email_reset.md)
- [2.5 WebSocket / STOMP 연결·인증](./docs/2.5-websocket_chat.md)

### [3. 기술적 고민 및 아키텍처 결정](./docs/03-tech-decisions.md)
- JWT와 Redis를 조합한 인증 방식
- Spring Security 커스텀 필터·CSRF(`/api/**` 예외) 흐름
- 비동기 이메일(SMTP) 비밀번호 재설정
- WebSocket(STOMP) 연결·인증과 HTTP 기반 메시징 역할 분리

### [4. 문제 해결](./docs/04-troubleshooting.md)
과도한 API 요청에 의한 `429 Too Many Requests` 문제와, Steam API 실시간 호출을 폐기하고 JSON/DB 사전 적재로 전환한 경험을 담았습니다.

### [5. AWS 관련](./docs/aws/aws_main.md)
EC2, ELB, RDS 등 배포 관련 내용입니다.
- [EC2](./docs/aws/ec2.md)
- [Ubuntu 배포](./docs/aws/ubuntu.md)
- [ELB](./docs/aws/elb.md)
- [HTTPS](./docs/aws/https.md)
- [RDS](./docs/aws/rds.md)

<br/>

*이상입니다, 저의 깃허브 방문을 감사드립니다.*
