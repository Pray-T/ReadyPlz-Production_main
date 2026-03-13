1. 서비스를 제공할 리젼을 선택하고 "데이터베이스 생성"을 클릭하여 데이터베이스를 생성합니다.
<img width="400" height="736" alt="스크린샷 2026-03-12 195551" src="https://github.com/user-attachments/assets/d7b2fafa-c654-45e4-95c0-bc57b4a5cb59" />
<br>
<img width="600" height="199" alt="스크린샷 2026-03-12 195620" src="https://github.com/user-attachments/assets/4bcab5ec-40c2-4222-bdc6-c13bb1426efe" />

<br>
2.사용할 데이터베이스 종류와 버전을 선택합니다. 이 프로젝트의 경우 MySQL이 사용되었습니다.
<img width="900" height="675" alt="스크린샷 2026-03-12 200155" src="https://github.com/user-attachments/assets/af64a37a-b248-4942-bfbc-473680ff893c" />


3.템플릿은 "프로덕션", 가용성 및 내구성은 "단일 AZ DB 인스턴스"를 선택했습니다. 트래픽이 적은 상태이기에 단일 DB를 선택했고 무중단이 필수적인 서비스는 아니라고 판단하여 제한된 예산에 맞추기 위하여 단일 AZ DB 인스턴스를 선택했습니다.
단일 인스턴스의 리스크를 보완하기 위해서 AWS의 RDS의 자동 백업, 스냅샷 기능을 활용하여 장애 발생시 수동 복구 할 수 있는 지점을 마련하여 유사시를 대비했습니다.
<img width="1293" height="589" alt="스크린샷 2026-03-12 200658" src="https://github.com/user-attachments/assets/9794199a-714e-4e02-9b76-6b9201ed095b" />

4.DB를 식별할 수 있는 식별자와 마스터 사용자의 이름 및 비밀번호를 설정합니다.
<img width="1620" height="673" alt="Image" src="https://github.com/user-attachments/assets/4ca85437-d3b2-42a7-8832-3921e0add0b0" />

5."EC2 컴퓨팅 리소스에 연결 안함"과 "퍼블릭 액세스"를 "예"로 설정함으로써 직접 만든 보안 그룹을 적용했습니다. 이를 통해서 수정사항이 많은 초기 개발단계에서 AWS에 로컬컴퓨터를 통해 원격으로 DB에 접속할 수 있습니다. 따라서 RDS를 먼저 생성하고 그 뒤에 보안 그룹의 인바운드, 아웃바운드 규칙을 설정함으로써 단일 IP가 아닌 EC2 계층 전용 보안그룹을 적용했습니다. <br>
EC2들이 공통으로 사용 되는 보안 그룹의 인바운드 설정을 통해서 특정 IP, 앱이 구동되는 EC2 인스턴스의 특정 보안그룹에서만 접근을 허용하도록 설정했기 때문에 외부 공격에 대한 보안 리스크를 감소시켰습니다.<br>
또한 추후에 고도화가 필요해지면 RDS의 퍼블릭 엑세스를 "아니요"로 설정하고 경유지 역할을 하는 EC2 인스턴스를 생성하여 이 경유지 역할을 하는 인스턴스를 통해서 AWS의 프라이빗 망에 접속하는 방식을 염두에 두었습니다. <br>
<img width="1293" height="732" alt="Image" src="https://github.com/user-attachments/assets/2d04d1fe-2a86-4697-b04d-b330a8ce4d39" />

6.RDS의 보안그룹을 설정하고 적용합니다. 인바운드 설정에 5번에서 언급한 특정 IP, 앱이 실행되는 EC2 인스턴스를 통해서만 접근을 허용했습니다. 보안 그룹을 생성한 후 적용할 RDS 인스턴스을 수정하여 연합니다.
<img width="1248" height="641" alt="Image" src="https://github.com/user-attachments/assets/d742c368-49b9-485e-87c3-37178106817b" /> <br>
<img width="1208" height="363" alt="Image" src="https://github.com/user-attachments/assets/9487c4f2-6efd-4f17-87e7-03930c53b140" /> <br>


7. 다양한 언어와 이모지들의 활용을 위해서 파라미터 그룹을 설정합니다. 기본값은 최대 3바이트만 저장 가능한 utf8입니다.( 전 세계 표준인 UTF-8과 다름). 그렇기에 4바이트의 이모지까지 깨지지 않고 전송할 수 있게 파라미터 설정을 합니다. <br>
아래의 속성들을 "utf8mb4"로 변경합니다.<br>
Client & Connection: 클라이언트가 4바이트짜리 이모지를 입력해서 서버로 보낼 때, 그 통신 글자가 깨지지 않게 합니다.<br>
Database & Server: 서버와 DB가 4바이트 이모지를 안전하게 디스크 저장하도록 합니다.<br>
Results: 나중에 저장된 채팅을 조회할 때 화면에 온전히 보여주도록 보장합니다. <br>
<img width="1210" height="252" alt="Image" src="https://github.com/user-attachments/assets/a0a1e9ce-a445-424c-ac6c-8be7b498a645" /> <br>
collation_connection, collation_server의 속성을 "utf8mb4_unicode_ci"로 변경합니다. 이렇게 설정함으로써 정렬의 정확도를 높이고 사전의 순서대로 정렬을 가능케 합니다. <br>
또한 커넥션(통신하는 쪽)과 서버(저장을 담당하는 쪽)의 정렬 기준을 맞춤으로써 동기화와 인덱스 효율을 챙길 수 있습니다. <br>
<img width="1207" height="155" alt="Image" src="https://github.com/user-attachments/assets/857e9bc8-da4b-42dc-bfcd-4724d808e0ac" />
