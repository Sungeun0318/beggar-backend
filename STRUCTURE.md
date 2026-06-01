# 백엔드 파일 구조 & 기능 설명

> `backend/` 디렉터리의 모든 파일이 어떤 역할인지, 어떤 내용을 담아야 하는지 정리.
> **2026-05-30 기준**: DB/JPA 골격은 커뮤니티, 통합/분할 영수증, 방별 거지평가 기준으로 최신화됨. 서비스 / 컨트롤러 / 시큐리티 본체 코드는 팀이 직접 구현 예정.
> 본 문서가 아직 구현되지 않은 서비스/컨트롤러 메서드의 "구현 계약서" 역할을 함.

---

## 전체 구조

```
backend/
├── build.gradle                Gradle 의존성 + 빌드 설정
├── settings.gradle             프로젝트 이름
├── sql/
│   └── init.sql                MySQL 10개 테이블 DDL (한 번에 실행)
├── README.md                   실행 방법 + 기술 스택 요약
├── STRUCTURE.md                (본 문서) 파일별 상세 설명
└── src/
    ├── main/
    │   ├── java/com/beggar/api/
    │   │   ├── BeggarApplication.java          Spring Boot 진입점
    │   │   ├── common/                         공통 (응답 래퍼, 예외, JPA Auditing 베이스)
    │   │   ├── config/                         설정 (JPA, CORS, WebMvc, WebClient, 비밀번호 인코더)
    │   │   ├── security/                       JWT + @LoginUser (구현 예정)
    │   │   ├── entity/                         10개 테이블 기준 JPA 엔티티
    │   │   ├── repository/                     JpaRepository 메서드 골격
    │   │   ├── service/                        서비스 골격 (구현 예정)
    │   │   ├── controller/                     REST 컨트롤러 골격 (구현 예정)
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
MySQL 초기화 스크립트. `CREATE DATABASE beggar` + 10개 테이블 DDL (`docs/DB_DESIGN.md`와 동일).
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
**상속하는 엔티티**: `User`, `Receipt`, `ReceiptSplit`, `CommunityPost`.
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

### `config/WebConfig.java` ⚠ 구현 예정
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

## security/ — JWT 인증 (구현 예정)

### `security/JwtTokenProvider.java` ⚠ 구현 예정
`@Component`. **TODO**:
- `createAccessToken(Long userNo)` → 액세스 토큰 (subject = userNo)
- `createRefreshToken(Long userNo)` → 리프레시 토큰
- `parseUserNo(String token)` → 토큰에서 userNo 추출
- `isValid(String token)` → 서명 + 만료 검증
- 의존: `jjwt-api/impl/jackson` 0.12.6, `application.properties`의 `jwt.*` 키

### `security/JwtInterceptor.java` ⚠ 구현 예정
`HandlerInterceptor` 구현. **TODO**:
- `preHandle()`에서 `Authorization: Bearer xxx` 헤더 파싱
- `JwtTokenProvider.isValid()` 검증, 실패 시 `CustomException(INVALID_TOKEN)`
- 성공 시 `request.setAttribute("userNo", userNo)` 저장 (상수 키 권장: `USER_NO_ATTR`)
- OPTIONS 메서드(CORS preflight)는 통과

### `security/LoginUser.java` ⚠ 구현 예정
파라미터용 어노테이션. **TODO**:
- `@Target(ElementType.PARAMETER) @Retention(RetentionPolicy.RUNTIME)` 추가
- 컨트롤러에서 `public ApiResponse<?> foo(@LoginUser Long userNo)` 형태로 사용

### `security/LoginUserArgumentResolver.java` ⚠ 구현 예정
`HandlerMethodArgumentResolver` 구현. **TODO**:
- `supportsParameter`: `@LoginUser` + `Long` 타입이면 true
- `resolveArgument`: `HttpServletRequest.getAttribute("userNo")` 반환
- `WebConfig.addArgumentResolvers()`에서 등록

---

## entity/ — JPA 엔티티

현재 Java 엔티티는 `docs/DB_DESIGN.md`의 10개 테이블 기준으로 맞춰져 있다. 서비스 본체 구현은 별도 단계에서 진행한다.

각 엔티티는 `protected` 기본 생성자 + `@Builder` 패턴. setter 대신 의미있는 메서드(`updateXxx`, `leave`, `kick` 등)로 상태 변경.

### `entity/User.java`
`users` 테이블. `BaseTimeEntity` 상속 (created_at / updated_at 자동).
- 필드 목표: `userNo`(PK), `userName`(UNIQUE), `passwordHash`(NULL), `profileImageUrl`(NULL), `email`(UNIQUE, 컬럼명 `uemail`), `gender`(NULL), `ageRange`(NULL), `role`(기본 `USER`)
- 메서드: `updateProfile(userName, profileImageUrl)`

### `entity/Room.java`
`rooms` 테이블.
- 필드: `roomNo`(PK), `owner`(User FK), `roomName`(UNIQUE), `roomCode`(UNIQUE), `maxMemberCount`(기본 100), `totalBudget`(NULL), `roomCreated`(생성 시 자동), `isFriends`
- `isFriends` 의미: 현재 MVP는 친구 초대 기반 방 중심. `FALSE`는 공개 방 후보로 남겨두지만 커뮤니티와 분리한다.
- `maxMemberCount`: 반장이 거지방 설정에서 수정 가능한 최대 인원. 최소 2~최대 100 정책.
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
- enum `ReceiptType`: `COMBINED / SPLIT`
- enum `InputMethod`: `CAMERA / GALLERY / MANUAL`
- enum `OcrStatus`: `PENDING / SUCCESS / FAILED / CANCELED / MANUAL`
- 필드 목표: `receiptId`(PK), `room`, `uploader`(RoomMember FK), `receiptType`, `inputMethod`, `imageUrl`, `ocrStatus`, `storeName`, `totalAmount`(OCR 원본), `amount`(사용자 보정값), `address`, `centerLat/centerLng`, 착한가격업소 매칭 컬럼
- 메서드: `applyOcrResult(...)`, `markOcrFailed()`, `updateAmount(int)`

### `entity/ReceiptSplit.java`
`receipt_splits` 테이블. 분할 영수증의 멤버별 금액.
- 필드: `splitId`, `receipt`, `roomMember`, `amount`, `createdAt`, `updatedAt`

### `entity/RoomBeggarScore.java`
`room_beggar_scores` 테이블. `room_no` 기준 UNIQUE.
- 필드: `scoreId`, `room`, `score`(0~100), `title`(칭호 캐시), `totalSpentAmount`, `totalSavedAmount`, `goodPriceVerifiedCount`, `budgetComplianceRate`, `avgSavingsRatio`, `lastCalculatedAt`, `updatedAt`
- 정적: `resolveTitle(score)` — 점수→칭호 매핑 (5단계)
- 인덱스: `idx_room_scores_score_desc` (전체 방 점수 정렬 후보)

### `entity/CommunityPost.java`
`community_posts` 테이블. 커뮤니티 게시글.
- 필드: `postId`, `user`, `title`, `content`, `category`, `createdAt`, `updatedAt`

---

## repository/ — JpaRepository

### `repository/UserRepository.java`
- `findByEmail(email)`, `existsByEmail(email)`, `existsByUserName(name)`

### `repository/RoomRepository.java`
- `findByRoomCode(code)` — 입장 코드로 조회
- `existsByRoomCode(code)` — 초대 코드 중복 확인
- `findActiveRoomsByUserNo(userNo)` — JPQL, ACTIVE 멤버인 방 목록 (정렬: roomCreated DESC)
- 공개 방 목록 API는 현재 MVP에서 제외한다. 커뮤니티는 `community_posts` 계열로 분리한다.

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
- `countGoodPriceMatchedByRoomNo(roomNo)` — 착한가격업소 인증 성공 수
- `findAllByUploaderUserNo(userNo)` (JPQL)

### `repository/ReceiptSplitRepository.java`
- `findAllByReceipt_ReceiptId(receiptId)`
- `findAllByRoomMember_RoomMemberId(roomMemberId)`

### `repository/RoomBeggarScoreRepository.java`
- `findByRoom_RoomNo(roomNo)` — 방별 거지평가 화면용
- `existsByRoom_RoomNo(roomNo)`
- `findTopRoomScores(pageable)` — 방 점수 정렬 후보

### `repository/CommunityPostRepository.java`
- `findAllByOrderByCreatedAtDesc()`
- `findAllByCategoryOrderByCreatedAtDesc(category)`

---

## service/ — 비즈니스 로직 골격

각 서비스는 `@Service @Transactional(readOnly = true)` 클래스 레벨로 시작. 쓰기 메서드는 메서드 레벨에 `@Transactional` 추가.

### `service/AuthService.java` ⚠ 구현 예정
- `loginWithKakao(KakaoLoginRequest)` → `TokenResponse`
	  - `kakaoWebClient`로 카카오 `/v2/user/me` 호출 → 이메일/닉네임 추출
	  - `UserRepository.findByEmail` → 없으면 `users` INSERT
	  - 카카오 식별은 `uemail` 기준. 별도 소셜 ID/인증 제공자 컬럼이나 가입 시점 점수 행은 만들지 않음
  - `JwtTokenProvider.createAccessToken/Refresh` 발급
- `refresh(RefreshTokenRequest)` → `TokenResponse`
- `signOut(userNo)` — (필요 시) 블랙리스트
- `withdraw(userNo)` — hard delete (정책은 추후 결정)

### `service/UserService.java` ⚠ 구현 예정
- `getMyProfile(userNo)` → `UserResponse`

### `service/RoomService.java` ⚠ 구현 예정
- `create(ownerUserNo, CreateRoomRequest)` → `RoomResponse`
	  - `Room(maxMemberCount 포함)` INSERT → 방장을 `RoomMember(ACTIVE)`로 자동 INSERT → tags 일괄 INSERT
	  - `roomCode`는 UUID 12자
- `findMyRooms(userNo)` → `List<RoomResponse>`
- `findById(roomNo)` → `RoomResponse`
- `joinByCode(userNo, code)` → `RoomResponse` (친구 전용 방용, 중복 입장 시 `ALREADY_JOINED`)
- `findMembers(roomNo)` → `List<RoomMemberResponse>` — **금액 미노출, 제출 여부만**
- `updateSettings(roomNo, ownerUserNo, request)` — 반장 전용 지역/태그/최대 인원 변경

### `service/BudgetService.java` ⚠ 구현 예정
- `submit(userNo, roomNo, amount)` — INSERT or UPDATE
  - 제출 후: `countByRoomNo == countActiveMember` 이면 자동 확정(`confirmInternal`)
- `confirm(roomNo)` → `BudgetResultResponse` (수동 확정)
  - 모두 제출 안 했으면 `BUDGET_NOT_READY`
- `getResult(roomNo)` → `BudgetResultResponse`
- **내부**: `confirmInternal(roomNo)` — MIN × COUNT 계산 → `room_budget_results` INSERT + `rooms.total_budget` UPDATE + 해당 방 `room_beggar_scores` 재계산 (모두 같은 트랜잭션)

### `service/ReceiptService.java` ✅ 1차 구현
- `create(roomNo, userNo, ReceiptCreateRequest)` → `ReceiptResponse`
  - `receiptType`, `inputMethod`, `ocrStatus`, `amount` 저장
  - 통합은 `receipts`만, 분할은 `receipt_splits`까지 저장
  - `storeName/address` 기준 착한가격업소 인증 매칭 후 `good_price_*` 저장
  - **후속**: 같은 트랜잭션 내 `BeggarScoreService.recalculate(roomNo)` 호출
- `updateAmount(roomNo, userNo, receiptId, ReceiptUpdateRequest)` → 수동 보정 → 재계산
- `listByRoom(roomNo)` → `List<ReceiptResponse>` (최신순)
- `applyOcrResult(receiptId, ...)` — OCR 콜백/수동 반영 후 착한가격업소 재매칭

### `service/GoodPriceMatchService.java` ✅ 1차 구현
- 영수증 `storeName/address`를 행정안전부 착한가격업소 OpenAPI 결과와 비교
- 매칭 성공 기준:
  - 상호명이 정확히 같으면 인증 후보로 인정
  - 상호명이 부분 일치하면 주소 일치 점수를 더해 임계값 이상일 때 인증
- 외부 API 실패 시 영수증 저장은 막지 않고 미인증으로 처리

### `service/RecommendationService.java` ✅ 1차 구현
- `recommend(roomNo, tag, region, lat, lng, radius)` → `RecommendationResponse`
  - `Room` 조회로 totalBudget 확보
  - `ReceiptRepository.sumAmountByRoomNo(roomNo)`로 사용 금액/남은 예산 계산
  - 활성 멤버 수로 1인 남은 예산을 계산한 뒤 태그별 추천 예산 산출
  - 결과 부족 시 1인 남은 예산 전체 → 가격 조건 제거 → 지역/거리 조건 완화 순서로 fallback
  - `GoodPriceStoreClient`로 행정안전부 착한가격업소 OpenAPI 호출
  - 지역 문자열은 주소 조건 검색으로 후보를 줄이고, `lat/lng`가 있으면 카카오 지오코딩 좌표로 거리 계산
  - 카카오 Local API 키워드/주소/좌표 변환은 응답 지연 방지를 위해 2초 타임아웃 적용
  - 태그는 한식/양식/일식/중식/기타요식업 및 식사/카페/놀거리 매칭 규칙으로 필터링
  - 대표 메뉴명(`menuName`), 메뉴 가격(`expectedPrice`), 카테고리 기본 이미지 경로(`thumbnailUrl`), 카카오맵 검색 링크(`mapUrl`)를 함께 반환
  - `mapUrl` 검색어는 `업소명 + 짧은 지역` 형식으로 생성해 주소 검색보다 장소명 검색을 우선한다.
  - **DB에 적재하지 않음** (거지력 점수는 추천 이력이 아니라 영수증 착한가격업소 인증 기준)

### `service/BeggarScoreService.java` ⚠ 구현 예정
- 산식 후보: `score = 예산준수율 35% + 절약률 35% + 착한가격업소 인증 점수 30%` (0~100)
- 칭호 5단계: 아기 거지 / 성장하는 거지 / 알뜰한 거지 / 프로 거지 / 전설의 거지
- `getRoomScore(roomNo)` → `BeggarScoreResponse`
- `recalculate(roomNo)` — 동기 + 동일 트랜잭션
  - 해당 방 totalBudget vs 방 전체 지출/SUM(amount) 계산
  - 착한가격업소 매칭 성공 수 반영
  - UPSERT (없으면 INSERT, 있으면 update)

### `service/CommunityService.java` ⚠ 구현 예정
- `findPosts(category)` — 커뮤니티 게시글 최신순 조회
- `createPost(userNo, request)` — 게시글 작성
- `findGlobalChatRoom()` / `sendChatMessage(...)` — 전체 채팅 후보 API. 저장 테이블은 추후 확정

---

## controller/ — REST 컨트롤러 골격

각 컨트롤러는 `@RestController` + `@RequestMapping("/xxx")` 클래스 레벨. 메서드는 비어있고 주석으로 엔드포인트만 명시.

### `controller/AuthController.java` ⚠ 구현 예정 — `/auth/**`
- `POST /auth/kakao` — `KakaoLoginRequest` → `TokenResponse` (공개)
- `POST /auth/refresh` — `RefreshTokenRequest` → `TokenResponse` (공개)
- `POST /auth/signout` — `@LoginUser Long userNo` → 204

### `controller/UserController.java` ⚠ 구현 예정 — `/users/**`
- `GET /users/me` → `UserResponse`
- `DELETE /users/me` → 204

### `controller/RoomController.java` ⚠ 구현 예정 — `/rooms/**`
- `GET /rooms/my` → `List<RoomResponse>`
- `POST /rooms` (`CreateRoomRequest`) → `RoomResponse`
- `GET /rooms/{roomNo}` → `RoomResponse`
- `PATCH /rooms/{roomNo}/settings` — 반장 전용 지역/태그/최대 인원 변경
- `POST /rooms/join` (`JoinRoomRequest`) → `RoomResponse` — 친구 전용 방 (코드)
- `GET /rooms/{roomNo}/members` → `List<RoomMemberResponse>`
- `GET /rooms/{roomNo}/beggar-score` → `BeggarScoreResponse`

### `controller/BudgetController.java` ⚠ 구현 예정 — `/rooms/{roomNo}/budget`
- `POST /rooms/{roomNo}/budget` (`SubmitBudgetRequest`) → 204
- `POST /rooms/{roomNo}/budget/confirm` → `BudgetResultResponse`
- `GET /rooms/{roomNo}/budget/result` → `BudgetResultResponse`

### `controller/ReceiptController.java` ⚠ 구현 예정 — `/rooms/{roomNo}/receipts/**`
- `POST /rooms/{roomNo}/receipts` (`ReceiptCreateRequest`) → `ReceiptResponse`
- `PATCH /rooms/{roomNo}/receipts/{receiptId}` (`ReceiptUpdateRequest`) → `ReceiptResponse`
- `GET /rooms/{roomNo}/receipts` → `List<ReceiptResponse>`

### `controller/RecommendationController.java` ✅ 1차 구현 — `/rooms/{roomNo}/recommend`
- `GET /rooms/{roomNo}/recommend?tag={tag}&region={region}&lat={lat}&lng={lng}&radius={radius}` → `RecommendationResponse`

### `controller/CommunityController.java` ⚠ 구현 예정 — `/community/**`
- `GET /community/posts` → 게시글 목록
- `POST /community/posts` → 게시글 작성
- `GET /community/chat-room` → 전체 채팅방 조회 후보
- `POST /community/messages` → 전체 채팅 메시지 전송 후보

---

## dto/ — 요청/응답 DTO (모두 Java `record`)

### dto/auth/
- `KakaoLoginRequest(kakaoAccessToken)` — 카카오 OAuth 토큰 받기
- `RefreshTokenRequest(refreshToken)`
- `TokenResponse(accessToken, refreshToken, userNo, userName)`

### dto/user/
- `UserResponse(userNo, userName, email, profileImageUrl, gender, ageRange, role)` + `from(User)` 정적 메서드

### dto/room/
- `CreateRoomRequest(roomName, tags, maxMemberCount, isFriends)`
- `JoinRoomRequest(roomCode)`
- `RoomResponse(roomNo, roomName, roomCode, ownerUserNo, maxMemberCount, totalBudget, isFriends, roomCreated, tags)` + `of(Room, tags)`
- `RoomMemberResponse(roomMemberId, userNo, userName, status, budgetSubmitted)` — **금액 X, 제출 여부만**

### dto/budget/
- `SubmitBudgetRequest(budgetAmount)` — `@NotNull @Min(0)`
- `BudgetResultResponse(roomNo, minBudgetPerPerson, memberCount, totalBudget, confirmedAt)` + `from(RoomBudgetResult)`

### dto/receipt/
- `ReceiptCreateRequest(roomNo, receiptType, inputMethod, imageUrl, storeName, address, amount, splits)`
- `ReceiptUpdateRequest(amount)`
- `ReceiptResponse(...)` — 모든 컬럼 (uploaderUserNo는 `RoomMember.user.userNo`로 평탄화)

### dto/recommendation/
- `RecommendationResponse(roomNo, totalBudget, spentAmount, remainingBudget, recommendationBudget, budgetGuide, fallbackApplied, requestedTag, requestedRegion, places)` + 내부 `record Place(...)`
- `Place`: `storeId`, `name`, `category`, `expectedPrice`, `menuName`, `walkTime`, `rating`, `thumbnailUrl`, `address`, `mapUrl`, `lat`, `lng`, `source`, `reason`

### dto/ranking/
- `RankingEntryResponse(rank, roomNo, roomName, score, title)` + `of(rank, RoomBeggarScore)`
- `BeggarScoreResponse(...)` — 방별 거지평가용, 산식 구성요소까지 포함

---

## resources/

### `application.properties`
- DataSource (MySQL): `DB_HOST/PORT/NAME` env 주입, 기본 `localhost:3306/beggar`, 계정 `root/1208`
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
8. **`BeggarScoreService`** — 영수증/예산 트리거에서 호출 + 방별 평가 API
9. **`CommunityService` + `CommunityController`** — 게시글 API
10. **`RecommendationService`** — Spring 착한가격업소 추천 유지, Python은 후순위 AI Hub 고도화

각 단계마다 `application.properties`의 DB 연결 + JWT 시크릿이 정상인지 먼저 확인하면 빠름.
