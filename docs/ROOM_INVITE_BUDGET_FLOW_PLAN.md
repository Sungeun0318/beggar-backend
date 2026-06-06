# 거지방 초대·예산·추천 실시간 흐름 기획

## 1. 목표

거지방 생성 이후 초대받은 인원이 실시간으로 들어오는 것을 방장이 확인하고, 방장이 예산 입력을 시작하면 모든 참여자가 예산 입력 화면으로 이동한다. 방장 포함 모든 활성 참여자가 예산을 제출하면 서버가 자동으로 예산을 확정하고, 확정된 예산·태그·지역을 기준으로 맞춤 추천을 호출한다.

이 흐름에서 WebSocket은 실시간 화면 갱신과 자동 이동을 맡고, 실제 권한·상태·검증은 REST API와 DB에서 처리한다.

## 2. 핵심 용어

- WebSocket: 브라우저와 서버가 연결을 유지하면서 서버가 즉시 메시지를 밀어줄 수 있는 통신 방식.
- Topic: WebSocket 메시지를 구독하는 주소. 예를 들어 `/topic/rooms/1/state`는 1번 방 상태 이벤트를 받는 주소다.
- ACTIVE 멤버: 현재 방에 정상 참여 중인 사용자.
- 방장: `Room.ownerUserNo`와 로그인 유저 번호가 같은 사용자.
- 예산 확정: ACTIVE 멤버 전원이 예산을 제출한 뒤, 가장 낮은 1인 예산에 ACTIVE 멤버 수를 곱해 총 예산을 저장하는 처리.

## 3. 전체 사용자 흐름

1. 방장이 방을 생성한다.
2. 서버는 `Room`을 만들고 방장을 `RoomMember(ACTIVE)`로 자동 추가한다.
3. 방 상태는 `INVITING`으로 시작한다.
4. 방장은 친구 초대 화면에서 초대 링크를 복사한다.
5. 초대받은 사용자는 `/join/{code}`로 들어온다.
6. 로그인하지 않은 사용자는 로그인 후 다시 입장 흐름으로 돌아온다.
7. 로그인한 사용자는 `POST /rooms/join`으로 방에 입장한다.
8. 서버는 멤버 추가 후 WebSocket으로 멤버 목록 갱신 이벤트를 보낸다.
9. 방장은 실시간으로 들어온 인원을 확인한다.
10. 방장이 `예산 입력 시작`을 누른다.
11. 서버는 방장 권한과 방 상태를 검증한 뒤 `BUDGET_INPUT`으로 변경한다.
12. 서버는 WebSocket으로 모든 참여자에게 예산 입력 시작 이벤트를 보낸다.
13. 모든 참여자는 예산 입력 화면으로 이동한다.
14. 각 참여자가 본인 예산을 제출한다.
15. 서버는 제출자가 ACTIVE 멤버인지 확인하고 예산을 저장한다.
16. 서버는 예산 금액은 숨기고 제출 여부만 WebSocket으로 보낸다.
17. ACTIVE 멤버 전원이 제출하면 서버가 자동으로 예산을 확정한다.
18. 서버는 `RoomBudgetResult`와 `Room.totalBudget`을 저장하고 방 상태를 `BUDGET_DONE`으로 변경한다.
19. 서버는 WebSocket으로 예산 확정 이벤트를 보낸다.
20. 모든 참여자는 예산 결과 화면으로 이동한다.
21. 예산 결과 화면에서 맞춤 추천으로 이동하면 서버는 방의 위치·태그·확정 예산을 기준으로 추천을 반환한다.

## 4. 방 상태 설계

`Room`에 상태 컬럼을 추가한다.

```text
INVITING        초대 대기 중
BUDGET_INPUT    예산 입력 중
BUDGET_DONE     예산 확정 완료
ACTIVE          거지방 진행 중
ENDED           방 종료
```

상태 전이 규칙은 다음과 같다.

```text
방 생성
-> INVITING

방장이 예산 입력 시작
-> BUDGET_INPUT

ACTIVE 멤버 전원 예산 제출
-> BUDGET_DONE

추천 확인 후 거지방 시작
-> ACTIVE

방 종료
-> ENDED
```

## 5. 백엔드 API 설계

### 5.1 방 생성

```http
POST /rooms
```

처리 규칙:

