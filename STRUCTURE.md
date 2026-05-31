# 백엔드 파일 구조 & 기능 설명

> `backend/` 디렉터리의 모든 파일이 어떤 역할인지, 어떤 내용을 담아야 하는지 정리.
> **2026-05-28 기준**: 서비스 / 컨트롤러 / 시큐리티 본체 코드는 비어있음 (팀이 직접 구현 예정).
> 본 문서가 그 빈 파일들의 "구현 계약서" 역할을 함.

---

## 전체 구조

```
backend/
├── build.gradle                Gradle 의존성 + 빌드 설정
├── settings.gradle             프로젝트 이름
├── sql/
│   └── init.sql                MySQL 8개 테이블 DDL (한 번에 실행)
├── README.md                   실행 방법 + 기술 스택 요약
├── STRUCTURE.md                (본 문서) 파일별 상세 설명
└── src/
    ├── main/
    │   ├── java/com/beggar/api/
    │   │   ├── BeggarApplication.java          Spring Boot 진입점
    │   │   ├── common/                         공통 (응답 래퍼, 예외, JPA Auditing 베이스)
    │   │   ├── config/                         설정 (JPA, CORS, WebMvc, WebClient, 비밀번호 인코더)
    │   │   ├── security/                       JWT + @LoginUser (전부 비어있음 — 구현 예정)
    │   │   ├── entity/                         8개 JPA 엔티티 (테이블 매핑 완료)
    │   │   ├── repository/                     8개 JpaRepository (메서드 시그니처 완료)
    │   │   ├── service/                        7개 서비스 (전부 비어있음 — 구현 예정)
    │   │   ├── controller/                     7개 REST 컨트롤러 (전부 비어있음 — 구현 예정)
    │   │   └── dto/                            도메인별 요청/응답 DTO (record 기반)
    │   └── resources/
    │       └── application.properties          MySQL + JPA + JWT + 외부 API 키 (env 주입)
    └── test/
        └── java/com/beggar/api/
            └── BeggarApplicationTests.java     컨텍스트 로딩 테스트 1개
```

---

## 루트 파일

### `build.gradle`
Gradle Groovy DSL 빌드 스크립트. **의존성**:
- Spring Boot 3.3.5: `starter-web`(MVC), `starter-data-jpa`, `starter-thymeleaf`, `starter-webflux`(WebClient용), `starter-validation`, `starter-test`
- MySQL 드라이버 (`mysql-connector-j`)
- Lombok (compileOnly + annotationProcessor)
- 크롤링: `jsoup` 1.22.1 (정적), `selenium-java` 4.41.0, `webdrivermanager` 6.3.1 (동적/Chrome)
- XML: `jackson-dataformat-xml` 2.21.1
- 비밀번호: `spring-security-crypto` 6.4.4 (BCrypt만, Security 프레임워크 X)
- JWT: `jjwt-api/impl/jackson` 0.12.6

### `settings.gradle`
프로젝트 이름 `beggar-api` 지정.

### `sql/init.sql`
MySQL 초기화 스크립트. `CREATE DATABASE beggar` + 8개 테이블 DDL (`docs/DB_DESIGN.md`와 동일).
실행: `mysql -u root -p < sql/init.sql`

### `README.md`
프로젝트 소개, 기술 스택, 실행 방법, 인증 흐름, TODO 목록.

---

## src/main/java/com/beggar/api/

### `BeggarApplication.java`
Spring Boot 진입점. `@SpringBootApplication` + `main()`만 있는 표준 부트스트랩.

---

## common/ — 공통 유틸

### `common/BaseTimeEntity.java`
`@MappedSuperclass` + JPA Auditing. `created_at` / `updated_at` 컬럼을 자동 채워줌.
**상속하는 엔티티**: `User`, `Receipt` (시트의 두 컬럼을 모두 가진 테이블).
> 활성화하려면 `JpaConfig`에 `@EnableJpaAuditing`이 켜져 있어야 함 (이미 켜져 있음).

### `common/response/ApiResponse.java`
모든 API 응답 표준 래퍼. `{ success, data, code, message }` 구조.
정적 팩토리: `ApiResponse.success(data)`, `ApiResponse.success()`, `ApiResponse.error(code, message)`.
`@JsonInclude(NON_NULL)`로 null 필드는 응답에서 빠짐.

