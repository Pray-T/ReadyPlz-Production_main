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