- 로그인 필수.
- `Room.status = INVITING`.
- `Room.ownerUserNo = loginUserNo`.
- 방장을 `RoomMember(ACTIVE)`로 자동 추가.
- 초대 코드 생성.
- 방 태그 저장.
- 응답에 `roomNo`, `roomCode`, `status`, `memberCount`, `maxMemberCount`, `tags`, `location` 포함.

### 5.2 초대 코드 입장

```http
POST /rooms/join
```

요청:

```json
{
  "code": "BWY3xNNZW6CJ"
}
```

처리 규칙:

- 로그인 필수.
- 초대 코드로 방 조회.
- 방 상태가 `INVITING` 또는 `BUDGET_INPUT`일 때만 입장 허용할지 정책 결정 필요.
- 기본 정책은 `INVITING`에서만 입장 허용.
- ACTIVE 멤버 수가 `maxMemberCount` 이상이면 거절.
- 이미 ACTIVE 멤버면 중복 생성하지 않고 기존 방 응답.
- `LEFT` 상태 재입장은 추후 정책 결정. 우선은 ACTIVE로 재활성화 가능하게 설계.
- `KICKED` 상태 재입장은 거절.
- 입장 성공 후 `/topic/rooms/{roomNo}/members`로 멤버 갱신 이벤트 발행.

### 5.3 멤버 목록 조회

```http
GET /rooms/{roomNo}/members
```

처리 규칙:

- 로그인 필수.
- 방 ACTIVE 멤버만 조회.
- 예산 금액은 절대 응답하지 않는다.
- 제출 여부만 응답한다.

응답 예시:

```json
[
  {
    "name": "거지판다",
    "status": "방장",
    "mine": true,
    "budgetSubmitted": false
  }
]
```

### 5.4 예산 입력 시작

```http
POST /rooms/{roomNo}/budget/start
```

처리 규칙:

- 로그인 필수.
- 방장만 가능.
- 방 상태가 `INVITING`일 때만 가능.
- ACTIVE 멤버가 최소 2명 이상인지 정책 결정 필요.
- 성공 시 `Room.status = BUDGET_INPUT`.
- `/topic/rooms/{roomNo}/state`로 `BUDGET_INPUT_STARTED` 이벤트 발행.

### 5.5 예산 제출

```http
POST /rooms/{roomNo}/budget
```

처리 규칙:

- 로그인 필수.
- 로그인 유저가 해당 방 ACTIVE 멤버인지 검증.
- 방 상태가 `BUDGET_INPUT`인지 검증.
- 예산 금액은 양수인지 검증.
- 같은 유저가 다시 제출하면 기존 예산 수정.
- 제출 후 `/topic/rooms/{roomNo}/budget`으로 제출 상태 이벤트 발행.
- ACTIVE 멤버 수와 예산 제출 유저 수가 같으면 자동 확정.

### 5.6 예산 확정

자동 확정 기준:

```text
ACTIVE 멤버 수 == ACTIVE 멤버 중 예산 제출 완료 수
```

계산 규칙:

```text
minBudgetPerPerson = ACTIVE 멤버가 제출한 예산 중 최저 금액
memberCount = ACTIVE 멤버 수
totalBudget = minBudgetPerPerson * memberCount
```

저장 규칙:

- `RoomBudgetResult.minBudgetPerPerson` 저장.
- `RoomBudgetResult.memberCount` 저장.
- `RoomBudgetResult.totalBudget` 저장.
- `Room.totalBudget` 동기화.
- `Room.status = BUDGET_DONE`.
- `/topic/rooms/{roomNo}/state`로 `BUDGET_CONFIRMED` 이벤트 발행.

### 5.7 예산 결과 조회

```http
GET /rooms/{roomNo}/budget/result
```

처리 규칙:

- 로그인 필수.
- 해당 방 ACTIVE 멤버만 조회 가능.
- 확정 전이면 409 또는 명확한 에러 응답.

### 5.8 맞춤 추천

```http
GET /rooms/{roomNo}/recommend
```

처리 규칙:

- `Room` 조회.
- `RoomBudgetResult` 조회.
- `RoomPurposeTag` 조회.
- 위치는 `Room.location` 또는 좌표 기반으로 사용.
- 추천 가격 기준은 우선 `minBudgetPerPerson`.
- 조합 추천이나 남은 예산 계산이 필요하면 `totalBudget`도 사용.
- 프론트는 `roomNo`만 넘기고, 서버가 방의 예산·태그·지역을 DB에서 읽는 구조로 간다.