### `common/exception/ErrorCode.java`
도메인별 에러 코드 enum. HttpStatus + 코드(`AUTH_001` 등) + 메시지 페어.
새 에러 정의 시 enum 상수 추가만 하면 됨.

### `common/exception/CustomException.java`
비즈니스 예외. 생성자에 `ErrorCode` 전달.
사용 예: `throw new CustomException(ErrorCode.ROOM_NOT_FOUND);`

### `common/exception/GlobalExceptionHandler.java`
`@RestControllerAdvice`. 다음을 잡아 `ApiResponse.error()`로 변환:
- `CustomException` → 정의된 status + 코드
- `MethodArgumentNotValidException` (`@Valid` 검증 실패) → 400
- 그 외 `Exception` → 500 (`INTERNAL_ERROR`)

---

## config/ — Spring 설정

### `config/JpaConfig.java`
`@EnableJpaAuditing`. `BaseTimeEntity`의 `@CreatedDate` / `@LastModifiedDate`를 활성화.

### `config/CorsConfig.java`
`CorsFilter` Bean. 모든 Origin/Method/Header 허용 (개발용). 운영 시 도메인 화이트리스트로 좁혀야 함.

### `config/WebConfig.java` ⚠ 비어있음
`WebMvcConfigurer` 구현체. **TODO**:
1. `addInterceptors()`로 `JwtInterceptor` 등록 + `/auth/kakao`, `/auth/refresh`, `/error`, `/actuator/health` 제외
2. `addArgumentResolvers()`로 `LoginUserArgumentResolver` 등록

`JwtInterceptor` / `LoginUserArgumentResolver` 구현 후 생성자 주입으로 받아서 등록.

### `config/PasswordEncoderConfig.java`
`PasswordEncoder` Bean (`BCryptPasswordEncoder`). 로컬 비밀번호 해싱이 필요해지면 주입받아 사용.

### `config/WebClientConfig.java`
WebFlux의 `WebClient` 두 개:
- `kakaoWebClient`: `https://kapi.kakao.com` 베이스 (카카오 OAuth/Profile API)
- `aiServerWebClient`: `${ai-server.base-url}` 베이스 (Python AI 추천 서버)

`AuthService`, `RecommendationService`에서 주입받아 사용.

---

## security/ — JWT 인증 (전부 비어있음)

### `security/JwtTokenProvider.java` ⚠ 비어있음
`@Component`. **TODO**:
- `createAccessToken(Long userNo)` → 액세스 토큰 (subject = userNo)
- `createRefreshToken(Long userNo)` → 리프레시 토큰
- `parseUserNo(String token)` → 토큰에서 userNo 추출
- `isValid(String token)` → 서명 + 만료 검증
- 의존: `jjwt-api/impl/jackson` 0.12.6, `application.properties`의 `jwt.*` 키

### `security/JwtInterceptor.java` ⚠ 비어있음
`HandlerInterceptor` 구현. **TODO**:
- `preHandle()`에서 `Authorization: Bearer xxx` 헤더 파싱
- `JwtTokenProvider.isValid()` 검증, 실패 시 `CustomException(INVALID_TOKEN)`
- 성공 시 `request.setAttribute("userNo", userNo)` 저장 (상수 키 권장: `USER_NO_ATTR`)
- OPTIONS 메서드(CORS preflight)는 통과

### `security/LoginUser.java` ⚠ 비어있음
파라미터용 어노테이션. **TODO**:
- `@Target(ElementType.PARAMETER) @Retention(RetentionPolicy.RUNTIME)` 추가
- 컨트롤러에서 `public ApiResponse<?> foo(@LoginUser Long userNo)` 형태로 사용

### `security/LoginUserArgumentResolver.java` ⚠ 비어있음
`HandlerMethodArgumentResolver` 구현. **TODO**:
- `supportsParameter`: `@LoginUser` + `Long` 타입이면 true
- `resolveArgument`: `HttpServletRequest.getAttribute("userNo")` 반환
- `WebConfig.addArgumentResolvers()`에서 등록

---

## entity/ — JPA 엔티티 8개 (시트와 1:1)

각 엔티티는 `protected` 기본 생성자 + `@Builder` 패턴. setter 대신 의미있는 메서드(`updateXxx`, `leave`, `kick` 등)로 상태 변경.

