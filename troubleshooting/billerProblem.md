# biller(Apple/Google Play) 경유 고정지출이 실제 카드 결제와 매칭되지 않음

- 작성일: 2026-07-15
- 관련 기능: fixedExpense.md, merchant.md, erd.md
- 유형: 실제 발생형(③) + 예방형(②) 혼합 — 항목별로 구분 표기
- 상태: 부분 해결 (ERD·해결 방향은 확정, 코드 구현은 착수 전)

## 1. 한 줄 요약

Apple/Google Play처럼 실제 서비스명을 가리는 결제대행(biller) 경유 구독은 카드 거래에 이름이
찍히지 않아 이름 기반 매칭이 구조적으로 불가능하다. 자동탐지(②)와 직접등록(③) 두 경로 모두
이 문제를 풀 방법이 없었고, `recurring_payment_group`·`user_merchant_preferences`에
`biller_merchant_id`를 추가해 `fixed_expense`와 같은 방식으로 풀기로 확정했다. 코드 구현은
아직 시작 전이다.

## 2. 배경과 초기 상태

`fixedExpense.md` 지시서대로 F-03(고정지출 직접 등록)을 구현하던 중 발견했다. 당시 구조:

- `FixedExpense.registerDirect()`는 `billerMerchant`를 항상 `null`로 저장한다
  (`FixedExpenseRegistrationService.java` 129-137행).
- `FixedExpenseDetectionService`는 `merchant_alias_id` 기준 alias 그룹핑만 수행한다
  (`findAliasGroupCandidates`, 76-77행). 엔티티 주석 자체가 "biller 경유 결제는 이 그룹핑
  대상에서 빠진다"고 명시(`RecurringPaymentGroup.java` 23-25행).
- 기대한 동작: `merchant.md` §5-D가 정의한 대로, biller 경유 구독도 이름 대신 결제대행사(merchant)와
  금액으로 대조해 정상적으로 자동 탐지·매칭돼야 한다.

## 3. 문제 현상 또는 예상 실패

**시나리오**: 사용자가 F-03에서 "웨이브"(카드형, 6,300원, 매달 5일)를 등록한다.

1. 한 달 뒤 실제 카드 거래가 들어온다 — 가맹점명은 `Apple`뿐, "웨이브"라는 문자열은 어디에도 없다.
2. `card_transaction.merchant_alias_id`는 biller 판정 시 이름 조회 자체를 건너뛰므로 **항상 NULL**로
   남는다(`MerchantIdentityService.resolve()` 48행, `merchant.md` §5-D-2).
3. `FixedExpenseMatchingService.findCandidates()`의 매칭 조건①(`tx.merchant_alias_id ==
   fe.merchant_alias_id`)이 한쪽이 항상 NULL이라 영원히 참이 될 수 없다.
4. 결과: 매달 "웨이브 결제가 안 보여요" 미납 알림(`FixedExpenseNotificationBatchService`가 이미
   생성함, 74-85행) + 실제 Apple 거래는 태깅 없이 일반 소비로 남아 월 예산에서 이중차감된다
   (예산 차감 1회 + "오늘 쓴 돈" 1회).

이 실패는 **직접등록 경로(③)에서 이미 재현 가능한 현재 코드 상태**다. **자동탐지(②)** 쪽은
아직 탐지 로직 자체가 없어 "실패가 발생한 적"은 없지만, `merchant.md` §5-D-3(`group_key =
(merchant ∈ biller) ? (merchant, amount) : alias`)을 구현하려는 순간 아래 원인 분석의 스키마
제약에 곧바로 막힐 것으로 예측된다.

## 4. 원인 분석과 근거

- `merchant.md` §5-D-2: biller 판정 시 `merchant_alias_terms` 조회를 건너뛰므로 거래에 alias가
  절대 붙지 않는다 — 이름형 학습 루프(§3, terms INSERT)로도 해결 불가. biller 이름을 terms에
  넣으면 전역 오염(§5-D-4 가드).
