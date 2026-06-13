# beggar-backend

거지 우정 수호대 Spring Boot 백엔드다.

## 현재 상태

- Spring Boot 3.3.5 + Java 17 target + Gradle.
- MySQL + JPA 기반.
- JWT 인증은 Spring Security 필터 체인이 아니라 `HandlerInterceptor` + `@LoginUser` argument resolver로 처리한다.
- 사용자 API, WebSocket, 추천, 영수증, 커뮤니티, 랭킹, 방별 거지력 점수가 구현되어 있다.
- `origin/yeonji` 머지로 관리자 컨트롤러/서비스/DTO가 `backend` 안에 통합됐다.
- 최근 머지 충돌은 `BudgetRepository`, `UserRepository`에서 양쪽 변경을 모두 살려 해결했다.
- `./gradlew compileJava` 통과 확인.

## 실행

```bash
cd backend
./gradlew bootRun
```

기본 주소:

```text
http://localhost:8080
```

## 주요 기술

- Spring Boot Web MVC
- Spring Data JPA
- MySQL
- WebFlux WebClient
- STOMP WebSocket + SockJS
- JWT (`io.jsonwebtoken`)
- AWS S3 presigned URL
- Google Cloud Vision dependency
- Apache POI 엑셀 다운로드
- Lombok

## 패키지 구조

```text
src/main/java/com/beggar/api/
├── client/
│   ├── goodprice/             착한가격업소 API client
│   └── kakao/                 Kakao Local API client
├── common/                    ApiResponse, 예외, BaseTimeEntity
├── config/                    CORS, WebConfig, WebSocket, S3, WebClient 등
├── controller/                사용자 REST API + 관리자 컨트롤러 + STOMP controller
│   └── admin/                 관리자 기능
├── dto/                       사용자/관리자 API DTO
│   └── admin/                 관리자 화면 전용 DTO
├── entity/                    JPA 엔티티
├── repository/                사용자 도메인 repository
│   └── admin/                 관리자 계정/로그 repository
├── security/                  JWT interceptor, provider, @LoginUser
└── service/                   도메인 서비스
    └── admin/                 관리자 서비스
```

## 인증

| Method | Path | 설명 |
|---|---|---|
| `POST` | `/auth/login` | 이메일 로그인 |
| `POST` | `/auth/kakao` | 카카오 access token 로그인 |
| `POST` | `/auth/kakao/code` | 카카오 authorization code 로그인 |
| `POST` | `/auth/refresh` | access token 갱신 |
| `POST` | `/auth/signout` | 로그아웃 |
| `DELETE` | `/auth/withdraw` | 회원 탈퇴 |

인증 제외 주요 경로:

- `/auth/login`
- `/auth/kakao`
- `/auth/kakao/code`
- `/auth/refresh`
- `/users/signup`
- `/locations/search`
- `/admin/**`
- `/actuator/health`

## 사용자

| Method | Path | 설명 |
|---|---|---|
| `POST` | `/users/signup` | 회원가입 |
| `GET` | `/users/me` | 내 프로필 |
| `PATCH` | `/users/me` | 닉네임/프로필 수정 |
| `GET` | `/users/me/receipts` | 내 영수증 히스토리 |
| `DELETE` | `/users/me` | 회원 탈퇴 |
| `GET` | `/users/me/presigned-url?fileName=` | 프로필 업로드 URL |

## 방

| Method | Path | 설명 |
|---|---|---|
| `POST` | `/rooms` | 방 생성 |
| `GET` | `/rooms/my` | 내 방 목록 |
| `GET` | `/rooms/{roomNo}` | 방 상세 |
| `GET` | `/rooms/{roomNo}/members` | 멤버 목록 + 예산 제출 여부 |
| `POST` | `/rooms/join` | 초대 코드 입장 |
| `POST` | `/rooms/{roomNo}/budget/start` | 예산 입력 시작 |
| `PATCH` | `/rooms/{roomNo}/settings` | 방 설정 수정 |
| `POST` | `/rooms/{roomNo}/close` | 방 종료 |
| `GET` | `/rooms/{roomNo}/beggar-score` | 방별 거지력 점수 |

방 상태:

```text
INVITING -> BUDGET_INPUT -> BUDGET_DONE -> ACTIVE / ENDED / DELETED
```

## 예산

| Method | Path | 설명 |
|---|---|---|
| `GET` | `/rooms/{roomNo}/budget` | 본인 예산 조회 |
| `POST` | `/rooms/{roomNo}/budget` | 본인 예산 제출/수정 |
| `POST` | `/rooms/{roomNo}/budget/confirm` | 예산 확정 |
| `GET` | `/rooms/{roomNo}/budget/result` | 확정 결과 |
| `GET` | `/rooms/{roomNo}/budget/excel` | 엑셀 다운로드 |