### `entity/User.java`
`users` 테이블. `BaseTimeEntity` 상속 (created_at / updated_at 자동).
- 필드: `userNo`(PK), `userName`(UNIQUE), `passwordHash`(NULL), `profileImageUrl`(NULL), `email`(UNIQUE, 컬럼명 `uemail`), `role`(기본 `USER`)
- 메서드: `updateProfile(userName, profileImageUrl)`

### `entity/Room.java`
`rooms` 테이블.
- 필드: `roomNo`(PK), `owner`(User FK), `roomName`(UNIQUE), `roomCode`(UNIQUE), `totalBudget`(NULL), `roomCreated`(생성 시 자동), `isFriends`(방 공개 정책)
- `isFriends` 의미: `TRUE` = 친구 전용 (초대 코드/링크로만 입장) / `FALSE` = 익명방 (익명방 목록 노출, 코드 없이 입장 가능)
- 메서드: `updateTotalBudget(int)` — 예산 확정 시 호출

### `entity/RoomMember.java`
`room_members` 테이블. `(room_no, user_no)` 복합 UNIQUE.
- enum `Status`: `ACTIVE / LEFT / KICKED`
- 필드: `roomMemberId`(PK), `room`, `user`, `status`, `joinedAt`, `leftAt`(NULL)
- 메서드: `leave()`, `kick()` — 상태 + leftAt 동시 변경

### `entity/RoomPurposeTag.java`
`room_purpose_tags` 테이블. 방 단위 태그 (시트의 room_member_id FK는 room_no로 보정됨).
- 필드: `tagId`(PK), `room`, `tag`

### `entity/Budget.java`
`budgets` 테이블. `room_member_id` UNIQUE → 1인 1행.
- 필드: `budgetId`(PK), `roomMember`(@OneToOne), `budgetAmount`(INT, 시트 varchar→INT 보정), `submittedAt`
- 메서드: `updateAmount(int)` — 재제출 시 호출

### `entity/RoomBudgetResult.java`
`room_budget_results` 테이블. `room_no` UNIQUE → 방당 1결과.
- 필드: `resultId`(PK), `room`(@OneToOne), `minBudgetPerPerson`, `memberCount`, `totalBudget`, `confirmedAt`

### `entity/Receipt.java`
`receipts` 테이블. `BaseTimeEntity` 상속.
- enum `OcrStatus`: `PENDING / SUCCESS / FAILED / CANCELED`
- 필드: `receiptId`(PK), `room`, `uploader`(RoomMember FK), `imageUrl`, `ocrStatus`, `storeName`, `totalAmount`(OCR 원본), `amount`(사용자 보정값), `address`, `centerLat/centerLng`(DECIMAL 10,7 — 시트 datetime→DECIMAL 보정)
- 메서드: `applyOcrResult(...)`, `markOcrFailed()`, `updateAmount(int)`

### `entity/UserBeggarScore.java`
`user_beggar_scores` 테이블. user_no 공유 PK (`@MapsId`).
- 필드: `userNo`(PK/FK), `score`(0~100), `title`(칭호 캐시), `totalSavedAmount`, `budgetComplianceRate`(DECIMAL 5,2), `avgSavingsRatio`, `participationCount`, `lastCalculatedAt`, `updatedAt`
- 정적: `resolveTitle(score)` — 점수→칭호 매핑 (5단계)
- 인덱스: `idx_scores_score_desc` (랭킹 정렬용)

---

## repository/ — JpaRepository 8개

### `repository/UserRepository.java`
- `findByEmail(email)`, `existsByEmail(email)`, `existsByUserName(name)`

### `repository/RoomRepository.java`
- `findByRoomCode(code)` — 입장 코드로 조회
- `findActiveRoomsByUserNo(userNo)` — JPQL, ACTIVE 멤버인 방 목록 (정렬: roomCreated DESC)
- `findOpenRooms(Pageable)` — **익명방 둘러보기** (`is_friends = FALSE`) JPQL, 페이징 + 카테고리 필터용 (추가 예정)

### `repository/RoomMemberRepository.java`
- `findAllByRoom_RoomNo(roomNo)`, `findAllByRoom_RoomNoAndStatus(roomNo, status)`
- `findByRoom_RoomNoAndUser_UserNo(roomNo, userNo)` — 중복 입장 확인
- `countByRoom_RoomNoAndStatus(...)` — 자동 확정 조건 체크용
- `findAllByUser_UserNo(userNo)` — 거지력 재계산 시 사용