## 6. WebSocket 이벤트 설계

### 6.1 구독 주소

```text
/topic/rooms/{roomNo}/members
/topic/rooms/{roomNo}/budget
/topic/rooms/{roomNo}/state
```

### 6.2 이벤트 타입

```text
MEMBERS_UPDATED
BUDGET_INPUT_STARTED
BUDGET_SUBMITTED
BUDGET_CONFIRMED
ROOM_STARTED
ROOM_ENDED
```

### 6.3 이벤트 응답 예시

멤버 갱신:

```json
{
  "type": "MEMBERS_UPDATED",
  "roomNo": 1,
  "members": [
    {
      "name": "거지판다",
      "status": "방장",
      "mine": false,
      "budgetSubmitted": false
    }
  ]
}
```

예산 입력 시작:

```json
{
  "type": "BUDGET_INPUT_STARTED",
  "roomNo": 1,
  "nextPath": "/budget/input?roomNo=1"
}
```

예산 제출 상태:

```json
{
  "type": "BUDGET_SUBMITTED",
  "roomNo": 1,
  "submittedCount": 3,
  "memberCount": 4
}
```

예산 확정:

```json
{
  "type": "BUDGET_CONFIRMED",
  "roomNo": 1,
  "nextPath": "/budget/result?roomNo=1"
}
```

## 7. 프론트 화면 설계

### 7.1 친구 초대 화면

필요 기능:

- 초대 링크 복사 버튼.
- 현재 참여자 목록 조회.
- `/topic/rooms/{roomNo}/members` 구독.
- 멤버 입장 시 목록 자동 갱신.
- 방장만 `예산 입력 시작` 버튼 표시.
- 방장이 시작하면 REST API 호출.
- `BUDGET_INPUT_STARTED` 이벤트 수신 시 예산 입력 화면으로 이동.

### 7.2 초대 링크 입장 화면

필요 기능:

- URL: `/join/:code`.
- 로그인 여부 확인.
- 미로그인 상태면 로그인 화면으로 보내고, 로그인 후 원래 초대 코드로 복귀.
- 로그인 상태면 `POST /rooms/join`.
- 입장 성공 후 초대 대기 화면으로 이동.
- 방장이 시작하기 전까지 대기.

### 7.3 예산 입력 화면

필요 기능:

- 방 정보 조회.
- 멤버 제출 상태 조회.
- `/topic/rooms/{roomNo}/budget` 구독.
- `/topic/rooms/{roomNo}/state` 구독.
- 제출 상태 실시간 갱신.
- 본인 예산 제출.
- `BUDGET_CONFIRMED` 이벤트 수신 시 예산 결과 화면으로 이동.

### 7.4 예산 결과 화면

필요 기능:

- `GET /rooms/{roomNo}/budget/result` 호출.
- 최저 1인 예산 표시.
- 참여 인원 표시.
- 총 예산 표시.
- 추천 보기 버튼으로 `/recommend?roomNo=1` 이동.

### 7.5 맞춤 추천 화면

필요 기능:

- `GET /rooms/{roomNo}/recommend` 호출.
- 서버가 방의 위치·태그·예산 기준 추천을 반환.
- 지역 변경 시 위치 검색 후 추천 재호출.
- 현재 위치 사용 시 브라우저 Geolocation으로 좌표 전달.

## 8. 구현 단계와 커밋 단위

### Step 1. Room 상태 모델 추가

- `RoomStatus` enum 추가.
- `Room.status` 컬럼 추가.
- 방 생성 시 `INVITING`.
- 응답 DTO에 `status` 추가.
- 빌드 확인.
- 커밋: `Feat: 거지방 상태 모델 추가`

### Step 2. 방 입장 규칙 보강

- 최대 인원 제한.
- ACTIVE 중복 입장 처리.
- LEFT 재입장 처리.
- KICKED 재입장 차단.
- 입장 후 멤버 목록 응답 정리.
- 빌드 확인.
- 커밋: `Feat: 초대 코드 입장 규칙 보강`

### Step 3. WebSocket 이벤트 구조 추가

