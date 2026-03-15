1. SSH에서 Java 설치 후 버전 확인을 진행했습니다.
<img width="618" height="11" alt="스크린샷 2026-03-06 163020" src="https://github.com/user-attachments/assets/e0dfe483-ef76-4ad8-acf9-63b52ba3909d" />
<br>
<br>
<img width="727" height="67" alt="스크린샷 2026-03-06 162932" src="https://github.com/user-attachments/assets/0fb03e46-0862-4c47-8b44-640a163b11db" />
<br><br><br>

2. SpringBoot에서 띄운 서버와 토큰 관리용 Redis서버를 연결하기 위해서 Redis를 먼저 설치하고 Redis가 잘 설치되어 있는지 확인합니다.
<img width="549" height="23" alt="Image" src="https://github.com/user-attachments/assets/2ab1cde1-d6f7-4f32-a163-5fabcbc09c62" />
<img width="562" height="21" alt="Image" src="https://github.com/user-attachments/assets/354c1194-c65b-4e45-92be-9d2f44f27eff" />
<img width="591" height="30" alt="Image" src="https://github.com/user-attachments/assets/369a26fc-5526-4ad2-9c73-d5c1b6f30485" />
<img width="920" height="46" alt="Image" src="https://github.com/user-attachments/assets/56e7488a-b2bc-4fd5-9e56-55bb9f3e3ae7" />

 
4. 깃헙에서 프로젝트를 당겨온 후 깃헙에 저장하지 않은 민감한 정보가 담겨져 있는 application.properties를 설정해줍니다.
<img width="820" height="16" alt="스크린샷 2026-03-06 165022" src="https://github.com/user-attachments/assets/5dbcad7a-fcc5-451e-878a-03f8a952c550" />
<br>
<br>
<img width="784" height="31" alt="스크린샷 2026-03-06 170156" src="https://github.com/user-attachments/assets/12788587-e6f3-4116-8789-94321ab8e577" />
<br><br><br>

5. 프로젝트를 빌드하고 백그라운드에서 서버를 실행시켜줍니다.
<br>
<img width="529" height="30" alt="스크린샷 2026-03-06 170732" src="https://github.com/user-attachments/assets/1eb87b87-9b22-4309-a0ec-c39dbf8ba1cd" />
<br>
<br>
<img width="949" height="53" alt="스크린샷 2026-03-06 170645" src="https://github.com/user-attachments/assets/f0a38cee-20cb-45e9-8b8c-ec8e0af190cc" />

<br><br><br>

  ! 문제 발생: JVM의 과도한 메모리 사용으로 SSH이 멈추는 현상이 발생했습니다. <br>
    해결방안 : JVM의 메모리 사용을 제한합니다.
<img width="769" height="24" alt="스크린샷 2026-03-06 171000" src="https://github.com/user-attachments/assets/42da38aa-faf5-4edd-b2ef-9322af792058" />
