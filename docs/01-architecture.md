[🔙 메인으로 돌아가기](../README.md)

# 🌊 1. 웹앱 흐름 및 아키텍처 개요

### 권한 분리
ROLE에 따라 관리자(ADMIN) 전용 경로와 사용자(USER) 전용 경로가 존재합니다.
<img width="522" height="92" alt="스크린샷 2025-10-12 182028" src="https://github.com/user-attachments/assets/58497bac-732e-45ee-88a3-5d678900a51f" />

### 관리자 기능
ADMIN은 JSON 파일로부터 DB를 업데이트하는 데 사용됩니다.
<br>
<img width="541" height="207" alt="스크린샷 2025-10-12 182809" src="https://github.com/user-attachments/assets/dbe9bfe3-1a1f-495c-a1b4-00848e58d3de" />

### 고객 센터 채널
사용자가 문의사항을 작성한 경우 기존의 채팅 기능을 활용하여 ADMIN 계정에게 메시지가 전송됩니다. ADMIN에게 보낸 메시지는 사용자 측 채팅 목록에 표시되지 않습니다.

### 데이터 흐름
클라이언트가 요청을 보내면 JWT를 통해 Spring Security 인증을 거치고 컨트롤러가 요청을 처리합니다.
- **MySQL**: 게임 정보, 유저 정보, 메시지 정보 저장
- **Redis**: JWT 및 일회성 토큰 관련 정보 저장(In-Memory DB)

<img width="1882" height="553" alt="스크린샷 2025-10-12 182424" src="https://github.com/user-attachments/assets/a5fa788c-0d19-46d0-9ee0-dcb7db051035" />

### 인프라 및 배포
- AWS EC2(Ubuntu)를 통해 서버 배포 및 내부 Redis 구동
- AWS RDS를 통해 MySQL 구축
- AWS ELB를 사용하여 엔드포인트 보안, 가용성 증대 및 라우팅 효과 적용

<img width="1060" height="273" alt="스크린샷 2025-10-12 184338" src="https://github.com/user-attachments/assets/c7ec06d7-4c2f-427c-b5b1-62554303fe23" />