- `merchant.md` §5-D-3/D-4: biller 경유 구독은 탐지 시 `(merchant, amount)`로 그룹핑하고,
  후보 승인 시점에 `merchant_alias_id`(사용자가 새로 짓는 이름)와 `biller_merchant_id`(그룹의
  merchant)를 함께 채우도록 이미 "확정"으로 문서화돼 있었다.
- `erd.md` 432행(수정 전): `recurring_payment_group.merchant_alias_id`가 **FK NOT NULL**로
  확정돼 있어, biller 그룹(alias 없음)을 이 테이블에 넣을 방법이 없었다 — **D-3/D-4가 요구하는
  구현과 erd.md의 기존 확정 제약이 서로 모순되는 상태**였다.
- `RecurringPatternDetector.java`(28-58행): 넘겨받은 거래 목록 전체를 하나의 패턴으로만
  판정한다. 같은 merchant(Apple) 안에 금액이 다른 거래(정기결제 6,300원 + 일회성 15,000원)가
  섞이면 날짜 간격상 체인은 이어지되 금액 허용치 검사에서 전체가 탈락한다 — biller 그룹핑은
  alias 그룹핑과 달리 사전 금액 클러스터링이 별도로 필요하다.
- `FixedExpenseCandidateService.suppressAlias()`(60-72행): `candidate.getRecurringPaymentGroup()
  .getMerchantAlias()`를 전제로 거절/습관분류 preference를 저장한다. biller 후보는 승인 전까지
  alias가 없어 이 메서드가 그대로는 동작하지 않는다.
- `FixedExpenseCandidateRegisterRequest.java`: 가맹점(alias) 입력 필드가 없다 — 후보 승인 시점에
  이름형은 그룹이 이미 alias를 알고 있어 문제없지만, biller 후보는 그룹에 alias가 없어 사용자가
  승인 시점에 이름을 지어야 하는데 그 입력 경로가 없다.

## 5. 검토한 해결책과 트레이드오프

**① F-03에 "결제대행사 경유" 선택 필드 추가** (기각): 사용자가 등록 시점에 미리 Apple/Google Play를
지정하게 하는 안. 화면 필드 하나로 끝나 단순해 보이지만, 아직 결제내역이 없는 신규 구독은 대응
`merchant` 행이 없을 수 있어 find-or-create가 필요했다. 또한 프로젝트가 이미 채택한 "학습 루프"
철학(표기 흔들림도 사전 입력이 아니라 첫 실패 후 학습으로 해결)과 어긋난다.

**② F-05(미납확인) 학습 흐름에 biller 분기 추가** (채택, 직접등록 경로의 보완재로 유지): 이미
수집된 후보 거래를 사용자가 "이 거래예요"로 확정하면, 그 거래는 이미 실제 `merchant` 행을 갖고
있으므로 `fixed_expense.biller_merchant_id = transaction.getMerchant()`로 바로 채울 수 있다.
find-or-create가 필요 없고 화면 필드 추가도 없다. 자동탐지가 못 잡은 사각지대(3개월 데이터
미축적, 탐지 전 직접 선등록)의 백업 경로로 유지한다.

**③ `recurring_payment_group`·`user_merchant_preferences`에 `biller_merchant_id` 추가**
(채택, ②의 근본 해결): `fixed_expense`가 이미 쓰는 "무엇을 구독하나(alias) vs 어디로
청구되나(merchant)" 짝을 반복 적용한다. 새 개념을 발명하지 않고 기존 패턴을 재사용하므로
일관성이 있다. `merchant_alias_id`를 nullable로 바꾸고 CHECK로 상호배타를 강제한다.
`fixed_expense.biller_merchant_id`의 FK 타겟(merchant vs biller)은 논의 결과 **변경하지
않는다** — ②의 F-05 흐름이 이미 수집된 transaction의 merchant를 그대로 쓰므로 find-or-create
문제 자체가 발생하지 않아 FK를 바꿀 이유가 사라졌다.

## 6. 수행한 Action

- `plan/md/erd.md` 수정:
  - `recurring_payment_group`: `merchant_alias_id` NOT NULL → NULL, `biller_merchant_id`
    (FK NULL → `merchant`) 추가, CHECK(정확히 하나만 NOT NULL) 명시, biller 그룹 dedup은
    DB UNIQUE 대신 배치의 애플리케이션 레벨 find-or-create로 처리한다고 명시.
  - `user_merchant_preferences`: 동일하게 `merchant_alias_id` nullable화 +
    `biller_merchant_id` 추가 + CHECK + `UNIQUE(member_id, biller_merchant_id,
    preference_type)` 추가.