### `repository/RoomPurposeTagRepository.java`
- `findAllByRoom_RoomNo(roomNo)`, `deleteAllByRoom_RoomNo(roomNo)`

### `repository/BudgetRepository.java`
- `findByRoomMember_RoomMemberId(rmId)`
- `findAllByRoomNo(roomNo)` (JPQL)
- `countByRoomNo(roomNo)` (JPQL)
- `findMinBudgetByRoomNo(roomNo)` (JPQL, MIN 집계) — 예산 확정 산식

### `repository/RoomBudgetResultRepository.java`
- `findByRoom_RoomNo(roomNo)`, `existsByRoom_RoomNo(roomNo)`

### `repository/ReceiptRepository.java`
- `findAllByRoom_RoomNoOrderByCreatedAtDesc(roomNo)`
- `sumAmountByRoomNo(roomNo)` (JPQL, SUM) — 거지력 산식
- `findAllByUploaderUserNo(userNo)` (JPQL)

### `repository/UserBeggarScoreRepository.java`
- `findTopRanking(Pageable)` (JPQL + fetch join) — 랭킹 화면용

---

## service/ — 비즈니스 로직 7개 (전부 비어있음, 시그니처만 정의)

각 서비스는 `@Service @Transactional(readOnly = true)` 클래스 레벨로 시작. 쓰기 메서드는 메서드 레벨에 `@Transactional` 추가.

### `service/AuthService.java` ⚠ 비어있음
- `loginWithKakao(KakaoLoginRequest)` → `TokenResponse`
  - `kakaoWebClient`로 카카오 `/v2/user/me` 호출 → 이메일/닉네임 추출
  - `UserRepository.findByEmail` → 없으면 `users` + `user_beggar_scores`(0점/아기 거지) 같은 트랜잭션에 INSERT
  - `JwtTokenProvider.createAccessToken/Refresh` 발급
- `refresh(RefreshTokenRequest)` → `TokenResponse`
- `signOut(userNo)` — (필요 시) 블랙리스트
- `withdraw(userNo)` — hard delete (정책은 추후 결정)

### `service/UserService.java` ⚠ 비어있음
- `getMyProfile(userNo)` → `UserResponse`

### `service/RoomService.java` ⚠ 비어있음
- `create(ownerUserNo, CreateRoomRequest)` → `RoomResponse`
  - `Room` INSERT (`isFriends` 저장) → 방장을 `RoomMember(ACTIVE)`로 자동 INSERT → tags 일괄 INSERT
  - `roomCode`는 UUID 12자 (익명방도 동일하게 생성, URL 공유용)
- `findMyRooms(userNo)` → `List<RoomResponse>`
- `findById(roomNo)` → `RoomResponse`
- `findOpenRooms(Pageable)` → `List<RoomResponse>` — **익명방 둘러보기** (`is_friends = FALSE` 만)
- `joinByCode(userNo, code)` → `RoomResponse` (친구 전용 방용, 중복 입장 시 `ALREADY_JOINED`)
- `joinOpenRoom(userNo, roomNo)` → `RoomResponse` — **익명방 직접 입장** (코드 없이 입장, `is_friends = TRUE` 방은 `ROOM_NOT_OPEN` 에러)
- `findMembers(roomNo)` → `List<RoomMemberResponse>` — **금액 미노출, 제출 여부만**

### `service/BudgetService.java` ⚠ 비어있음
- `submit(userNo, roomNo, amount)` — INSERT or UPDATE
  - 제출 후: `countByRoomNo == countActiveMember` 이면 자동 확정(`confirmInternal`)
- `confirm(roomNo)` → `BudgetResultResponse` (수동 확정)
  - 모두 제출 안 했으면 `BUDGET_NOT_READY`
- `getResult(roomNo)` → `BudgetResultResponse`
- **내부**: `confirmInternal(roomNo)` — MIN × COUNT 계산 → `room_budget_results` INSERT + `rooms.total_budget` UPDATE + ACTIVE 멤버 전원 거지력 재계산 (모두 같은 트랜잭션)

