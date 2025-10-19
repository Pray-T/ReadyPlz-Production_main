| 인사말 |

안녕하세요, 먼저 귀한시간을 내어 저의 깃허브에 방문해주신것에 대한 감사를 먼저 드립니다.<br/>저는 팀의 일원으로서 팀원들과 함께 좋은 에너지와 좋은 시너지를 만들며 일을 하고 싶고 동시에 개인적으로 혼자 고민하는 시간을 갖고 그것을 토대로 성장하는 전문가로서의 역량을 기르고자 하는 개발자입니다.
<br/>저는 게임 리스트에서 게임을 선택하고 해당 게임을 선택한 다른 유저가 존재하면 채팅을 할 수 있는 웹사이트인 "Readyplz.com"을 배포한 개발자입니다. JWT를 통한 로그인 시스템을 구현했으며 인증, 인가, 비밀번호의 암호화 저장을 위해서 스프링시큐리티를 사용했습니다.<br/>
해당 사이트를 배포하기 위해서 사용한 기술스택은 Java, Spring Boot, JPA, SpringSecurity, JWT, Redis, MySQL, AWS입니다. 감사합니다.


##
| 이름 | 최성욱 |

| 이메일 | cenmot@naver.com |

| 포트폴리오 | https://readyplz.com |

| 기술 스택 | Java, Spring Boot, JPA, SpringSecurity, JWT, Redis, MySQL, AWS |

| 한줄 소개 |  조직에 속한 팀원으로서의 성장과 동시에 개인의 성장을 지향하는 개발자입니다.

## Cursor IDE에 관련 설정 소개
1.VScode에서 Java를 사용하기 위해 설치한 확장 프로그램
- Extension Pack for Java, Debugger for Java, Gradle for Java, Maven for Java, Project Manager for Java, Test Runner for Java
- IntelliCode, IntelliCode API Usage Example
  
2.사용자 편의성과 SQL 편의성을 위해 설치한 확장 프로그램
- Korean Language Pack for Visual Studio Code, Language Support for Java(TM) by Red Hat
- SQLTools, SQLTools MySQL/MariaDB/TIDB

3.정확한 어시스턴스를 받기 위한 프롬프트 설정(Cursor Settings의 Rules & Memories 설정)
 
(1) Data Access and ORM
- Use Mysql DataBase for database operations.
- Implement proper entity relationships and cascading.
  
(2) API Documentation
- Use Springdoc OpenAPI (formerly Swagger) for API documentation.
  
(3) Logging and Monitoring
- Use SLF4J with Logback for logging.
- Implement proper log levels (ERROR, WARN, INFO, DEBUG).
- Use Spring Boot Actuator for application monitoring and metrics.
  
(4) Performance and Scalability
- Implement proper database indexing and query optimization.

(5) Dependency Injection and IoC
- Use constructor injection over field injection for better testability.
- Leverage Spring's IoC container for managing bean lifecycles.

(6) Configuration and Properties
- Use application.properties for configuration.
- Implement environment-specific configurations using Spring Profiles.
- Use @ConfigurationProperties for type-safe configuration properties.
(7) Java and Spring Boot Usage
- Use Java 17 or later features when applicable (e.g., records, sealed classes, pattern matching).
- Leverage Spring Boot 3.x features and best practices.
- Implement proper validation using Bean Validation (e.g., @Valid, custom validators).
(8) Naming Conventions
- Use PascalCase for class names (e.g., UserController, OrderService).
- Use camelCase for method and variable names (e.g., findUserById, isOrderValid).
- Use ALL_CAPS for constants (e.g., MAX_RETRY_ATTEMPTS, DEFAULT_PAGE_SIZE).
(9) Utilize Spring Boot's auto-configuration features effectively.

(10) Implement proper use of annotations (e.g., @SpringBootApplication, @RestController, @Service).

(11) Use Spring Boot starters for quick project setup and dependency management.

(12) Structure Spring Boot applications: controllers, services, repositories, models, configurations.

(13) Implement RESTful API design patterns when creating web services.

(14) Use Spring Boot best practices and conventions throughout your code.

(15) Write clean, efficient, and well-documented Java code with accurate Spring Boot examples.

(16) You are an expert in Java programming, Spring Boot, Spring Framework, Gradle, JUnit, and related Java technologies.

