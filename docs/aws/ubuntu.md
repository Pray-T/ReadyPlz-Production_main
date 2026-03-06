1. SSH에서 Java 설치 후 버전 확인을 진행했습니다.
<img width="618" height="11" alt="스크린샷 2026-03-06 163020" src="https://github.com/user-attachments/assets/e0dfe483-ef76-4ad8-acf9-63b52ba3909d" />
<br>
<br>
<img width="727" height="67" alt="스크린샷 2026-03-06 162932" src="https://github.com/user-attachments/assets/0fb03e46-0862-4c47-8b44-640a163b11db" />
<br><br><br>
  2. 깃헙에서 프로젝트를 당겨온 후 깃헙에 저장하지 않은 민감한 정보가 담겨져 있는 application.properties를 설정해줍니다.
<img width="820" height="16" alt="스크린샷 2026-03-06 165022" src="https://github.com/user-attachments/assets/5dbcad7a-fcc5-451e-878a-03f8a952c550" />
<br>
<br>
<img width="784" height="31" alt="스크린샷 2026-03-06 170156" src="https://github.com/user-attachments/assets/12788587-e6f3-4116-8789-94321ab8e577" />
<br><br><br>
  3. 프로젝트를 빌드하고 백그라운드에서 서버를 실행시켜줍니다.
<br>
<img width="529" height="30" alt="스크린샷 2026-03-06 170732" src="https://github.com/user-attachments/assets/1eb87b87-9b22-4309-a0ec-c39dbf8ba1cd" />
<br>
<br>
<img width="949" height="53" alt="스크린샷 2026-03-06 170645" src="https://github.com/user-attachments/assets/f0a38cee-20cb-45e9-8b8c-ec8e0af190cc" />

<br><br><br>

  ! 문제 발생: JVM의 과도한 메모리 사용으로 SSH이 멈추는 현상이 발생했습니다.
    해결방안 : JVM의 메모리 사용을 제한합니다.
<img width="769" height="24" alt="스크린샷 2026-03-06 171000" src="https://github.com/user-attachments/assets/42da38aa-faf5-4edd-b2ef-9322af792058" />