### `service/ReceiptService.java` ⚠ 비어있음
- `create(userNo, ReceiptCreateRequest)` → `ReceiptResponse`
  - `ocrStatus = PENDING`, `amount = request.amount or 0` 으로 INSERT
  - **같은 트랜잭션 내 `BeggarScoreService.recalculate(userNo)` 호출**
- `updateAmount(userNo, receiptId, ReceiptUpdateRequest)` → 수동 보정 → 재계산
- `listByRoom(roomNo)` → `List<ReceiptResponse>` (최신순)
- `applyOcrResult(receiptId, ...)` — OCR 콜백 (Python에서 push)

### `service/RecommendationService.java` ⚠ 비어있음
- `recommend(roomNo)` → `RecommendationResponse`
  - `Room` 조회로 totalBudget + tags 확보
  - `aiServerWebClient.get()` → `/recommend?roomNo=...&budget=...&tags=...` 호출
  - 응답을 `RecommendationResponse.Place`로 매핑
  - **DB에 적재하지 않음** (채택률 트래킹 제외)

### `service/BeggarScoreService.java` ⚠ 비어있음
- 산식: `score = 예산준수율 × 0.40 + 평균절약률 × 0.40 + 참여빈도 × 0.20` (0~100)
- 칭호 5단계: 아기 거지 / 성장하는 거지 / 알뜰한 거지 / 프로 거지 / 전설의 거지
- `getMyScore(userNo)` → `BeggarScoreResponse`
- `getRanking(limit)` → `List<RankingEntryResponse>` (rank 1부터)
- `recalculate(userNo)` — 동기 + 동일 트랜잭션
  - 본인 멤버십 전체 순회 → 방별 totalBudget vs SUM(amount) 계산
  - `participationScore = min(count/10, 1) × 100`
  - UPSERT (없으면 INSERT, 있으면 update)

---

## controller/ — REST 컨트롤러 7개 (전부 비어있음, 경로만 정의)

각 컨트롤러는 `@RestController` + `@RequestMapping("/xxx")` 클래스 레벨. 메서드는 비어있고 주석으로 엔드포인트만 명시.

### `controller/AuthController.java` ⚠ 비어있음 — `/auth/**`
- `POST /auth/kakao` — `KakaoLoginRequest` → `TokenResponse` (공개)
- `POST /auth/refresh` — `RefreshTokenRequest` → `TokenResponse` (공개)
- `POST /auth/signout` — `@LoginUser Long userNo` → 204

### `controller/UserController.java` ⚠ 비어있음 — `/users/**`
- `GET /users/me` → `UserResponse`
- `GET /users/me/beggar-score` → `BeggarScoreResponse`
- `DELETE /users/me` → 204

### `controller/RoomController.java` ⚠ 비어있음 — `/rooms/**`
- `GET /rooms/my` → `List<RoomResponse>`
- `POST /rooms` (`CreateRoomRequest`) → `RoomResponse`
- `GET /rooms/{roomNo}` → `RoomResponse`
- `GET /rooms/open?limit=20&category=...` → `List<RoomResponse>` — **익명방 둘러보기**
- `POST /rooms/join` (`JoinRoomRequest`) → `RoomResponse` — 친구 전용 방 (코드)
- `POST /rooms/{roomNo}/join` → `RoomResponse` — **익명방 직접 입장 (코드 불필요)**
- `GET /rooms/{roomNo}/members` → `List<RoomMemberResponse>`

### `controller/BudgetController.java` ⚠ 비어있음 — `/rooms/{roomNo}/budget`
- `POST /rooms/{roomNo}/budget` (`SubmitBudgetRequest`) → 204
- `POST /rooms/{roomNo}/budget/confirm` → `BudgetResultResponse`
- `GET /rooms/{roomNo}/budget/result` → `BudgetResultResponse`

### `controller/ReceiptController.java` ⚠ 비어있음 — `/receipts/**`
- `POST /receipts` (`ReceiptCreateRequest`) → `ReceiptResponse`
- `PATCH /receipts/{receiptId}` (`ReceiptUpdateRequest`) → `ReceiptResponse`
- `GET /receipts?roomNo=...` → `List<ReceiptResponse>`

### `controller/RecommendationController.java` ⚠ 비어있음 — `/rooms/{roomNo}/recommend`
- `GET /rooms/{roomNo}/recommend` → `RecommendationResponse`

