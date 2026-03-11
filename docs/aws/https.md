1. HTTPS를 적용하기 위해서 인증서를 발급 받았습니다. AWS의 "AWS Certificate Manager(ACM)"에서 인증서를 요청합니다.

<img width="1664" height="342" alt="스크린샷 2026-03-11 185841" src="https://github.com/user-attachments/assets/97bf0cb4-7afd-4112-82cf-cf85f0e9caf6" />


<img width="1304" height="706" alt="스크린샷 2026-03-11 190054" src="https://github.com/user-attachments/assets/38793717-dfc0-4420-b7e3-a1dcbb32168f" />


2.검증 대기 중이 검증 완료로 변경 되면 Route 53에서 레코드 생성 버튼을 눌러 인증을 완료합니다. 이 과정을 통해서 AWS에서 제가 만든 사이트가 안전한 사이트라는 인증을 받습니다. 도메인의 DNS 설정(Route53)에서 AWS가 지정해준 암호를 적음으로써 인증을 받을 수 있습니다. 즉 해당 사이트의 실소유자이고 해당 사이트의 DNS 설정까지도 바꿀 수 있으니 내가 이 도메인의 권한자임을 증명하는 셈입니다. 일전에 가비아에서 발급받은 도메인의 네임서버를 AWS의 Route53으로 이관을 해둔 상태이기 때문에 인증절차가 더욱 간편해졌습니다.
<img width="1313" height="426" alt="스크린샷 2026-03-11 190204" src="https://github.com/user-attachments/assets/41ea5e43-90fe-4381-87b0-5780dcd223f2" />
