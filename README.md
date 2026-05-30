# beggar-backend

거지 우정 수호대 (Save My Friendship) — Spring Boot 백엔드 API

## 현재 상태 (2026-05-30)
**스캐폴딩 완료**. Spring Boot 3.3.5 + Java 17 + Gradle (Groovy DSL) MVC 셸이 생성됨.
모든 엔티티/리포지토리/서비스/컨트롤러/DTO 골격이 들어가 있고, 카카오 로그인 본체 + AI 중계 본체 + OCR 콜백은 TODO로 남아있음.
DB 설계는 커뮤니티, 통합/분할 영수증, 방별 거지평가 기준으로 먼저 갱신됨. 백엔드 Java 구현은 별도 단계에서 맞춘다.

## 기술 스택
- **DB**: MySQL 8.0 (H2 미사용)
- **HTTP 요청**: WebFlux WebClient (카카오 API, AI 서버 호출용)
- **인증**: JWT (jjwt) — Spring Security 프레임워크 미사용. `HandlerInterceptor` + `@LoginUser` 어노테이션 + `ArgumentResolver` 패턴
- **비밀번호**: spring-security-crypto의 BCrypt만 사용
- **크롤링**: Jsoup (정적) + Selenium + WebDriverManager (동적)
- **XML 파싱**: jackson-dataformat-xml
- **템플릿**: Thymeleaf
- **유틸**: Lombok

## 패키지 구조 (com.beggar.api)
```
api/
├── BeggarApplication.java
├── common/                      공통 (BaseTimeEntity, ApiResponse, Exception 핸들러)
├── config/                      JpaConfig, WebConfig, CorsConfig, PasswordEncoderConfig, WebClientConfig
├── security/                    JwtTokenProvider, JwtInterceptor, LoginUser(@), LoginUserArgumentResolver
├── entity/                      현재 스캐폴딩 엔티티. DB 목표 설계는 docs/DB_DESIGN.md 기준
├── repository/                  JpaRepository 확장
├── service/                     7 서비스 (Auth, User, Room, Budget, Receipt,
│                                 Recommendation, BeggarScore)
├── controller/                  7 컨트롤러 (도메인별 RestController)
└── dto/
    ├── auth/      KakaoLoginRequest, RefreshTokenRequest, TokenResponse
    ├── user/      UserResponse
    ├── room/      CreateRoomRequest, JoinRoomRequest, RoomResponse, RoomMemberResponse
    ├── budget/    SubmitBudgetRequest, BudgetResultResponse
    ├── receipt/   ReceiptCreateRequest, ReceiptUpdateRequest, ReceiptResponse
    ├── recommendation/  RecommendationResponse (+ Place 내부 record)
    └── ranking/   RankingEntryResponse, BeggarScoreResponse
```

## 인증 흐름 (JWT but no Spring Security)
1. 프론트가 `POST /auth/kakao` (카카오 액세스 토큰 첨부) → `TokenResponse` (자체 JWT) 반환
2. 이후 모든 요청에 `Authorization: Bearer {accessToken}` 헤더 첨부
3. `JwtInterceptor`가 토큰 검증 → request attribute `userNo` 저장
4. 컨트롤러 메서드는 `@LoginUser Long userNo`로 주입받음
5. 토큰 만료 시 `POST /auth/refresh` 로 갱신
- 공개 엔드포인트: `/auth/kakao`, `/auth/refresh`, `/error`, `/actuator/health`
- 그 외 전부 인터셉터 통과 필요

## Tech Stack (예정)
- Java 17 + Spring Boot 3.x
- Spring Web / Spring Data JPA / Spring Security
- JWT (액세스/리프레시 토큰)
- 카카오 OAuth2 (`kakao_flutter_sdk_user` ↔ `/auth/kakao`)
- MySQL 8.0 (운영) / H2 (로컬 테스트)
- AWS (EC2, RDS, S3)
- Gradle (Kotlin DSL 권장)

## 1차 구현 범위 (MVP)
프론트 13개 화면이 mock 없이 동작하도록 만드는 것이 1차 목표입니다.
DB는 [`../docs/DB_DESIGN.md`](../docs/DB_DESIGN.md)의 **총 10개 테이블**을 목표로 한다.
- `users / rooms / room_members / room_purpose_tags / budgets / room_budget_results / receipts / receipt_splits / room_beggar_scores / community_posts`

### 인증
- `POST /auth/kakao` — 카카오 OAuth 토큰 → 이메일 조회 → 자체 JWT 발급 (없으면 `users` INSERT)
- 로컬 로그인 확장 시에도 같은 `uemail` 기준으로 사용자 중복을 막는다.
- `POST /auth/refresh` — 리프레시 토큰으로 액세스 토큰 재발급
- `POST /auth/signout` — 로그아웃
- `DELETE /users/me` — 회원 탈퇴 (Hard delete or `role='WITHDRAWN'` 정책 결정 필요)

### 사용자
- `GET /users/me` — 마이페이지 프로필 조회 (`users` 1행)

### 거지방 (Room)
- `GET /rooms/my` — 홈 화면 카드 리스트 (`rooms ⋈ room_members WHERE status='ACTIVE'`)
- `POST /rooms` — 방 생성 (room_name, room_code, max_member_count 생성, tags[] 동시에 `room_purpose_tags` INSERT)
- `GET /rooms/{no}` — 방 상세 (+ tags, + 예산 결과)
- `PATCH /rooms/{no}/settings` — 반장만 지역/태그/최대 인원 수정. 최대 인원은 현재 참여 인원보다 낮게 줄일 수 없고 100명까지 허용
- `POST /rooms/join` — `room_code`로 `room_members` INSERT (`status='ACTIVE'`) — 친구 전용 방용
- `GET /rooms/{no}/members` — 입장 현황 (초대 화면 폴링). **금액 절대 노출 금지**, `EXISTS(budgets)` 여부만 반환

