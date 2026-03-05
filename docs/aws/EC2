<p>1. 주로 서비스를 제공할 지역이 한국이기에 "아시아태평양(서울)"로 리전을 선택했습니다.</p>
<img src="https://github.com/user-attachments/assets/fcc3998d-601f-474e-b019-0dda3962f663" alt="Image 1" style="max-width: 100%;">
<br><br>

<p>2. Windows나 Mac보다 가벼운 Ubuntu를 인스턴스의 OS로 선택했습니다.</p>
<img src="https://github.com/user-attachments/assets/7fa966fe-a5b5-418b-82a5-1e46c2eaa482" alt="Image 2" style="max-width: 100%;">
<br><br>

<p>3. 사용자가 많지 않은 환경이기에 인스턴스는 t3.micro를 선택했습니다. t2.micro와 가격 차이는 크게 안 나고 2vCPU의 장점을 취할 수 있습니다.</p>
<img src="https://github.com/user-attachments/assets/04b260f1-4dc1-4c6c-8fd3-ca7556af3ad1" alt="Image 3" style="max-width: 100%;">
<br><br>

<p>4. 네트워크를 설정하고 보안 그룹을 생성하여 인바운드 규칙을 정했습니다. SSH의 원격 접근을 위해서 22번 포트를 허용했고, 백엔드 서버를 띄울 80번 포트를 허용했습니다.</p>
<img src="https://github.com/user-attachments/assets/0429bc0a-fbab-4d32-8b29-54983ed59f85" alt="Image 4" style="max-width: 100%;">
<br><br>

<p>5. 사용자가 많지 않은 점을 감안하여 스토리지는 8GB의 "gp3"를 선택했습니다. 스토리지는 부족하면 추후에 변경할 수 있기에 가벼운 스토리지로 시작했습니다.</p>
<img src="https://github.com/user-attachments/assets/8adb24b1-ead7-43b6-81ae-bfaf10912755" alt="Image 5" style="max-width: 100%;">
<br><br>

<p>6. 탄력적 IP를 설정하여 인스턴스를 중지 후 재가동하더라도 IP 변경이 없도록 설정했습니다.</p>
<img src="https://github.com/user-attachments/assets/8ea52e49-fdd4-402d-b2db-dbf77e5241e3" alt="Image 6" style="max-width: 100%;">
<br><br>

<p>7. DNS 연결을 위해 가비아에서 도메인을 구매 후 DNS 설정을 진행했습니다.</p>
<img src="https://github.com/user-attachments/assets/d94d4c76-8e28-4bb6-a76e-64df14a6d14a" alt="Image 7" style="max-width: 100%;">
<br><br>

<p>8. AWS의 Route 53에서 도메인과 탄력적 IP를 연결했습니다.</p>
<img src="https://github.com/user-attachments/assets/c8e8fe32-37a1-49b1-9d15-7db060c241ce" alt="Image 8" style="max-width: 100%;">
<br><br>

<p>9. 가비아의 네임서버에 Route 53 호스팅 영역의 NS 값 4개를 연결했습니다.</p>
<img src="https://github.com/user-attachments/assets/f63ecb97-3fc2-4530-961e-23a14a4ef12d" alt="Image 9-1" style="max-width: 100%;">
<br>
<img src="https://github.com/user-attachments/assets/46880d9e-c068-4343-83ce-a81b7828cef6" alt="Image 9-2" style="max-width: 100%;">
<br><br>
