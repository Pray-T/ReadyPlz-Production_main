[AWS 메인으로 돌아가기](./aws_main.md)

## 4. Https관련

1. HTTPS를 적용하기 위해서 인증서를 발급 받았습니다. AWS의 "AWS Certificate Manager(ACM)"에서 인증서를 요청합니다.

<img width="1664" height="342" alt="스크린샷 2026-03-11 185841" src="https://github.com/user-attachments/assets/97bf0cb4-7afd-4112-82cf-cf85f0e9caf6" />


<img width="1304" height="706" alt="스크린샷 2026-03-11 190054" src="https://github.com/user-attachments/assets/38793717-dfc0-4420-b7e3-a1dcbb32168f" />


2.검증 대기 중이 검증 완료로 변경 되면 Route 53에서 레코드 생성 버튼을 눌러 인증을 완료합니다. 이 과정을 통해서 AWS에서 제가 만든 사이트가 안전한 사이트라는 인증을 받습니다. <br>
도메인의 DNS 설정(Route53)에서 AWS가 지정해준 암호를 적음으로써 인증을 받습니다. 해당 사이트의 실소유자임을 증명하고 해당 도메인의 권한자임을 증명하는 셈입니다. 일전에 가비아에서 발급받은 도메인의 네임서버를 AWS의 Route53으로 이관을 해둔 상태이기 때문에 인증절차가 더욱 간편해졌습니다.
<img width="1313" height="426" alt="스크린샷 2026-03-11 190204" src="https://github.com/user-attachments/assets/41ea5e43-90fe-4381-87b0-5780dcd223f2" />

3.EC2의 ELB에 HTTPS에 대한 리스너 규칙을 추가합니다. <br>
ELB에 리스너를 추가하는 이유? 리스너는 ELB가 외부에서 들어오은 클라이언트의 트래픽을 어떤 포트, 프로토콜로 처리할지를 결정합니다. 처음 ELB(ALB)를 생성하면 기본값은 80번 포트입니다. 이는 보안화되지 않은 프로토콜입니다. 이제 이 리스너에 HTTPS(443번 포트)를 추가함으로써 80번 포트뿐만 아니라 HTTPS 프로토콜도 같이 처리할 수 있습니다. 암호화된 통신을 위해서는 암복호화 키가 필요합니다. 이 과정을 위해서 1번에서 언급한 "ACM(AWS Certificate Manager)"에 인증서가 필요합니다. 리스너 규칙을 추가함으로써 발급받은 인증서를 리스너에 장착해주는 과정입니다.

<img width="1638" height="359" alt="스크린샷 2026-03-12 171955" src="https://github.com/user-attachments/assets/4e5e6f8f-d57d-40fb-aece-a2a3f53c3b91" />
<img width="1456" height="658" alt="스크린샷 2026-03-12 172110" src="https://github.com/user-attachments/assets/deee45ef-975d-45cf-b7d9-2aaf01f46d07" />
<img width="1457" height="486" alt="스크린샷 2026-03-12 172144" src="https://github.com/user-attachments/assets/d2772087-70b6-4048-83d0-dcc5431131e2" />

4.HTTP로 접속해도 HTTPS로 리다이렉트 되도록 설정합니다. (기존의 HTTP에 대한 리스너 규칙은 삭제합니다.)
<img width="1466" height="652" alt="스크린샷 2026-03-12 172626" src="https://github.com/user-attachments/assets/505bc04a-a81f-4df6-a6bf-0d09e0b59da0" />