> `is_friends` 정책 (rooms 컬럼): 현재 MVP는 친구 초대 기반 방 중심이다. `FALSE`는 공개 방 후보로 남겨두지만 커뮤니티와 분리하며, 프론트 하단 탭에는 공개방/익명방 목록을 두지 않는다.

### 익명 예산
- `POST /rooms/{no}/budget` — 본인 예산 제출 (INSERT or UPDATE on `room_member_id`)
- `POST /rooms/{no}/budget/confirm` (또는 마지막 제출 시 자동) — 모든 ACTIVE 멤버 제출 확인 → `MIN(budget_amount) × COUNT(*)` 계산 → `room_budget_results` INSERT + `rooms.total_budget` UPDATE (동일 트랜잭션)
- `GET /rooms/{no}/budget/result` — `room_budget_results` 조회

### 추천 (Python AI 서버 중계, DB 미적재)
- `GET /rooms/{no}/recommend` — 위치 + 예산 + 카테고리 → AI 서버 호출 → 응답 그대로 반환
- **채택률/선택 트래킹 테이블 없음**. 호출 시점에만 사용

### 영수증
- `POST /receipts` (multipart) — S3 업로드 → `receipts` INSERT (`ocr_status='PENDING'`)
- OCR 콜백 처리 → `ocr_status / store_name / total_amount / amount / address / center_lat / center_lng` 업데이트
- `PATCH /receipts/{id}` — 사용자 수동 보정 (amount만 변경, total_amount는 OCR 원본 보존)
- `GET /receipts?roomNo=...&sort=created_at,desc`

### 거지력 / 랭킹
- `GET /rooms/{no}/beggar-score` — 해당 방 안에서만 쓰는 공동 점수/칭호/누적 절약 금액
- **내부 산식** (DB_DESIGN.md "9. room_beggar_scores" 참조):
  - `score = 예산준수율 35% + 절약률 35% + 착한가격업소 인증 점수 30%` 후보
  - 칭호 5단계: 아기 거지 / 성장하는 거지 / 알뜰한 거지 / 프로 거지 / 전설의 거지
- **재계산 트리거**: 영수증 INSERT/UPDATE, 예산 확정, 멤버 상태 변경 시 해당 `room_no` 점수만 UPSERT
- **방식**: **동기 + 동일 `@Transactional` 안에서 처리**
  - ReceiptService.save() → receipts INSERT → BeggarScoreService.recalculate(roomNo) → room_beggar_scores UPSERT, 모두 한 트랜잭션
  - 방 1개 점수만 갱신하므로 락 경합/성능 영향 미미
  - 트래픽 증가 시점에 `@Async` 분리 또는 메시지 큐로 확장
- **신규 가입자**: 전역 점수 행을 만들지 않는다. 방 참여/예산/영수증 흐름에서 방별 점수를 생성한다.

## 실행 방법

### 1. MySQL 준비 (최초 1회)
```bash
# beggar DB + 10개 테이블 생성
mysql -u root -p < sql/init.sql
```

### 2. Gradle Wrapper 생성 (최초 1회)
```bash
cd backend
gradle wrapper          # 또는 IntelliJ로 열면 자동 생성
```

### 3. 실행
```bash
# 환경 변수 (필요 시)
export DB_HOST=localhost DB_PORT=3306 DB_NAME=beggar DB_USERNAME=root DB_PASSWORD=root

./gradlew bootRun
# → http://localhost:8080
```

기본값(root/root, localhost:3306, db=beggar)으로 충분하면 env 없이 그냥 `./gradlew bootRun`.

### 4. 운영 (예정)
`application-prod.properties`는 `.gitignore`에 포함됨. RDS 엔드포인트 + 환경변수 주입으로 부팅.

## 다음 작업 (TODO)
- [ ] `AuthService.loginWithKakao()` — Kakao API (/v2/user/me) 호출로 사용자 정보 추출
- [ ] `RecommendationService.recommend()` — `RestClient`로 Python AI 서버 호출 + 응답 매핑
- [ ] 영수증 OCR 콜백 엔드포인트 (`POST /receipts/{id}/ocr-callback`) — AI 서버에서 결과 push
- [ ] S3 presigned URL 발급 엔드포인트 (`POST /receipts/upload-url`)
- [ ] Flyway/Liquibase 도입 (운영 전환 시)
- [ ] 통합 테스트 (TestContainers + MySQL)
- [ ] `withdraw` 정책 결정 (hard delete vs soft)

## 익명성 가드 체크리스트
- `BudgetRepository`에서 금액 SELECT 메서드는 본인 조회 + 확정 계산 2곳만 노출
- 멤버 목록 DTO에 `budget_amount` 절대 포함 금지 → `boolean submitted` 만
- 응답 직렬화 시 잘못 노출되지 않도록 `BudgetDto` 자체를 외부 응답에 쓰지 말기

## 참고 문서
- DB 스키마: [`../docs/DB_DESIGN.md`](../docs/DB_DESIGN.md)
- 앱 기능 명세: [`../docs/APP_FEATURES.md`](../docs/APP_FEATURES.md)
- Python AI 서버: [`../docs/PYTHON_AI_FEATURES.md`](../docs/PYTHON_AI_FEATURES.md)
- 프론트 화면 ↔ API 매핑: [`../frontend/STRUCTURE.md`](../frontend/STRUCTURE.md) (하단 "화면별 ↔ 백엔드 API 매핑" 표)