- 코드 변경은 아직 없음. 예정된 구현(별도 작업지시서로 분리 예정):
  1. `RecurringPaymentGroup` 엔티티에 `billerMerchant` 필드 추가, `merchantAlias` nullable화
  2. `FixedExpenseDetectionService`에 biller 그룹핑 경로 추가 — **금액 사전 클러스터링 알고리즘**:
     같은 `(member, biller_merchant)`의 취소 제외 거래를 **금액 오름차순 정렬** 후 그리디 버킷팅한다
     (인접 거래 금액 차이가 기존 `AMOUNT_TOLERANCE_RATIO`(10%, `RecurringPatternDetector`가 이미
     쓰는 상수 재사용) 이내면 같은 버킷, 벗어나면 새 버킷 시작). 버킷마다 **날짜 오름차순으로
     다시 정렬**해 `RecurringPatternDetector.detect()`를 **개별 호출**한다 — 한 merchant에서
     버킷 수만큼 `recurring_payment_group` 후보가 나올 수 있다(예: Apple 안에 웨이브 6,300원
     버킷 + 넷플릭스 9,900원 버킷 2개). alias 경로처럼 "merchant 1개 = group 1개"가 아니라
     "merchant 1개 = 버킷 수만큼의 group"이 될 수 있다는 점이 탐지 배치 루프 구조에 반영돼야 한다.
  3. `FixedExpenseCandidateRegisterRequest`에 가맹점(alias) 선택/신규 입력 필드 추가
  4. `FixedExpenseRegistrationService.registerFromCandidate()`가 biller 후보의 alias를
     resolve하고 `biller_merchant_id`를 함께 저장하도록 수정
  5. `UserMerchantPreference` 엔티티·`FixedExpenseCandidateService.suppressAlias()`에
     biller 분기 추가
  6. F-05(미납확인) 확인 API 신규 구현 — 이름형은 `merchant_alias_terms` 학습, biller형은
     `fixed_expense.biller_merchant_id` 직접 채우기로 분기. **전제 조건으로 `CardTransactionRepository`에
     후보 거래 재조회 쿼리가 먼저 필요하다** — 지금은 alias 그룹핑용 쿼리(`findAliasGroupCandidates`
     등)만 있고, "이 회원의, 아직 어떤 고정지출에도 안 태깅된(`fixedExpense IS NULL`) 거래 중
     금액이 `expected_amount`(_krw) 허용오차(기존 `FixedExpenseMatchingService`의
     `DOMESTIC_TOLERANCE`/`FOREIGN_TYPE2_TOLERANCE` 재사용) 이내이고 날짜가 `expected_pay_day`
     ±3일 창 안인 거래"를 찾는 신규 메서드가 없다. F-05 화면이 후보 목록을 보여주려면 이 쿼리가
     선행돼야 한다.

## 7. Result

구현 전. ERD 스키마 결정만 완료된 상태이며 실제 코드 동작 검증은 **검증 예정**이다.

## 8. 검증 방법

구현 후 아래 시나리오로 확인한다:

- **직접등록 회귀 확인**: 웨이브(카드형, Apple 경유)를 F-03으로 등록 → 다음 결제 전까지는
  `biller_merchant_id`가 NULL이어야 정상 → 미납 알림 발생 → F-05에서 후보 확정 →
  `biller_merchant_id`가 채워지고 그 달 이행 기록이 생기는지 확인.
  → 다음 달 같은 금액·결제일의 Apple 거래가 **자동으로** 매칭되는지 확인.
- **자동탐지 확인**: `codefapiResponse.json` 실데이터의 Apple 6,300원(3/21, 4/22, 5/21, 6/21)
  패턴이 3개월 이상 누적된 회원에게 F-02 추천 카드가 뜨는지, 승인 시 `merchant_alias_id`(신규
  입력)와 `biller_merchant_id`가 동시에 채워지는지 확인.