(17) If I ask for adjustments to code I have provided you, do not repeat all of my code unnecessarily. Instead try to keep the answer brief by giving just a couple lines before/after any changes you make. Multiple code blocks are ok.

(18) Split into multiple responses if one response isn't enough to answer the question.

(19) No moral lectures.

(20) No need to mention your knowledge cutoff.

(21) Treat me as an expert.

(22) First Explain, Second Confirm  then write code

(23) First think step-by-step - describe your plan for what to build in pseudocode, written out in great detail.

(24) Follow the user’s requirements carefully & to the letter.

(25) Always respond in Korean.


## 전체 아키텍처
-ROLE에 따라서 관리자 전용 경로와 사용자 전용 경로가 존재.

<img width="522" height="92" alt="스크린샷 2025-10-12 182028" src="https://github.com/user-attachments/assets/58497bac-732e-45ee-88a3-5d678900a51f" />


-ADMIN은 JSON파일로부터 DB를 업데이트 하는데 사용됩니다.

<img width="541" height="207" alt="스크린샷 2025-10-12 182809" src="https://github.com/user-attachments/assets/dbe9bfe3-1a1f-495c-a1b4-00848e58d3de" />
<br/>
<br/>

-사용자가 문의사항을 작성한 경우 기존의 채팅 기능을 활용하여 해당 내용이 ADMIN계정에게 메시지가 갑니다. ADMIN에게 보낸 메시지는 사용자측의 메시지에 표시되지 않습니다.

-클라이언트는 웹에 요청을 하면 JWT을 통해서 스프링 시큐리티를 통한 인증을 거치고 각 요청에 맞게 컨트롤러가 해당 요청을 처리합니다. DB는 MySQL과 Redis를 사용헀고 MySQL은 게임정보, 유저 정보, 메시지 정보를 담당하는 DB이고 Redis는 JWT및 일회성 토큰 관련 정보를 저장하는 DB입니다.<br/>

<img width="1882" height="553" alt="스크린샷 2025-10-12 182424" src="https://github.com/user-attachments/assets/a5fa788c-0d19-46d0-9ee0-dcb7db051035" />

-AWS에서 EC2서비스의 Ubuntu를 통해서 서버를 배포했고, AWS의 RDS를 통해서 MySQL의 역할을 하고 있고 Redis는 EC2안의 Ubuntu에 설치해서 사용하고 있습니다.

<img width="1060" height="273" alt="스크린샷 2025-10-12 184338" src="https://github.com/user-attachments/assets/c7ec06d7-4c2f-427c-b5b1-62554303fe23" />


## 주요 기능
- JWT를 통한 로그인 시스템과 Access/Refresh 이중 토큰 전략 사용, JWT관련 정보를 저장하는 In-Memory DB인 Redis 사용
- JWT에 맞춰 커스텀한 Spring Security 사용
- 이메일 비밀번호 재설정(포트 587 SMTP 사용)
- WebSocket을 통한 채팅 기능과 STOMP를 통한 수신자, 발신자 구분


## 프로토콜 및 기능 개요


1. JWT사용 이유:
- 현재는 웹앱이지만 훗날 네이티브 모바일 앱 환경으로의 확장을 고려하여 모바일 환경에 더 적합하고 확장성이 뛰어난 인증방식인 JWT방식을 채택헀습니다.
- Stateless한 상태를 통해서 인증과정에서 사용자의 정보를 DB저장하는 방식에 비하여 네트워크 통신 비용을 낮출 수 있고 사용자수가 많아지면 생기는 HttpSession방식의 병목현상을 해결할 수 있습니다.
- API통신끼리 최적회된 JWT를 사용함으로써 프론트단과 백단의 분리와 독립적 개발을 가능하게 하여 개발 및 유지보수의 용이성이 좋을것이라고 판단했습니다.
- Redis와의 조합을 통해 JWT관리의 편리함을 취하고 Stateless상태에 가깝게 아키텍쳐를 구성할 수 있었습니다.
<br/>
<br/>
2. JWT와 Redis 조합을 선택한 이유와 동작 과정:<br/>
- Redis의 특유의  인메모리 저장방식은 디스크 기반의 RDBMS과 비해 빠른 속도로 JWT가 유효한지 확인 할 수 있습니다. 이러한 장점은 요청이 잦고 인증작업이 자주 발생한는 JWT의 작업과 더욱 시너지가 좋으며 그 요청이 많을수록 더욱 큰 효과를 가져옵니다.<br/>
또한 Redis의 기본적으로 탑재되어 있는 TTL기능은 JWT의 만료시간을 설정하는데 별도의 로직을 짤 필요가 없는 이점또한 제공하기에 JWT와 Redis의 조합을 채택했습니다.<br/>
- 사용자의 인증을 위한 토큰을 서버가 아닌 클라이언트측에서 제시하는 JWT과 인메모리를 사용하는 Redis의 특성이 잘 맞물려 JWT를 담당하는 DB로 Redis를 사용했습니다.

