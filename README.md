# 긍정적으로 일하고 세상에 도움이 되자, 안녕하세요, 백엔드 개발자 최성욱입니다.

먼저 귀한 시간을 내어 저의 깃허브에 방문해 주신 것에 감사드립니다.<br/>
저는 팀의 일원으로서 팀원들과 함께 좋은 에너지와 시너지를 만들며 일하고 싶고, 동시에 혼자 고민하는 시간을 통해 전문가로서의 역량을 기르고자 하는 개발자입니다.

조직에 속한 팀원으로서의 성장과 개인의 성장을 함께 지향합니다.

<br/>

## 👨‍💻 Profile

- **이름** : 최성욱
- **이메일** : cenmot@naver.com
- **포트폴리오** : [https://readyplz.com](https://readyplz.com)
- **기술 스택** : `Java`, `Spring Boot`, `JPA`, `Spring Security`, `JWT`, `Redis`, `MySQL`, `AWS`
- **한줄 소개** : 게임 유저 매칭 및 채팅 웹사이트 **"Readyplz.com"** 개발 및 운영

<br/>

---

## 📑 목차 (Table of Contents)
*원하는 카테고리를 클릭하시면 해당 항목으로 이동합니다.*

1. [🌊 웹앱 흐름 및 아키텍처 개요](#1--웹앱-흐름-및-아키텍처-개요)
2. [✨ 주요 기능](#2--주요-기능)
3. [🛠 기술적 고민 및 아키텍처 결정](#3--기술적-고민-및-아키텍처-결정)
   - [3.1 JWT 사용 이유와 Redis와의 조합](#31-jwt-사용-이유와-redis와의-조합)
   - [3.2 Spring Security와 JWT 인증/인가 흐름](#32-spring-security와-jwt-인증인가-흐름)
   - [3.3 SMTP (포트 587)를 활용한 비밀번호 재설정](#33-smtp-포트-587를-활용한-비밀번호-재설정)
   - [3.4 WebSocket과 STOMP를 활용한 채팅 아키텍처](#34-websocket과-stomp를-활용한-채팅-아키텍처)
4. [🔥 트러블 슈팅](#4--트러블-슈팅)
   - [과도한 API 요청에 의한 '302 Too many Request' 해결 방안](#과도한-api-요청에-의한-302-too-many-request-해결-방안)

---

<br/>

## 1. 🌊 웹앱 흐름 및 아키텍처 개요

- **권한 분리**: ROLE에 따라 관리자(ADMIN) 전용 경로와 사용자(USER) 전용 경로가 존재합니다.
<img width="522" height="92" alt="스크린샷 2025-10-12 182028" src="https://github.com/user-attachments/assets/58497bac-732e-45ee-88a3-5d678900a51f" />

- **관리자 기능**: ADMIN은 JSON 파일로부터 DB를 업데이트하는 데 사용됩니다.
<img width="541" height="207" alt="스크린샷 2025-10-12 182809" src="https://github.com/user-attachments/assets/dbe9bfe3-1a1f-495c-a1b4-00848e58d3de" />

- **고객 센터 채널**: 사용자가 문의사항을 작성한 경우 기존의 채팅 기능을 활용하여 ADMIN 계정에게 메시지가 전송됩니다. ADMIN에게 보낸 메시지는 사용자 측 채팅 목록에 표시되지 않습니다.

- **데이터 흐름**: 클라이언트가 요청을 보내면 JWT를 통해 Spring Security 인증을 거치고 컨트롤러가 요청을 처리합니다.
  - `MySQL`: 게임 정보, 유저 정보, 메시지 정보 저장
  - `Redis`: JWT 및 일회성 토큰 관련 정보 저장(In-Memory DB)
<img width="1882" height="553" alt="스크린샷 2025-10-12 182424" src="https://github.com/user-attachments/assets/a5fa788c-0d19-46d0-9ee0-dcb7db051035" />

- **인프라 및 배포**:
  - AWS EC2(Ubuntu)를 통해 서버 배포 및 내부 Redis 구동
  - AWS RDS를 통해 MySQL 구축
  - AWS ELB를 사용하여 엔드포인트 보안, 가용성 증대 및 라우팅 효과 적용
<img width="1060" height="273" alt="스크린샷 2025-10-12 184338" src="https://github.com/user-attachments/assets/c7ec06d7-4c2f-427c-b5b1-62554303fe23" />

<br/>

## 2. ✨ 주요 기능

- JWT를 통한 로그인 시스템과 Access/Refresh 이중 토큰 전략 사용
- JWT 관련 정보를 저장하는 In-Memory DB인 Redis 사용
- JWT 특성에 맞춰 커스텀한 Spring Security 기반 인증/인가 처리
- 이메일 비밀번호 재설정 기능 구현 (포트 587 SMTP 사용, 비동기 처리)
- WebSocket을 통한 실시간 채팅 기능 및 STOMP를 활용한 수신자/발신자 라우팅

<br/>

## 3. 🛠 기술적 고민 및 아키텍처 결정

### 3.1 JWT 사용 이유와 Redis와의 조합

**JWT 채택 배경:**
- 훗날 네이티브 모바일 앱 환경으로의 확장을 고려하여 수평적 확장에 적합한 JWT 방식을 채택했습니다.
- Stateless 상태를 유지해 네트워크 통신 비용을 낮추고, 사용자 증가 시 발생하는 HttpSession 방식의 병목 현상을 방지합니다.
- 프론트엔드와 백엔드의 분리 및 독립적 개발을 용이하게 합니다.

> **💡 Q. 세션 기반 대신 JWT를 선택했지만, 결국 Redis에 저장한다면 세션 방식과 다를 바 없지 않은가?**<br/>
> **A.** JWT를 Redis(In-Memory DB)로 관리함으로써 인증에 소모되는 자원과 시간을 대폭 줄일 수 있습니다. 기존 세션 방식은 인증 거부자를 찾기 위해 세션 저장소를 모두 탐색해야 하지만, JWT+Redis 조합은 **Redis의 블랙리스트만 탐색**하므로 시간이 훨씬 단축됩니다. 완전한 Stateless는 아니지만 세션 방식보다 훨씬 뛰어난 확장성을 가집니다.

**Redis 동작 과정 및 이중 토큰 전략:**
- Redis의 인메모리 방식과 기본 탑재된 `TTL(Time To Live)` 기능은 빠른 속도의 JWT 유효성 검증과 만료 시간 관리에 최적의 시너지를 냅니다.
- Access/Refresh 이중 토큰 전략을 사용하여 Access 토큰 탈취 리스크를 줄였습니다.
- 만료되거나 탈취가 의심되는 토큰은 **블랙리스트**에 등록하여 즉시 무효화합니다.

<img width="1136" height="722" alt="image" src="https://github.com/user-attachments/assets/1b147b6e-260b-47df-a8eb-c3483c0f2198" />
<img width="612" height="151" alt="image" src="https://github.com/user-attachments/assets/836ae0f6-83fe-480c-b6b7-0181d97d830d" />

---

### 3.2 Spring Security와 JWT 인증/인가 흐름

- **Security 흐름**: `CORS` → `(CSRF 비활성)` → `JwtAuthenticationFilter` → `ExceptionTranslationFilter` → `FilterSecurityInterceptor` → `Controller`
- JWT 인증을 위해 기존 FormLogin 방식을 Disable하고, 필터 체인의 `UsernamePasswordAuthenticationFilter` 이전에 커스텀한 `JwtAuthenticationFilter(OncePerRequestFilter)`를 배치했습니다.
- 요청마다 토큰을 검증하고, 인증 성공 시 SecurityContext에 주입하여 인가 처리를 진행합니다. (토큰이 블랙리스트에 존재하면 진행 불가)

<img width="1867" height="201" alt="스크린샷 2025-10-12 173346" src="https://github.com/user-attachments/assets/7514d003-0326-40ad-8a66-77216049ecc9" />

---

### 3.3 SMTP (포트 587)를 활용한 비밀번호 재설정

**포트 587을 선택한 이유:**
- 포트 25: 암호화가 되지 않고 대부분의 ISP에서 차단됨.
- 포트 465: 시작부터 SSL/TLS로 암호화되지만 유연성이 떨어짐.
- **포트 587**: 평문으로 시작하여 필요 시 `STARTTLS` 명령어를 통해 암호화 채널로 통신을 업그레이드할 수 있는 유연성이 있어 채택했습니다.

**보안 및 최적화 로직:**
1. **타이밍 공격 방지**: 메일 존재 여부에 따른 타이밍 기반 추정을 막기 위해 BCrypt 해시 연산을 통해 의도적으로 연산 시간을 증가시켰습니다.
<img width="495" height="348" alt="image" src="https://github.com/user-attachments/assets/9a928483-4644-4b2f-93c5-df11afe8614e" />
2. **비동기 처리(@Async)**: 이메일 전송 시 메인 스레드가 멈추는 것을 방지하기 위해 별도의 스레드 풀(`mailTaskExecutor`)에 작업을 위임하여 응답 시간을 단축하고 UX를 개선했습니다.
<img width="473" height="285" alt="image" src="https://github.com/user-attachments/assets/3857b9b6-f532-46f5-8cc6-6b5da70a3de6" />
3. **동작 과정**:
<img width="763" height="524" alt="image" src="https://github.com/user-attachments/assets/e1fdc7c3-4323-41ff-8554-e667aff8a9c2" />

---

### 3.4 WebSocket과 STOMP를 활용한 채팅 아키텍처

**WebSocket 도입 배경:**
기존 HTTP Polling 방식은 요청마다 헤더를 포함하여 오버헤드가 크고 리소스 낭비가 심합니다. 양방향 통신이 가능하고 헤더 크기가 작은 WebSocket 프로토콜을 도입하여 실시간 채팅의 효율을 높였습니다.

**STOMP 채택 이유:**
WebSocket 자체는 발신자와 수신자를 라우팅하는 기능이 없습니다. STOMP(발행/구독 기반 프로토콜)를 사용하면 메시지 브로커가 송신자의 메시지를 받아 수신자에게 자동으로 라우팅해 주어, 세션 관리와 브로드캐스트 구현의 복잡성을 크게 낮출 수 있습니다.

> **💡 P.S) 다중 서버 환경에서의 STOMP와 ELB 웹소켓 통신**
> - 스프링 내장 브로커(`SimpUserRegistry`)는 힙 영역을 사용하므로 다중 서버 확장에 한계가 있습니다. 이를 해결하기 위해 추후 외부 메시지 브로커(RabbitMQ 등) 도입을 고려할 수 있습니다.
> - **AWS ELB와 웹소켓**: AWS ALB는 응용 계층(L7)에서 동작하여 WebSocket 업그레이드 요청을 인지합니다. 연결이 맺어지면 **'스티키 세션(Sticky Session)'**을 활성화하여, 생성된 쿠키(AWSALB)를 기반으로 클라이언트의 패킷을 특정 대상 인스턴스로만 전달하게끔 처리합니다.

<img width="1157" height="650" alt="image" src="https://github.com/user-attachments/assets/06ba6604-23dc-4ee7-a01e-b99bdd2796f3" />

<br/>

## 4. 🔥 트러블 슈팅

### 과도한 API 요청에 의한 "302 Too many Request" 해결 방안

**문제 상황:**
초기 아키텍처는 사용자가 선택할 게임 목록을 보여주기 위해 스팀(Steam) API를 직접 호출했습니다. 하지만 스팀 측의 API 호출 제한 정책으로 인해 `"302 Too many Request"` 에러가 빈번하게 발생했고, 게임 목록을 받아오지 못하는 치명적인 문제가 발생했습니다.

**해결 과정 및 결과:**
지연 시간 추가 등 여러 방법을 시도했으나 근본적인 해결책이 될 수 없다고 판단했습니다. 결국 **외부 API에 강하게 결합되어 있던 아키텍처를 폐기**하고, 게임 정보를 미리 크롤링/가공하여 내부 DB에 적재한 뒤, 클라이언트가 내부 DB를 조회하도록 아키텍처를 전면 수정했습니다. 

이를 통해 외부 API 장애에 대한 의존성을 제거하고, 조회 속도를 비약적으로 향상시켜 안정적인 서비스를 제공할 수 있게 되었습니다.

<br/>

---

이상으로 저의 깃허브 README를 읽어주셔서 대단히 감사합니다.