- 방 이벤트 DTO 추가.
- WebSocket 발행 서비스 추가.
- 멤버 갱신 이벤트 발행.
- 빌드 확인.
- 커밋: `Feat: 거지방 실시간 이벤트 추가`

### Step 4. 예산 입력 시작 API 추가

- `POST /rooms/{roomNo}/budget/start`.
- 방장 권한 검증.
- 상태 전이 `INVITING -> BUDGET_INPUT`.
- WebSocket 상태 이벤트 발행.
- 빌드 확인.
- 커밋: `Feat: 예산 입력 시작 API 추가`

### Step 5. 예산 제출 검증 보강

- ACTIVE 멤버만 제출 가능.
- `BUDGET_INPUT` 상태에서만 제출 가능.
- 제출 상태 WebSocket 발행.
- 비멤버 예산 제출 차단.
- 빌드 확인.
- 커밋: `Feat: 예산 제출 멤버 검증 추가`

### Step 6. 전원 제출 자동 확정

- ACTIVE 멤버 기준 제출 수 계산.
- 최저 예산 기준 총예산 계산.
- 결과 저장.
- 방 상태 `BUDGET_DONE`.
- WebSocket 확정 이벤트 발행.
- 빌드 확인.
- 커밋: `Feat: 전원 제출 예산 자동 확정`

### Step 7. 추천 API 예산 기준 연결

- 추천 API에서 `RoomBudgetResult` 조회.
- 태그·지역·예산 기준 사용.
- 프론트가 넘긴 임시 값 의존 제거.
- 빌드 확인.
- 커밋: `Feat: 맞춤 추천 예산 기준 연결`

### Step 8. 프론트 초대 화면 실시간 연결

- 초대 링크 복사 버튼 보강.
- 멤버 목록 WebSocket 구독.
- 방장 시작 버튼 연결.
- 상태 이벤트 수신 시 자동 이동.
- 빌드 확인.
- 커밋: `Feat: 초대 화면 실시간 입장 연결`

### Step 9. 프론트 예산 입력 실시간 연결

- 예산 제출 상태 WebSocket 구독.
- 전원 제출 시 결과 화면 자동 이동.
- 빌드 확인.
- 커밋: `Feat: 예산 입력 실시간 상태 연결`

### Step 10. 프론트 추천 화면 roomNo 기준 정리

- 예산 결과에서 추천으로 `roomNo` 전달.
- 추천 화면은 `roomNo` 기준 API 호출.
- 지역 변경·현재 위치 사용 시 추천 재호출.
- 빌드 확인.
- 커밋: `Feat: 추천 화면 방 기준 호출 정리`

## 9. 검증 체크리스트

### 백엔드

- 방 생성 시 방장 자동 입장.
- 초대 코드로 로그인 유저 입장.
- 최대 인원 초과 입장 차단.
- 비멤버 예산 제출 차단.
- 방장만 예산 입력 시작 가능.
- 전원 제출 전에는 결과 조회 불가.
- 전원 제출 후 결과 자동 생성.
- 예산 결과가 최저 금액 곱하기 ACTIVE 멤버 수로 계산됨.
- WebSocket 이벤트가 멤버 입장, 예산 시작, 예산 제출, 예산 확정 시 발행됨.

### 프론트

- 방장이 초대 화면에서 실시간으로 멤버를 확인할 수 있음.
- 초대받은 사용자가 로그인 후 방에 입장할 수 있음.
- 방장이 시작하면 모든 참여자가 예산 입력 화면으로 이동함.
- 예산 제출 상태가 실시간으로 바뀜.
- 전원 제출 후 모든 참여자가 예산 결과 화면으로 이동함.
- 추천 화면이 방의 예산·태그·지역을 기준으로 데이터를 다시 불러옴.

## 10. 주의할 점

- WebSocket 이벤트에는 예산 금액을 보내지 않는다.
- 프론트 자동 이동은 편의 기능이고, 권한 검증은 반드시 백엔드에서 한다.
- 추천 기준은 `minBudgetPerPerson`을 우선 사용한다.
- `totalBudget`은 조합 추천이나 남은 예산 계산에 사용한다.
- `RoomMember.Status.KICKED` 사용자의 재입장은 차단한다.
- `Room.status` 없이 프론트 상태만으로 화면을 넘기면 동시 접속 상황에서 쉽게 꼬인다.
