[메인으로 돌아가기](../README.md)

# 🛠 3. 기술적 고민 및 아키텍처 결정

## 3.1 JWT 사용 이유와 Redis와의 조합

**JWT 채택 배경:**
- 훗날 네이티브 모바일 앱 환경으로의 확장을 고려하여 수평적 확장에 적합한 인증 방식인 JWT 방식을 채택했습니다.
- Stateless 상태를 유지해 네트워크 통신 비용을 낮추고, 사용자 증가 시 발생하는 HttpSession 방식의 병목 현상을 해결할 수 있습니다.
- API 통신에 최적화된 JWT를 사용하여 프론트엔드와 백엔드의 분리 및 독립적 개발을 가능하게 하였습니다.

> **💡 Q. JWT도 결국 Redis에 저장한다면 세션 방식과 다른 점이 없지 않은가?**<br/>
> **A.** In-Memory DB인 Redis를 JWT 관리 저장소로 사용함으로써 인증에 소모되는 자원과 시간을 줄일 수 있습니다. 특히, 인증이 거부된 사용자를 찾기 위해 기존 세션 방식은 전체를 탐색해야 하지만, JWT+Redis 조합은 **Redis의 블랙리스트만을 탐색**하므로 시간이 훨씬 단축됩니다. 완전한 Stateless는 아니지만, 세션 방식보다 더욱 뛰어난 확장성을 가집니다.

**Redis 동작 과정 및 이중 토큰 전략:**
- Redis의 빠른 속도와 `TTL(Time To Live)` 기능은 JWT 유효성 검증과 만료 시간 관리에 최적의 시너지를 제공합니다.
- Access/Refresh 이중 토큰 전략을 사용하여 탈취 리스크를 감소시켰습니다. 탈취가 의심되는 토큰은 블랙리스트에 올려 인증을 차단합니다.

<img width="1136" height="722" alt="image" src="https://github.com/user-attachments/assets/1b147b6e-260b-47df-a8eb-c3483c0f2198" />
<img width="612" height="151" alt="image" src="https://github.com/user-attachments/assets/836ae0f6-83fe-480c-b6b7-0181d97d830d" />

---

## 3.2 Spring Security와 JWT 인증/인가 흐름

- **흐름**: `CORS` → `(CSRF 비활성)` → `JwtAuthenticationFilter` → `ExceptionTranslationFilter` → `FilterSecurityInterceptor` → `Controller`
- 로그인 시 FormLogin 방식을 Disable하고, 필터 체인의 `UsernamePasswordAuthenticationFilter` 이전에 커스텀한 `JwtAuthenticationFilter`를 배치했습니다.
- 요청마다 토큰 검증 → 인증 성공 시 SecurityContext 주입 → 인가 처리를 과정을 거칩니다.

<img width="1867" height="201" alt="스크린샷 2025-10-12 173346" src="https://github.com/user-attachments/assets/7514d003-0326-40ad-8a66-77216049ecc9" />

---

## 3.3 SMTP (포트 587)를 활용한 비밀번호 재설정

**포트 587을 선택한 이유:**
포트 25는 암호화가 안 되고 차단되며, 포트 465는 무조건 SSL/TLS 암호화를 진행합니다. 반면 **포트 587**은 평문으로 시작하여 필요시 `STARTTLS` 명령어를 통해 암호화 채널로 업그레이드할 수 있는 유연성이 있어 채택했습니다.

**보안 및 최적화:**
1. **타이밍 기반 추정 방지:** BCrypt 해시 연산을 통해 의도적으로 연산 시간을 증가시켜 보안을 강화했습니다.
<img width="495" height="348" alt="image" src="https://github.com/user-attachments/assets/9a928483-4644-4b2f-93c5-df11afe8614e" />

2. **비동기 처리(@Async):** 이메일 전송 시 스레드 풀(`mailTaskExecutor`)에 작업을 위임하여 응답 시간을 감소시키고 더 나은 UX를 제공합니다.
<img width="473" height="285" alt="image" src="https://github.com/user-attachments/assets/3857b9b6-f532-46f5-8cc6-6b5da70a3de6" />

3. **동작 과정:**
<img width="763" height="524" alt="image" src="https://github.com/user-attachments/assets/e1fdc7c3-4323-41ff-8554-e667aff8a9c2" />

---

## 3.4 WebSocket과 STOMP를 활용한 채팅 아키텍처

**WebSocket 프로토콜:**
HTTP Polling 방식의 오버헤드와 리소스 낭비를 해결하기 위해 양방향 통신이 가능한 WebSocket을 도입했습니다.

**STOMP 도입:**
WebSocket 자체는 라우팅 개념이 없습니다. STOMP(발행/구독 패턴)를 도입하여 브로커가 수신자에게 메시지를 자동 라우팅하도록 구현했습니다.

> **💡 AWS ELB에서의 WebSocket 통신 (스티키 세션)**
> AWS ALB(Application Load Balancer)는 HTTP 요청을 보고 WebSocket임을 인지합니다. 핸드셰이크 이후 ALB는 **스티키 세션(Sticky Session)**을 활성화하고 쿠키(`AWSALB`)를 기반으로 클라이언트의 모든 패킷을 해당 서버 인스턴스로만 고정 전달합니다.

<img width="1157" height="650" alt="image" src="https://github.com/user-attachments/assets/06ba6604-23dc-4ee7-a01e-b99bdd2796f3" />