익명성 규칙:

- 멤버 목록은 `budgetSubmitted`만 반환한다.
- 다른 사람의 예산 금액은 API 응답/웹소켓 이벤트에 넣지 않는다.
- 확정 계산은 ACTIVE 멤버의 제출 예산만 대상으로 한다.
- 전원 제출 시 자동으로 `confirmBudget`을 호출한다.

## WebSocket

endpoint:

```text
/ws-stomp
```

브로커:

```text
subscribe: /sub, /topic
publish:   /pub
```

방 이벤트:

| Topic | 설명 |
|---|---|
| `/topic/rooms/{roomNo}` | 멤버 변경, 예산 제출, 예산 확정, 방 종료 |
| `/sub/chats` | 전체 채팅 |

채팅 발행:

```text
/pub/chats
```

## 추천/위치

| Method | Path | 설명 |
|---|---|---|
| `GET` | `/rooms/{roomNo}/recommend` | 착한가격업소 기반 추천 |
| `GET` | `/locations/search?query=` | Kakao Local 검색 |

추천 query:

- `tag`
- `region`
- `lat`
- `lng`
- `radius`, 기본 2000m

Spring 백엔드가 착한가격업소 API와 Kakao Local API를 직접 호출한다. Python AI 서버는 현재 추천 경로에 끼지 않는다.

## 영수증

| Method | Path | 설명 |
|---|---|---|
| `POST` | `/rooms/{roomNo}/receipts/upload-url` | S3 presigned URL |
| `POST` | `/rooms/{roomNo}/receipts` | 영수증 생성 |
| `GET` | `/rooms/{roomNo}/receipts` | 방 영수증 목록 |
| `GET` | `/rooms/{roomNo}/receipts/{receiptId}` | 영수증 상세 |
| `PATCH` | `/rooms/{roomNo}/receipts/{receiptId}` | 수동 보정 |
| `PUT` | `/rooms/{roomNo}/receipts/{receiptId}/ocr` | OCR 결과 반영(수동/외부 보정용) |
| `DELETE` | `/rooms/{roomNo}/receipts/{receiptId}` | 삭제 |

OCR은 백엔드가 자체 처리한다. 영수증 생성(비수동 입력) 시 `ReceiptService.processOcrAsync`가 S3 이미지를 Google Vision + Groq로 분석해 결과를 DB에 직접 반영한다. Python AI 서버는 OCR에 관여하지 않는다.

## 커뮤니티

| Method | Path | 설명 |
|---|---|---|
| `GET` | `/community/posts` | 게시글 목록/검색 |
| `GET` | `/community/posts/popular` | 인기글 |
| `GET` | `/community/posts/{postId}` | 상세 |
| `POST` | `/community/posts` | 작성 |
| `DELETE` | `/community/posts/{postId}` | 삭제 |
| `POST` | `/community/posts/{postId}/comments` | 댓글 작성 |
| `GET` | `/community/chats` | 채팅 내역 |
| `POST` | `/community/chats` | HTTP 채팅 전송 |

## 랭킹

| Method | Path | 설명 |
|---|---|---|
| `GET` | `/ranking?limit=15` | 방별 거지력 점수 랭킹 |

## 관리자 통합

`backend` 안에 관리자 컨트롤러가 추가됐다.

```text
controller/admin/
dto/admin/
service/admin/
repository/admin/
entity/AdminAccount.java
entity/AdminActionLog.java
```

관리자 주요 경로:

| Path | 기능 |
|---|---|
| `/admin` | 대시보드 |
| `/admin/users` | 회원 목록 |
| `/admin/users/{userNo}` | 회원 상세 |
| `/admin/rooms` | 방 목록 |
| `/admin/rooms/{roomNo}` | 방 상세 |
| `/admin/community/posts` | 게시글 관리 |
| `/admin/community/comments` | 댓글 관리 |
| `/admin/chats` | 채팅 관리 |
| `/admin/receipts` | 영수증 관리 |
| `/admin/logs` | 운영 로그 |

주의:

- `/admin/**`은 JWT interceptor 제외 경로다.
- 관리자 인증/인가 정책은 통합 완료 전 별도 점검해야 한다.
- 기존 독립 `admin/` 앱과 중복 기능이 있으므로 최종 운영 구조를 결정해야 한다.

## 검증

```bash
./gradlew compileJava
```

현재 머지 후 `compileJava` 통과 확인.

## 참고 문서

- 전체 기능 명세: `../docs/APP_FEATURES.md`
- DB 설계: `../docs/DB_DESIGN.md`
- AI 서버: `../ai/README.md`