- JWT와 Redis 동작 과정.

  <img width="1136" height="722" alt="image" src="https://github.com/user-attachments/assets/1b147b6e-260b-47df-a8eb-c3483c0f2198" />

- Access토큰, Refresh토큰 이중 토큰 전략 사용함으로써 Accees토큰이 탈취되어도 이를 빠르게 무효화시켜 토큰 탈취 리스크를 감소시켰습니다. TTL이 만료된 토큰 혹은 탈취되었다고 생각한 토큰은 블랙리스트에 올려 해당 토큰으로 인증 불가하게 구성했습니다.

 <img width="612" height="151" alt="image" src="https://github.com/user-attachments/assets/836ae0f6-83fe-480c-b6b7-0181d97d830d" />
 
<br/>
<br/>
3. 인증과 인가기능을 위한 JWT와 Spring Security:<br/>
- Spring Security 흐름: CORS → (CSRF 비활성) → JwtAuthenticationFilter → ExceptionTranslationFilter → FilterSecurityInterceptor → Controller.<br/>
- 로그인 과정은 세션기반 인증방식에서 JWT인증방식으로 변경함에 따라 FormLogin방식을 Disable하고 JWT특성에 맞춰 JwtAuthenticationFilter(OncePerRequestFilter)를 커스텀하여 순서상 필터체인의 UsernamePasswordAuthenticationFilter전에 두어서 인증을 진행했습니다.<br/>
- Spring Security 필터 체인에 JwtAuthenticationFilter(OncePerRequestFilter)를 사용했기에 요청 단위로 SecurityContext를 구성합니다. 해당 요청이 완료되거나 요청의 토큰이 블랙리스트라서 더이상 진행이 불가하다면 SecuriyContext를 비웁니다.<br/>
- AuthenticFIleter가 클라이언트로부터 입력된 아이디와 패스워드를 AuthenticationManager한테 넘겨주고 AuthenticationManager는 UserDatails를 통해서 DB를 통해 이를 검증하고 성공하면 토큰을 발급합니다.<br/>
- 요청마다 토큰 검증 → 인증 성공시 SecurityContext 주입 → 인가 처리를 과정을 거칩니다.
<img width="1867" height="201" alt="스크린샷 2025-10-12 173346" src="https://github.com/user-attachments/assets/7514d003-0326-40ad-8a66-77216049ecc9" />
  

<br/>
<br/>

4. 이메일 비밀번호 재설정을 위한 SMTP와 "포트 587 SMTP"를 선택한 이유:
- 이메일을 발신자에서 수신자로 전송하는 표준 프로토콜인 SMTP를 사용했습니다.
- SMTP중에서 "포트 587 SMTP"를 선택한 이유는 다음과 같습니다. "포트 25"의 SMTP는 암호화가 되지 않습니다 또한 대부분 ISP에서 차단당해서 선택하지 않았습니다. "포트 465"은 SMTPS는 통신의 첫 순서로 SSL/TLS 암호화를 진행하기에 안정적입니다. 하지만 후술할 "포트 587"이 평문과 암호화 방식 모두를 사용할 수 있기에 선택하지 않았습니다. "포트 587": 하나의 포트로 평문과 암호화 모두를 사용할 수 있다는 유연성을 가지고 있어서 선택했습니다. "포트 587"은 평문으로 시작하여 필요할 시 암호문으로 변경하자는 명령어인 "STARTTLS"를 통해 암호화를 진행할 수 있습니다. 이후 암호화된 채널에서 SMTP를 재시작하게 됩니다.
- 일회성 토큰과 만료시간을 설정 후 비밀번호 재설정 요청자가 이메일을 통하여 재설정 버튼을 클릭시 일회성 토큰 검증 후 비밀번호 재설정 기능 활성화합니다.
- 위의 과정에서 메일 유무에 따른 타이밍 기반 추정이 가능할 수 있으니 BCrypt 해시 연산을 통해서 의도적으로 연산 시간 증가시킵니다.(리소스는 더 소모되지만 보안을 위한 트레이드 오프)
<img width="495" height="348" alt="image" src="https://github.com/user-attachments/assets/9a928483-4644-4b2f-93c5-df11afe8614e" />

  
- 비밀번호 재설정 이메일을 요청시 @Async을 사용해서 현재 쓰레드가 아닌 지정 쓰레드로 처리해서 비동기적 처리를 통한 응답시간 감소시킵니다.

  <img width="473" height="285" alt="image" src="https://github.com/user-attachments/assets/3857b9b6-f532-46f5-8cc6-6b5da70a3de6" />

