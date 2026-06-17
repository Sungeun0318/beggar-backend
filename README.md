# 거지 우정 수호대 (Beggar Friendship Guardians)

## 1. 프로젝트 소개

### 1.1 프로젝트 이름: 

>**거지 우정 수호대 (Beggar Friendship Guardians)**

### 1.2 기획 의도 및 배경
- **사회적 측면**: 익명 예산 제출 기능을 통해 모임 내 과소비 스트레스를 줄이고, 함께 소비를 절약하고 재미있고 건강한 소비 문화를 지향합니다.
- **기술적 측면**: WebSocket(STOMP)을 활용한 실시간 상태 동기화, Google Vision AI 기반의 영수증 OCR 자동 인식, JWT 기반의 보안 인증 등 최신 백엔드 기술을 실무적으로 적용했습니다.
- **데이터 측면**: 착한가격업소 공공데이터 및 카카오 로컬 API를 연동하여 유저의 위치 기반 절약형 장소를 추천하고, 영수증 데이터를 분석하여 객관적인 소비 지표를 제공합니다.

## 2. 팀원 / 역할

**박소영(팀장)** :
+ **거지방 메인 도메인 구축**: WebSocket 기반 실시간 방 생성, 입장, 상태 관리 로직 구현
+ **예산 수립 시스템**: 유저별 예산 제출, 최저가 기반 총예산 확정 알고리즘 및 점수 산정 로직 개발
+ **프로젝트 매니징**: JIRA를 활용한 업무 이슈 트래킹 및 전체 일정 관리

**김성은(팀원)** :
+ **관리자 대시보드 구축**: 회원 관리, 방 모니터링, 게시글/댓글 관리 등 전체 운영 시스템 설계 및 구현
+ **예산 내 추천 및 예측 로직**: 예산 맞춤형 실시간 장소 추천 및 지출 패턴 분석을 통한 예산 초과 위험도 예측
+ **AI 서버 배포 및 GIT 병합**: AI 서버의 AWS 배포, GIT 관리


**이연지(팀원)** :
+ **영수증 OCR 연동**: Google Vision AI를 활용한 영수증 분석 및 데이터 자동화
+ **지출 관리 시스템**: 통합 및 분할 영수증 처리 로직 개발로 복합적인 지출 증빙 지원 
+ **관리자 서버 배포**: 관리자 전용 서버의 AWS 배포

**이태현(팀원)** :
+ **커뮤니티 서비스**: 자유 게시판, 인기글 선정 로직 및 WebSocket 기반 실시간 채팅 기능 개발
+ **메인 서비스 배포**: 백엔드 메인 서비스의 AWS 배포 및 전체 배포 파이프라인 관리
+ **스케쥴러 기반 채팅 내역 관리 담당**: Spring Scheduler를 활용한 채팅 내역 자동 정리 및 시스템 부하 관리

**임도경(팀원)** : 
+ **회원 인증 및 보안 관리**: JWT 기반 인증 인터셉터, 사용자 프로필 관리 및 영수증 히스토리 조회 기능 구현 및 카카오 소셜 로그인 연동
+ **거지 점수 알고리즘**: 유저별 소비 패턴을 분석한 '거지력' 산정 로직 및 실시간 랭킹 시스템 개발
+ **소비 인사이트**: 데이터 기반의 개인별 소비 패턴 분석 및 인사이트 제공 기능 구현

## 3. 기술 스택

### **Backend**
<img src="https://img.shields.io/badge/Java_17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white"> <img src="https://img.shields.io/badge/Spring_Boot_3.3-6DB33F?style=for-the-badge&logo=springboot&logoColor=white"> <img src="https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white"> <img src="https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white">
<br>
<img src="https://img.shields.io/badge/Lombok-BC204B?style=for-the-badge&logo=databricks&logoColor=white"> <img src="https://img.shields.io/badge/WebSocket(STOMP)-000000?style=for-the-badge&logo=socketdotio&logoColor=white"> <img src="https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=for-the-badge&logo=spring&logoColor=white"> <img src="https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white">

### **Frontend**
<img src="https://img.shields.io/badge/React-61DAFB?style=for-the-badge&logo=react&logoColor=black"> <img src="https://img.shields.io/badge/HTML5-E34F26?style=for-the-badge&logo=html5&logoColor=white"> <img src="https://img.shields.io/badge/CSS3-1572B6?style=for-the-badge&logo=css3&logoColor=white"> <img src="https://img.shields.io/badge/JavaScript-F7DF1E?style=for-the-badge&logo=javascript&logoColor=black">

### **API**
<img src="https://img.shields.io/badge/Kakao_Local_API-FFCD00?style=for-the-badge&logo=kakaotalk&logoColor=black"> <img src="https://img.shields.io/badge/Public_Data_API-003399?style=for-the-badge&logo=data-dot-gov&logoColor=white"> <img src="https://img.shields.io/badge/AWS_S3-569A31?style=for-the-badge&logo=amazons3&logoColor=white"> <img src="https://img.shields.io/badge/Google_Vision_OCR-4285F4?style=for-the-badge&logo=googlecloud&logoColor=white">

### **ETC**
<img src="https://img.shields.io/badge/GitHub-181717?style=for-the-badge&logo=github&logoColor=white"> <img src="https://img.shields.io/badge/Figma-F24E1E?style=for-the-badge&logo=figma&logoColor=white"> <img src="https://img.shields.io/badge/Draw.io-F08705?style=for-the-badge&logo=diagramsdotnet&logoColor=white"> <img src="https://img.shields.io/badge/Jira-0052CC?style=for-the-badge&logo=jira&logoColor=white">

## 4. 서비스 아키텍처
<div align="center">
  <img width="600" alt="Image" src="https://github.com/user-attachments/assets/310bf8e6-82ff-4dcd-9b7a-74a18a71a32a" />
</div>

## 5. 시연영상 링크
[시연영상](https://youtu.be/cXXMjrrgYiQ)

## 6. 참고 링크
[배포 링크](https://dgh1r60fiahrz.cloudfront.net) <br/>
[프론트](https://github.com/Sungeun0318/beggar-webfront) <br/>


