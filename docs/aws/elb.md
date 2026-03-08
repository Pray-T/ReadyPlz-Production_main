1. 서비스 하는 지역의 리전을 선택한 후 로드 밸런서를 생성했습니다. HTTPS를 위해서 로드 밸런서를 사용하기 때문에 ALB(Application Load Balancer)를 선택했습니다.
ALB를 앞단에 두게 됨으로써 EC2가 직접 HTTPS를 처리하지 않아도 됩니다. ALB가 클라이언트와 HTTPS로 통신하며 암/복호화를 전담합니다. 추가적으로 HTTPS를 위해서는 SSL인증이 필요한데 이를 무료로 발급받을 수 있고 자동적으로 갱신을 해줍니다.
<img width="800" height="500" alt="스크린샷 2026-03-06 175106" src="https://github.com/user-attachments/assets/593fcc56-7cb7-4a22-a501-5cf1975d29b6" />

2.퍼블릭 IP주소로 서비스를 운영하고 이에 따라서 로드밸런서가 퍼블릭 IP를 가지는 "인터넷 경계"를 선택했습니다. Ip4를 사용하는 인스턴스 사용하기에 "Ip4"를 선택했습니다.

<img width="800" height="450" alt="스크린샷 2026-03-06 180028" src="https://github.com/user-attachments/assets/3b5dfaf9-5bd3-4280-8a96-a49937f30227" />


3.가용 영역제한은 로드 밸런서가 어떤 가용 영역으로만 트래픽을 보낼 건지 제한하는 기능입니다. 제한을 두지 않고 가용 영역을 모두 체크해서 한 곳이 멈추더라도 자동적으로 트래픽을 우회하여 서비스가 중단없이 운영될 수 있게 했습니다. 또한 나중에 사용자 증가에 의한 오토 스케일링을 위해 가용 영역을 모두 체크했습니다.

<img width="800" height="450" alt="스크린샷 2026-03-06 181033" src="https://github.com/user-attachments/assets/dccaacf2-a96d-491d-9747-33ee88ec0bd9" />

4."EC2 보안 그룹"에서 보안 그룹을 생성했습니다. 

<img width="800" height="284" alt="스크린샷 2026-03-08 173114" src="https://github.com/user-attachments/assets/e6bd3216-e5d5-46cc-a629-708f06ee4fd5" />
<br>
ELB특성상 인바운드 규칙에 HTTP, HTTPS 포트로 모든 IP요청을 받을 수 있게 설정했습니다.
<br>
<img width="800" height="284" alt="스크린샷 2026-03-08 172918" src="https://github.com/user-attachments/assets/842847f4-4f28-4ef3-92f4-dae2a11b5d1c" />
<br>
5. 생성한 보안그룹을 ELB에 등록시켰습니다.
<img width="800" height="451" alt="스크린샷 2026-03-08 173211" src="https://github.com/user-attachments/assets/dac3b3ce-af2b-4362-bfd5-e421744d0cf9" />
