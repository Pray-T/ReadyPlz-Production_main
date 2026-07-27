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

**사전 준비**
- JDK 17+
- MySQL (`readyplz` 또는 dev 프로필 시 `readyplz_dev`)
- Redis (`localhost:6379`)
- JWT 시크릿: 환경 변수 `JWT_SECRET` 또는(dev) `application-local.properties`의 `jwt.secret`

**실행 예시**
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
