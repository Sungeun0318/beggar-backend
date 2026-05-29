# 커밋 메시지 태그 관례 (Commit Convention)

팀 프로젝트의 효율적인 협업을 위해 아래의 태그 관례를 준수해 주세요.

---

## 📌 주요 태그 유형

| 태그 | 설명 | 예시 |
|---|---|---|
| **Feat** | 새로운 기능 추가 | `Feat: 소셜 로그인 기능 추가` |
| **Fix** | 버그 수정 | `Fix: 메인 페이지 이미지 로딩 오류 수정` |
| **Design** | CSS 등 사용자 UI 디자인 변경 | `Design: 홈 화면 카드 레이아웃 변경` |
| **!BREAKING CHANGE** | 커다란 API 변경이나 하위 호환성이 깨지는 변경 | `!BREAKING CHANGE: 인증 API 응답 구조 변경` |
| **Style** | 코드 의미에 영향을 주지 않는 수정 (세미콜론 누락, 화이트 스페이스 등) | `Style: 들여쓰기 정리` |
| **Refactor** | 코드 리팩토링 (기능 변화 없이 구조만 개선) | `Refactor: RoomService 책임 분리` |
| **Docs** | 문서 수정 (README.md, 주석 등) | `Docs: API 명세서 업데이트` |
| **Chore** | 빌드 업무, 패키지 매니저 설정, 자잘한 문서 수정 등 | `Chore: build.gradle 의존성 버전 업그레이드` |
| **Test** | 테스트 코드 추가 및 리팩토링 | `Test: BudgetService 단위 테스트 추가` |
| **Rename** | 파일 혹은 폴더명을 수정하거나 옮기는 작업만 하는 경우 | `Rename: util → common 패키지로 이동` |
| **Remove** | 파일을 삭제하는 작업만 수행하는 경우 | `Remove: 사용하지 않는 LegacyAuthService 삭제` |

---

## ✅ 커밋 메시지 작성 조건

1. **태그는 대문자로 시작**하고 콜론(`:`) 뒤에 한 칸 띄어쓴다.
   - ✅ `Feat: 카카오 로그인 추가`
   - ❌ `feat:카카오 로그인 추가`

2. **제목은 50자 이내**, 명령형·간결하게 작성한다.
   - ✅ `Fix: 영수증 OCR 콜백 NPE 수정`
   - ❌ `Fix: 영수증 OCR 콜백에서 NullPointerException이 발생하는 문제를 수정했습니다`

3. **제목 끝에 마침표(`.`) 금지**.

4. **본문이 필요하면 제목과 한 줄 띄우고** 작성한다.
   ```
   Feat: 거지력 점수 산식 구현

   - 예산준수율 × 0.40 + 평균절약률 × 0.40 + 참여빈도 × 0.20
   - 동기 + 동일 트랜잭션에서 UPSERT 처리
   - 칭호 5단계 매핑 추가 (아기 거지 ~ 전설의 거지)
   ```

5. **한 커밋에는 하나의 목적만** 담는다. 여러 태그가 섞이면 커밋을 분리한다.

6. **이슈 번호가 있으면** 본문 마지막 또는 제목 뒤 괄호에 표기한다.
   - `Fix: 방 생성 시 중복 코드 발생 (#42)`

7. **`!BREAKING CHANGE`는 본문에 변경 내용과 마이그레이션 가이드**를 함께 적는다.

---

## 예시 모음

```
Feat: 카카오 OAuth 로그인 구현
Fix: 방 입장 시 중복 검사 누락 수정
Refactor: RoomService와 RoomAnonymousService 통합
Docs: 백엔드 STRUCTURE.md에 인증 흐름 추가
Chore: application.properties 위치를 src/main/resources로 이동
Test: BudgetService.confirm 통합 테스트 추가
Remove: 사용하지 않는 RoomAnonymousService 삭제
Style: ErrorCode enum 정렬 및 들여쓰기 정리
Rename: util 패키지 → common 패키지로 이동
!BREAKING CHANGE: /auth/kakao 응답에서 userNo 제거, profile 객체로 통합
```