- P.S) @Async에 작동방식에 대해서: Spring이 @Async가 붙은 메서드를 프록시로 감싸서 현재 쓰레드가 아닌 지정된 쓰레드풀에 작업을 위임하게 합니다. 이 웹앱의 경우 AsyncConfig.java에 mailTaskExecutor라는 빈으로 등록하여 2~5개의 쓰레드를 사용할 수 있으며 대기 큐는 100개까지 가능합니다.

- 이메일을 통한 비밀번호 재설정 동작 과정.
  
  <img width="763" height="524" alt="image" src="https://github.com/user-attachments/assets/e1fdc7c3-4323-41ff-8554-e667aff8a9c2" />


<br/>
<br/>
5. 채팅 기능을 위한 WebSocket프로토콜과 STOMP: <br/>
- 채팅 기능을 위해서 WebSocket프로토콜을 선택한 이유는 다음과 같습니다.<br/>
서로 메시지를 주고 받은 기능을 구현하기 위해서라면 HTTP의 polling방식을 통해서도 메시지를 주고 받을 수 있습니다. 그러나 HTTP의 통신은 Stateless한 방식이기에 메시지를 주고 받기 위해서 클라이언트는 헤더를 통해 본인이 누군지를 증명해야 하고 서버측은 이를 검증하는데 리소스를 소요하게 됩니다. 즉 요청을 보내고 응답을 맺을 때마다 오버헤드가 발생해서 낭비가 심한 통신방법입니다.<br/>
이러한 방법을 보완하기 위해 생긴것이 바로 WebSocket프로토콜입니다. 클라이언트는 제일 처음 HTTP 프로토콜을 통해서 서버측에게 WebSocket프로토콜로의 업그레이드를 요청하고 서버측이 이를 수락하게 되면 양측이 연결된 세션을 종료하기 전까지는 이 연결은 유지가 됩니다.<br/>
또한 polling방식과 비교하여 헤더의 크기가 굉장히 작아 이를 해석하는데 사용되는 리소스가 줄어듭니다. 요청이 가야만 응답을 할 수 있었던 단방향의 http의 polling방식과 다르게 WebSocket프로토콜은 양방향 통신으로서 어느쪽이든 먼저 메시지를 보낼 수 있다는 장점이 존재하기에 채팅 기능을 위한 프로토콜로 WebSocket프로토콜을 선택했습니다.

- STOMP를 사용한 이유는 다음과 같습니다. WebSocket프로토콜은 양방향 통신으로서 어느쪽이던 메시지를 보낼 수 있다는 장점은 존재합니다만 누가 누구에게 보낸다는 라우팅 개념이 존재하지 않습니다. 발신자, 수신자를 구별하고 채팅을 위한 세션관리를 위한 방식으로 제가 선택한것이 STOMP입니다. STOMP를 사용하지 않는다면 개발자가 직접 발신자, 수신자를 라우팅하는 로직을 구현해야 합니다. STOMP없이 WebSokcet만 사용하여 개발자가 직접 라우팅을 구현한다면 개발자는 지속적인 연결을 위한 세션은 생성하고 사용자와 세션을 매핑하고 모든 사용자에게 메시지를 보내는 브로드캐스트와 특정 사용자에게만 메시지를 보내는 로직을 직접 구현해야합니다.