- **혼재 금액 확인(단일 버킷 vs 일회성)**: 같은 Apple 안에 정기결제(6,300원)와 일회성
  (15,000원/22,000원)이 섞인 상태에서 정기결제만 탐지되고 일회성은 그룹에서 빠지는지 확인
  (사전 금액 클러스터링 검증).
- **혼재 금액 확인(서로 다른 두 구독)**: 같은 Apple 안에 웨이브(6,300원)와 넷플릭스(9,900원)
  둘 다 3회 이상 반복된 상태에서, **두 버킷이 각각 별도로 클러스터링돼 후보 2건이 모두** 뜨는지
  확인 — 한쪽만 뜨거나 둘 다 안 뜨면 버킷팅이 아니라 여전히 단일 패턴 판정을 타고 있다는 뜻이다.
- **거절/습관분류 확인**: biller 후보를 거절했을 때 `user_merchant_preferences`에
  `biller_merchant_id` 기준 행이 생기고, 다음 배치에서 같은 biller+금액 조합이 재추천되지
  않는지 확인.
- **F-05 후보 조회 확인**: 미납 알림을 탭했을 때, 신규 쿼리가 해당 회원의 미태깅 거래 중
  금액·결제일 창 조건에 맞는 거래만 정확히 후보로 반환하는지(다른 회원 거래·이미 태깅된 거래·
  조건 밖 거래가 섞여 나오지 않는지) 확인.

## 9. 재발 방지와 적용 범위

- **같은 원칙을 적용해야 하는 다른 기능**: 이후 "결제내역에 이름이 안 찍히는 대상"(예: 상품권,
  기프트카드 경유 결제)이 생기면 이번과 같은 `alias`/`biller_merchant_id` 이중 참조 패턴을
  재사용한다.
- **재발 방지책**: 기능 md(`merchant.md`, `fixedExpense.md`)가 "확정"으로 표시한 내용도, 그
  구현이 딛고 서는 `erd.md`의 기존 제약과 실제로 충돌하지 않는지 구현 착수 전에 반드시
  대조한다 — 이번 건은 두 "확정" 문서가 서로 모순돼 있었는데도 한동안 발견되지 않았다.
- **다시 발생했을 때 먼저 확인할 항목**: `card_transaction.merchant_alias_id`가 NULL인데
  `fixed_expense`가 CARD형으로 등록돼 있는 케이스가 있는지, 그 `fixed_expense.biller_merchant_id`가
  채워져 있는지부터 확인한다.

## 10. 관련 자료

- `plan/md/merchant.md` §5-D(D-1~D-6), §3
- `plan/md/fixedExpense.md` §4, §5, §6
- `plan/md/erd.md` (`recurring_payment_group`, `user_merchant_preferences`, `fixed_expense` 절)
- `backend/src/main/java/com/weaone/themoa/domain/fixedexpense/entity/RecurringPaymentGroup.java`
- `backend/src/main/java/com/weaone/themoa/domain/fixedexpense/entity/FixedExpense.java`
- `backend/src/main/java/com/weaone/themoa/domain/fixedexpense/service/FixedExpenseDetectionService.java`
- `backend/src/main/java/com/weaone/themoa/domain/fixedexpense/service/FixedExpenseMatchingService.java`
- `backend/src/main/java/com/weaone/themoa/domain/fixedexpense/service/FixedExpenseNotificationBatchService.java`
- `backend/src/main/java/com/weaone/themoa/domain/fixedexpense/service/RecurringPatternDetector.java`
- `backend/src/main/java/com/weaone/themoa/domain/fixedexpense/service/FixedExpenseCandidateService.java`
- `backend/src/main/java/com/weaone/themoa/domain/fixedexpense/dto/request/FixedExpenseCandidateRegisterRequest.java`
- `backend/src/main/java/com/weaone/themoa/domain/fixedexpense/dto/request/FixedExpenseDirectRegisterRequest.java`
- `backend/src/main/java/com/weaone/themoa/domain/merchant/entity/MerchantAliasTerms.java`
- `backend/src/main/java/com/weaone/themoa/domain/merchant/service/MerchantIdentityService.java`