### `controller/RankingController.java` ⚠ 비어있음 — `/ranking`
- `GET /ranking?limit=15` → `List<RankingEntryResponse>`

---

## dto/ — 요청/응답 DTO (모두 Java `record`)

### dto/auth/
- `KakaoLoginRequest(kakaoAccessToken)` — 카카오 OAuth 토큰 받기
- `RefreshTokenRequest(refreshToken)`
- `TokenResponse(accessToken, refreshToken, userNo, userName)`

### dto/user/
- `UserResponse(userNo, userName, email, profileImageUrl, role)` + `from(User)` 정적 메서드

### dto/room/
- `CreateRoomRequest(roomName, isFriends, tags)`
- `JoinRoomRequest(roomCode)`
- `RoomResponse(roomNo, roomName, roomCode, ownerUserNo, totalBudget, isFriends, roomCreated, tags)` + `of(Room, tags)`
- `RoomMemberResponse(roomMemberId, userNo, userName, status, budgetSubmitted)` — **금액 X, 제출 여부만**

### dto/budget/
- `SubmitBudgetRequest(budgetAmount)` — `@NotNull @Min(0)`
- `BudgetResultResponse(roomNo, minBudgetPerPerson, memberCount, totalBudget, confirmedAt)` + `from(RoomBudgetResult)`

### dto/receipt/
- `ReceiptCreateRequest(roomNo, imageUrl, amount)`
- `ReceiptUpdateRequest(amount)`
- `ReceiptResponse(...)` — 모든 컬럼 (uploaderUserNo는 `RoomMember.user.userNo`로 평탄화)

### dto/recommendation/
- `RecommendationResponse(roomNo, totalBudget, places)` + 내부 `record Place(...)`

### dto/ranking/
- `RankingEntryResponse(rank, userNo, userName, score, title)` + `of(rank, UserBeggarScore)`
- `BeggarScoreResponse(...)` — 마이페이지용, 산식 구성요소까지 포함

---

## resources/

### `application.properties`
- DataSource (MySQL): `DB_HOST/PORT/NAME/USERNAME/PASSWORD` env 주입, 기본 `localhost:3306/beggar/root/root`
- JPA: `ddl-auto=update`, `show-sql=true`, MySQLDialect
- JWT: `JWT_SECRET` env 주입 (256bit 이상), 액세스 1h / 리프레시 14d
- Kakao: `KAKAO_REST_API_KEY`
- AI 서버: `AI_SERVER_BASE_URL` (기본 `http://localhost:8000`)
- 로깅: `com.beggar.api` DEBUG, Hibernate SQL DEBUG

> `application-prod.properties`, `application-secret.properties` 패턴은 `.gitignore`로 차단.

---

## src/test/

### `test/java/com/beggar/api/BeggarApplicationTests.java`
`@SpringBootTest` 컨텍스트 로딩 테스트 1개. MySQL 연결이 필요하므로 로컬 MySQL이 떠있어야 통과.
> 테스트용 별도 스키마(`beggar_test`)를 쓰려면 `src/test/resources/application-test.properties` 추가하고 `@ActiveProfiles("test")` 붙이는 방식 권장.

---

## 구현 순서 권장

1. **`security/JwtTokenProvider`** → JWT 발급/검증 동작 확인 (단위 테스트로)
2. **`security/JwtInterceptor` + `LoginUser` + `LoginUserArgumentResolver`** → `WebConfig`에서 등록
3. **`AuthService` + `AuthController.kakaoLogin`** → 카카오 로그인 동작 확인 (Postman)
4. **`UserService` + `UserController.getMe`** → 토큰 → userNo 추출 흐름 검증
5. **`RoomService` + `RoomController`** 전체 — 프론트 홈/방 생성/초대 화면 연결
6. **`BudgetService` + `BudgetController`** — 프론트 예산 입력/결과 화면 연결
7. **`ReceiptService` + `ReceiptController`** — 프론트 영수증/지출 화면 연결 (OCR 콜백은 마지막)
8. **`BeggarScoreService`** — 영수증/예산 트리거에서 호출 + 랭킹 API
9. **`RecommendationService`** — Python AI 서버 연동 (마지막)

각 단계마다 `application.properties`의 DB 연결 + JWT 시크릿이 정상인지 먼저 확인하면 빠름.