- STOMP는 발행(송신)/구독(수신) 패턴의 브로커 기반 아키텍쳐를 사용해서 송신과 수신 사이에 브로커를 둠으로써 송신자는 브로커에게만 메시지를 보내고 브로커는 해당 메시지를 수신자에게 자동 라우팅해주는 방식입니다. 원래대로라면 송신자A가 수신자 A,B,C에게 메시지를 보내려면 수신자 A,B,C를 모두 관리했어야 했지만 브로커 기반 아키텍쳐를 통하여 송신자는 수신자를 관리할 필요 없고 서로 독립적이게 변경되었습니다.

- 채팅 기능을 위한 WebSocket프로토콜과 STOMP 동작 과정.

<img width="1157" height="650" alt="image" src="https://github.com/user-attachments/assets/06ba6604-23dc-4ee7-a01e-b99bdd2796f3" />


<br/>
<br/>

- P.S) WebSocket프로토콜은 같은 클라이언트와 서버만이 통신할 수 있는데 AWS의 ELB는 어떻게 웹소켓의 통신을 가능하게 하는가?<br/>결론적으로 말하자면 AWS의 ELB는 "스티키 세션" 또는 "세션 어피니티"라는 기능을 통해서 이를 해결합니다. 이러한 "스티키 세션"은 3가지 방식으로 구현이 되는데 AWS의 ELB는 ALB(Application Load Balance)를 통한 방식으로 구현이 되어있습니다. 이름에 나와있듯이 ALB는 응용계층에서 동작하기에 HTTP의 프로토콜을 완벽하게 이해합니다. <br/> 작동 방식은 다음과 같습니다. WebSocket연결을 위한 HTTP가 최초의 요청을 보고 WebSocket프로토콜임을 인지합니다.-> <br/>설정된 라우팅 경로(특정 경로, 특정 도메인)에 따라 대상 그룹에 핸드쉐이크를 요청. 해당 그룹이 요청에 응하면 ALB가 연결되어 고정됩니다. -><br/> 고정 이후로는 ALB는 자동적으로 "스티키 세션"을 활성화 홥니다. 이후로 클라이언트가 보내는 모든 데이터 패킷은 대상 그룹의 서버 인스턴스로만 전달됩니다. 이 ALB의 "스티키 세션"은 로드밸런서가 생성한 쿠키를 기반으로 동작합니다. ALB가 클라이언트에 보내는 첫 응답에 AWSALB라는 쿠키를 포함시키고 클라이언트는 이후 모든 요청에 이 쿠키를 포함시킴으로서 ALB가 어떤 서버 인스턴스로 데이터를 보낼지 판단하게 됩니다.

<br/>
<br/>
6. 회원의 게임정보를 직접 SteamAPI를 통해서 받아오지 않고 게임 리스트를 DB에 넣은 후 선택하게 한 이유:<br/>
- 과도한 API 요청으로 "302 Too many Request"에 제한되어서 API 정보를 받지 못한것이 그 이유였습니다. 제가 만든 웹앱은 본인이 플레이하는 스팀 게임을 선택하고 같은 게임을 선택한 사용자가 있을 시 채팅을 할 수 있는 기능을 가진 웹앱입니다. 
 처음에 선택한 방법은 스팀에 API를 요청해서 게임 정보를 가져오는 방법을 택했습니다. 그러나 스팀에 API를 요청하는 것은 제한이 존재했고 스팀 측에서 이 제한 기준을 알려주지 않아서 혼자서 기준을 알아내기 위해서 시간을 늘려서도 시도를 해보고 API 요청 사이에 지연시간을 두면서 시도를 해보기도 했습니다. 하지만 결국 요청 제한의 조건을 찾아내지 못하고 개발 환경의 적은 요청에도 요청 제한이 걸린다면 실제 운영 환경에서는 요청 제한이 걸릴 것은 뻔한 일이었습니다. 그리하여 이 아키텍처는 폐기를 하고 새로운 아키텍처로 DB에 미리 게임 관련 정보를 넣어 놓고 사용자가 이를 선택하는 아키텍처를 선택해서 이 문제를 해결한 경험이 존재합니다. 이 방법을 통해 스팀 API에 강하게 결합되어 있던 게임 정보 조회 기능이 느슨해졌고 기존의 아키텍쳐인 외부의 API를 요청보다 새로운 아키첵쳐인 DB를 조회하는 것이 훨씬 빠르기에 이러한 아키텍처의 변경을 선택한 것이 훌륭한 선택이었다고 생각합니다.

<br/>
<br/>
이상으로 저의 깃헙 Readme를 마치겠습니다, 읽어주셔서 감사합니다.
