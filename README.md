# beggar-backend

거지 우정 수호대 (Save My Friendship) — Spring Boot 백엔드 API

## 현재 상태 (2026-05-31)
**DB/JPA 골격 최신화 + 착한가격업소 추천 API 1차 구현 완료**. Spring Boot 3.3.5 + Java 17 + Gradle (Groovy DSL) MVC 셸이 생성됨.
커뮤니티, 통합/분할 영수증, 방별 거지평가 기준으로 `init.sql`, 엔티티, 리포지토리, DTO 골격을 맞춤.
추천 API는 행정안전부 착한가격업소 OpenAPI를 WebClient로 호출해 지역/태그 기준 후보를 반환한다.
카카오 로그인 본체 + OCR 콜백 + 방/예산 실제 서비스 메서드 일부는 TODO로 남아있음.

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
├── entity/                      10개 테이블 기준 JPA 엔티티
├── repository/                  JpaRepository 확장
├── service/                     Auth, User, Room, Budget, Receipt,
│                                 Recommendation, BeggarScore, Community
├── controller/                  도메인별 RestController
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

## 1차 구현 범위 (MVP)
프론트 주요 화면이 mock 없이 동작하도록 만드는 것이 1차 목표다.
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

### 추천 (착한가격업소 API 기반, DB 미적재)
- `GET /rooms/{no}/recommend?tag={tag}&region={region}&lat={lat}&lng={lng}&radius={radius}` — 착한가격업소 API 호출 → 지역/태그/가격/거리 기준 정렬 → `RecommendationResponse` 반환
- `region`이 없고 `lat/lng`가 있으면 카카오 좌표→주소 변환으로 현재 지역을 해석한다.
- 응답 `places[]`는 업소명, 업종, 대표 메뉴명(`menuName`), 메뉴 가격(`expectedPrice`), 주소, 카테고리 기본 이미지 경로(`thumbnailUrl`), 카카오맵 검색 링크(`mapUrl`)를 포함한다.
- 응답은 `recommendationBudget`, `budgetGuide`, `fallbackApplied`를 포함한다.
- 추천 예산은 `남은 총예산 / 활성 멤버 수`를 1인 기준으로 바꾼 뒤 태그별 비율을 적용한다. 식사 계열은 다음 일정 비용을 남기도록 70%를 우선 사용한다.
- 결과가 부족하면 1인 남은 예산 전체 → 가격 조건 제거 → 지역/거리 조건 완화 순서로 fallback한다.
- `mapUrl`은 카카오맵 외부 검색 링크(`https://map.kakao.com/link/search/...`)다. 지도 SDK/로컬 API 호출이 아니므로 현재 단계에서는 카카오맵 API 키가 필요 없다.
- 현재 지역은 쿼리 파라미터로 받는다. 방 지역 저장은 `rooms` 설정 API 구현 시 DB 컬럼/DTO와 함께 확정한다.
- **추천 이력 저장 테이블 없음**. 거지력 점수는 추천 이력 기준이 아니라 영수증이 착한가격업소 API와 매칭되는지로 인증한다.
- AI Hub 여행로그 데이터는 Spring이 직접 읽지 않고, 추후 `ai/` 서버에서 다음 태그/소비 흐름 추천용으로 전처리해 사용한다.

### 영수증
- `POST /rooms/{no}/receipts` — 통합/분할 영수증 등록
  - `receiptType`: `COMBINED / SPLIT`
  - `inputMethod`: `CAMERA / GALLERY / MANUAL`
- 수동 입력은 `image_url` 없이 `ocr_status='MANUAL'`로 저장 가능
- 분할 영수증은 같은 요청에서 `receipt_splits`까지 저장
- OCR 콜백 처리 → `ocr_status / store_name / total_amount / amount / address / center_lat / center_lng` 업데이트
- 수동 입력 또는 OCR 결과의 `storeName/address`가 착한가격업소 API와 매칭되면 `good_price_*`, `good_price_matched`, `good_price_verified_at` 업데이트
- API에 없는 업소는 예산 차감과 지출 내역에는 반영하지만 착한가격업소 인증 점수에는 반영하지 않는다.
- `PATCH /rooms/{no}/receipts/{id}` — 사용자 수동 보정 (amount만 변경, total_amount는 OCR 원본 보존)
- `GET /rooms/{no}/receipts` — 방별 영수증 최신순

### 방별 거지평가
- `GET /rooms/{no}/beggar-score` — 해당 방 안에서만 쓰는 공동 점수/칭호/누적 절약 금액
- **내부 산식** (DB_DESIGN.md "9. room_beggar_scores" 참조):
  - `score = 예산준수율 35% + 절약률 35% + 착한가격업소 인증 점수 30%` 후보
  - 칭호 5단계: 아기 거지 / 성장하는 거지 / 알뜰한 거지 / 프로 거지 / 전설의 거지
- **재계산 트리거**: 영수증 INSERT/UPDATE, 예산 확정, 멤버 상태 변경 시 해당 `room_no` 점수만 UPSERT
- **방식**: **동기 + 동일 `@Transactional` 안에서 처리**
  - ReceiptService.save() → receipts INSERT → BeggarScoreService.recalculate(roomNo) → room_beggar_scores UPSERT, 모두 한 트랜잭션
  - 방 1개 점수만 갱신하므로 락 경합/성능 영향 미미
  - 트래픽 증가 시점에 `@Async` 분리 또는 메시지 큐로 확장
- **신규 가입자**: 가입 시 점수 행을 만들지 않는다. 방 참여/예산/영수증 흐름에서 방별 점수를 생성한다.

### 커뮤니티
- `GET /community/posts` — 게시글 목록
- `POST /community/posts` — 게시글 작성
- 전체 채팅방은 별도 저장 테이블 도입 전까지 API TODO로만 둔다.

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
export DB_HOST=localhost DB_PORT=3306 DB_NAME=beggar

./gradlew bootRun
# → http://localhost:8080
```

기본값은 `application.properties` 기준 `localhost:3306/beggar`, 계정 `root/1208`.

### 4. 운영 (예정)
`application-prod.properties`는 `.gitignore`에 포함됨. RDS 엔드포인트 + 환경변수 주입으로 부팅.

## 다음 작업 (TODO)
- [ ] `AuthService.loginWithKakao()` — Kakao API (/v2/user/me) 호출로 사용자 정보 추출
- [x] `RecommendationService.recommend()` — 착한가격업소 OpenAPI 호출 + 지역/태그 기준 응답 매핑
- [x] 영수증 등록/수동 OCR 반영 시 착한가격업소 인증 매칭
- [ ] 영수증 OCR 콜백 엔드포인트 (`POST /rooms/{roomNo}/receipts/{id}/ocr-callback`) — AI 서버 결과 반영
- [ ] S3 presigned URL 발급 엔드포인트 (`POST /rooms/{roomNo}/receipts/upload-url`)
- [ ] 커뮤니티 게시글 API 구현
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
