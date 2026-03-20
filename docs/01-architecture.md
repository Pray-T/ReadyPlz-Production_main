[🔙 메인으로 돌아가기](../README.md)

1. 시스템 아키텍처 다이어그램 (System Architecture)<br>
계층(Layer) 간의 의존성은 아래와 같이 분리설계 하였습니다.<br>
<br>

<img width="400" height="697" alt="스크린샷 2026-03-20 140653" src="https://github.com/user-attachments/assets/74f1e165-7f42-4e01-9021-283476e37eee" />


<br>
<br>
2. 계층별 역할 (Layer Details)
<br>
<br>
  (1). Client Layer<br>
서버 사이드 렌더링(SSR)을 통해 초기 로딩 속도 향상을 의도했습니다.<br>
<br>
  (2). Security Layer (보안 및 인증) <br>
HTTP 요청: SecurityConfig에 등록된 JwtAuthenticationFilter를 거쳐 토큰의 유효성을 검사하고 SecurityContext에 인증 객체를 저장합니다. <br>
WebSocket 요청: STOMP 프로토콜 연결 시 WebSocketAuthChannelInterceptor가 개입하여, 소켓 세션이 맺어지기 전 JWT 토큰을 검증함으로써 허가되지 않은 사용자의 채팅 연결을 차단합니다. <br>
<br>
  (3). Controller Layer (표현 계층)<br>
클라이언트의 요청을 받아 해당 Service로 위임하고, 결과를 View나 JSON 형태로 반환합니다.<br>
AuthController, LoginController: JWT 발급 및 로그인 흐름 제어<br>
GameController, MemberController: 도메인별 비즈니스 요청 처리<br>
MessageController: 실시간 채팅 메시지 발행/구독(Pub/Sub) 라우팅<br>
<br>
  (4). Service Layer (비즈니스 로직 계층)<br>
트랜잭션(@Transactional) 경계를 설정하고 예외 처리를 담당합니다.<br>
CustomUserDetailsService: Spring Security 인증 시 DB에서 유저 정보를 로드합니다.<br>
TokenService: Redis와 연동하여 Refresh Token 검증 및 Access Token 재발급 로직을 수행합니다.<br>
JsonToDbService: 관리자(Admin)가 초기 게임 데이터를 JSON 파일로부터 읽어 DB에 적재하는 자동화 로직을 담당합니다.<br>
<br>
  (5). Repository & Domain Layer (데이터 접근 및 도메인 계층)<br>
Spring Data JPA를 사용하여 데이터베이스 접근 로직을 추상화하고, 객체 지향적인 도메인 모델을 구축했습니다.<br>
<br>
  (6). Database Layer<br>
MySQL: 회원 정보, 게임 리스트, 채팅 메시지 내역 등 영구적으로 보존되어야 하는 데이터를 관리합니다.<br>
Redis (In-Memory DB): 휘발성이 강하고 I/O 속도가 빨라야 하는 JWT Refresh Token 및 블랙리스트(로그아웃 처리), WebSocket 채팅 세션 정보를 관리하여 DB의 부하를 줄였습니다.
<br>
<br>
엔티티 매핑 관계 (ERD 구조) <br>
Member ↔ MemberGame ↔ Game (M:N 해소): 회원이 선택한 플레이 게임 목록을 관리하기 위해, MemberGame이라는 중간 엔티티를 두어 다대다 관계를 일대다/다대일/일대다 관계로 안전하게 풀었습니다.<br>
Member ↔ Message (1:N): 하나의 회원은 송신자(Sender) 또는 수신자(Receiver)로서 여러 메시지를 가질 수 있습니다.<br>
