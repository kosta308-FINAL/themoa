package com.weaone.themoa.domain.cardtransaction.repository;

import com.weaone.themoa.domain.cardtransaction.entity.CardTransaction;
import com.weaone.themoa.domain.cardtransaction.entity.PaymentMethod;
import com.weaone.themoa.domain.cardtransaction.entity.TransactionSource;
import com.weaone.themoa.domain.cardtransaction.entity.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CardTransactionRepository extends JpaRepository<CardTransaction, Long> {

    /** 멱등성 대조 키(cardtransaction.md §2). SYNC 건만 이 키가 전부 채워진다. */
    Optional<CardTransaction> findByMember_IdAndCard_IdAndUsedDateAndUsedAtAndApprovalNo(
            Long memberId, Long cardId, LocalDate usedDate, LocalDateTime usedAt, String approvalNo);

    Optional<CardTransaction> findByIdAndMember_Id(Long id, Long memberId);

    /** 수기 거래 수정·삭제 소유권 검증(dayguide.md §8.1). {@code @SQLRestriction}이 이미 대체된 행을 걸러낸다. */
    Optional<CardTransaction> findByIdAndMember_IdAndSource(Long id, Long memberId, TransactionSource source);

    Page<CardTransaction> findByMember_IdOrderByUsedAtDesc(Long memberId, Pageable pageable);

    /** 반복결제 탐지 1단계(fixedExpense.md §2): alias 레벨로 3회 이상 잡히는 그룹 후보. 취소 건 제외. */
    @Query("select t.merchantAlias.id as merchantAliasId, count(t) as transactionCount from CardTransaction t "
            + "where t.member.id = :memberId and t.merchantAlias is not null and t.status <> :canceled "
            + "group by t.merchantAlias.id having count(t) >= :minCount")
    List<AliasGroupCount> findAliasGroupCandidates(@Param("memberId") Long memberId,
                                                     @Param("canceled") TransactionStatus canceled,
                                                     @Param("minCount") long minCount);

    /** 위 후보 alias의 실제 거래 목록(패턴 검증용, §2 규칙 2~5). */
    List<CardTransaction> findByMember_IdAndMerchantAlias_IdAndStatusNotOrderByUsedDateAsc(
            Long memberId, Long merchantAliasId, TransactionStatus canceled);

    /**
     * 반복결제 탐지(biller 경유, merchant.md §5-D-3): 이름으로 alias가 안 붙는 biller 거래를
     * merchant 단위로 3회 이상 잡히는 후보. 실제 서비스 구분은 그룹핑 이후 금액 클러스터링이 담당한다.
     */
    @Query("select t.merchant.id as merchantId, count(t) as transactionCount from CardTransaction t "
            + "where t.member.id = :memberId and t.merchantAlias is null and t.merchant is not null "
            + "and t.status <> :canceled and upper(trim(t.merchant.merchantNameRaw)) in "
            + "(select upper(trim(b.name)) from Biller b) "
            + "group by t.merchant.id having count(t) >= :minCount")
    List<MerchantGroupCount> findBillerMerchantGroupCandidates(@Param("memberId") Long memberId,
                                                                 @Param("canceled") TransactionStatus canceled,
                                                                 @Param("minCount") long minCount);

    /** 위 후보 merchant의 실제 거래 목록(금액 클러스터링용, 금액 오름차순). */
    List<CardTransaction> findByMember_IdAndMerchant_IdAndMerchantAliasIsNullAndStatusNotOrderByAmountAsc(
            Long memberId, Long merchantId, TransactionStatus canceled);

    /** 학습 루프 2단계(merchant.md §3): 같은 원본 가맹점(merchant)의 이 회원 거래 전체 재태깅 대상. */
    List<CardTransaction> findByMember_IdAndMerchant_Id(Long memberId, Long merchantId);

    /**
     * F-05 미납 확인 후보(troubleshooting/billerProblem.md §6): 미태깅 + 금액오차 + 결제일 윈도우.
     * 가맹점은 미식별(merchantAlias null, biller 경유 포함) 거래이거나 이 고정지출과 같은 alias로
     * 이미 식별된 거래만 후보로 남긴다 — 이미 다른 alias로 확실히 식별된 거래(예: 편의점)는 배제해
     * 오탐을 줄인다. biller 경유 구독(Apple 등)은 신원 판별을 건너뛰어 항상 merchantAlias가 null이라
     * (merchant.md §3 2단계) 이 필터로도 그대로 후보에 남는다.
     * 컨트롤러에서 세션 밖에 {@code CardTransactionResponse.from}으로 변환하므로 category·card 체인·
     * merchantAlias·merchant를 미리 fetch join 한다.
     */
    @Query("select t from CardTransaction t "
            + "join fetch t.category "
            + "left join fetch t.card c "
            + "left join fetch c.cardConnection cc "
            + "left join fetch cc.cardIssuer "
            + "left join fetch t.merchantAlias "
            + "left join fetch t.merchant "
            + "where t.member.id = :memberId and t.fixedExpense is null "
            + "and t.status <> :canceled and t.usedDate between :startDate and :endDate "
            + "and t.amount between :minAmount and :maxAmount "
            + "and (t.merchantAlias is null or t.merchantAlias.id = :merchantAliasId) "
            + "order by t.usedDate desc")
    List<CardTransaction> findMissedPaymentCandidates(@Param("memberId") Long memberId,
                                                        @Param("canceled") TransactionStatus canceled,
                                                        @Param("startDate") LocalDate startDate,
                                                        @Param("endDate") LocalDate endDate,
                                                        @Param("minAmount") BigDecimal minAmount,
                                                        @Param("maxAmount") BigDecimal maxAmount,
                                                        @Param("merchantAliasId") Long merchantAliasId);

    /**
     * 카테고리별 소비 비중/내역(category.md §6·§7): 거절 제외, 고정지출 태그 거래 제외. 도넛에는
     * 거래별 순액(amount − canceled_amount)이 0원보다 큰 실제 소비만 반영한다(Type 2 음수행 포함,
     * 0원/음수 조각은 만들지 않음). 건수도 순액이 0원보다 큰 거래만 센다. category_id 스냅샷 기준
     * GROUP BY 한 방.
     */
    @Query("select t.category.id as categoryId, t.category.name as categoryName, "
            + "sum(case when (t.amount - coalesce(t.canceledAmount, 0)) > 0 "
            + "then (t.amount - coalesce(t.canceledAmount, 0)) else 0 end) as totalAmount, "
            + "sum(case when (t.amount - coalesce(t.canceledAmount, 0)) > 0 then 1L else 0L end) as transactionCount "
            + "from CardTransaction t where t.member.id = :memberId and t.status <> :rejected "
            + "and t.fixedExpense is null and t.usedDate between :startDate and :endDate "
            + "group by t.category.id, t.category.name "
            + "having sum(case when (t.amount - coalesce(t.canceledAmount, 0)) > 0 "
            + "then (t.amount - coalesce(t.canceledAmount, 0)) else 0 end) > 0 "
            + "order by totalAmount desc")
    List<CategorySummary> summarizeByCategory(@Param("memberId") Long memberId,
                                               @Param("rejected") TransactionStatus rejected,
                                               @Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate);

    /**
     * 취소 안내값(category.md §6): 같은 기준 거래의 Type 1 canceled_amount 합 + Type 2 음수행
     * abs(amount) 합. Type 2 행은 canceled_amount가 항상 비어 있어(cardtransaction.md §3-5) 이중
     * 집계되지 않는다.
     */
    @Query("select coalesce(sum(case when t.amount < 0 then abs(t.amount) else coalesce(t.canceledAmount, 0) end), 0) "
            + "from CardTransaction t where t.member.id = :memberId and t.status <> :rejected "
            + "and t.fixedExpense is null and t.usedDate between :startDate and :endDate")
    BigDecimal sumCanceledAmount(@Param("memberId") Long memberId,
                                  @Param("rejected") TransactionStatus rejected,
                                  @Param("startDate") LocalDate startDate,
                                  @Param("endDate") LocalDate endDate);

    /**
     * 이번 급여주기 순지출 집계(dailyBudget.md §3): 고정지출 태그 제외 + 거절 제외 + 순액 합계.
     * Type 1 취소는 원 결제행 순액 소급 정정, Type 2 취소행은 음수 amount로 그 날짜에 반영된다.
     * (대체된 수기 건은 엔티티의 {@code @SQLRestriction("replaced_at is null")}이 이 쿼리를 포함해 전부 자동 제외한다)
     */
    @Query("select coalesce(sum(t.amount - coalesce(t.canceledAmount, 0)), 0) from CardTransaction t "
            + "where t.member.id = :memberId and t.fixedExpense is null and t.status <> :rejected "
            + "and t.usedDate between :startDate and :endDate")
    BigDecimal sumNetSpend(@Param("memberId") Long memberId,
                           @Param("rejected") TransactionStatus rejected,
                           @Param("startDate") LocalDate startDate,
                           @Param("endDate") LocalDate endDate);

    /**
     * 카테고리 소비 상세 초·중·후반/평일·주말 집계(categoryDetail.md §8.2): 선택 카테고리의 일자별 순액 합계.
     * DB 전용 요일 함수를 쓰지 않고 서비스에서 {@code LocalDate#getDayOfWeek()}로 구간을 나눈다.
     */
    @Query("select t.usedDate as usedDate, "
            + "sum(case when (t.amount - coalesce(t.canceledAmount, 0)) > 0 "
            + "then (t.amount - coalesce(t.canceledAmount, 0)) else 0 end) as totalAmount "
            + "from CardTransaction t where t.member.id = :memberId and t.category.id = :categoryId "
            + "and t.status <> :rejected and t.fixedExpense is null "
            + "and t.usedDate between :startDate and :endDate "
            + "group by t.usedDate "
            + "having sum(case when (t.amount - coalesce(t.canceledAmount, 0)) > 0 "
            + "then (t.amount - coalesce(t.canceledAmount, 0)) else 0 end) > 0 "
            + "order by t.usedDate asc")
    List<DailyCategoryAmount> summarizeDailyByCategory(@Param("memberId") Long memberId,
                                                         @Param("categoryId") Long categoryId,
                                                         @Param("rejected") TransactionStatus rejected,
                                                         @Param("startDate") LocalDate startDate,
                                                         @Param("endDate") LocalDate endDate);

    /**
     * S-01 오늘 거래 미리보기(dayguide.md §8.1): 거절 제외, 최신순. 고정지출 태그 거래도 표시하되
     * ({@code fixedExpenseId} 배지) 오늘 순사용액 등 집계에서만 별도로 제외한다(§5). {@code pageable}로
     * 미리보기 개수를 제한하면서도 {@code Page.getTotalElements()}로 제한 전 전체 건수를 함께 얻는다.
     */
    Page<CardTransaction> findByMember_IdAndStatusNotAndUsedDateOrderByUsedAtDesc(
            Long memberId, TransactionStatus rejected, LocalDate usedDate, Pageable pageable);

    /**
     * S-01 최근 N일 막대그래프(dayguide.md §3.3·§8.1): 날짜별 순사용액. 거래가 없는 날짜는 결과에서
     * 빠지므로 호출자가 0원으로 채운다.
     */
    @Query("select t.usedDate as usedDate, sum(t.amount - coalesce(t.canceledAmount, 0)) as netAmount "
            + "from CardTransaction t where t.member.id = :memberId and t.fixedExpense is null "
            + "and t.status <> :rejected and t.usedDate between :startDate and :endDate "
            + "group by t.usedDate")
    List<DailyNetAmount> sumNetSpendByDate(@Param("memberId") Long memberId,
                                            @Param("rejected") TransactionStatus rejected,
                                            @Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);

    /**
     * S-04 전체 소비내역(dayguide.md §8.1): 급여 주기 범위 + 선택적 날짜·카테고리 필터. 거절 제외, 최신순
     * 페이지. 고정지출 태그 거래도 목록에는 표시하고(배지) 집계에서만 별도로 제외한다(§5).
     */
    @Query("select t from CardTransaction t where t.member.id = :memberId "
            + "and t.status <> :rejected and t.usedDate between :cycleStart and :cycleEnd "
            + "and (:date is null or t.usedDate = :date) "
            + "and (:categoryId is null or t.category.id = :categoryId) "
            + "order by t.usedAt desc")
    Page<CardTransaction> searchForSpendingGuide(@Param("memberId") Long memberId,
                                                  @Param("rejected") TransactionStatus rejected,
                                                  @Param("cycleStart") LocalDate cycleStart,
                                                  @Param("cycleEnd") LocalDate cycleEnd,
                                                  @Param("date") LocalDate date,
                                                  @Param("categoryId") Long categoryId,
                                                  Pageable pageable);

    /**
     * 대체 대상(entryMode.md §4/§4-1): 갭 구간의 결제수단=카드 수기 건. {@code @SQLRestriction}으로 이미
     * 대체된 행은 조회 자체에서 빠지므로, 나오는 행은 전부 "아직 대체 안 된" 건이다.
     */
    List<CardTransaction> findByMember_IdAndSourceAndPaymentMethodAndUsedDateBetween(
            Long memberId, TransactionSource source, PaymentMethod paymentMethod, LocalDate start, LocalDate end);

    /**
     * 대체 짝 탐색(entryMode.md §4-2): 같은 날짜·금액의 카드 수집 거래를 정확 일치로 찾는다(fuzzy 매칭 아님,
     * §4). 짝을 못 찾으면 호출자가 NULL로 처리한다. 여러 건이면 먼저 저장된 것을 쓴다.
     */
    Optional<CardTransaction> findFirstByMember_IdAndSourceAndUsedDateAndAmountOrderByIdAsc(
            Long memberId, TransactionSource source, LocalDate usedDate, BigDecimal amount);

    /**
     * 전체 소비내역 상세 결제내역(consumeHistoryDetail.md §3.2·§5·§7.2): 급여주기 범위 페이지, 정렬은
     * usedAt DESC, id DESC로 고정한다. 거절만 제외하고 고정지출 태그 거래도 목록에는 표시하며(배지) 집계에서만
     * 별도로 제외한다. 목록 DTO 변환({@link com.weaone.themoa.domain.cardtransaction.dto.response.CardTransactionResponse#from})이
     * category·merchantAlias·merchant·card·fixedExpense 연관관계를 전부 읽으므로 N+1을 막기 위해 함께 fetch join한다.
     * fetch join + Page이므로 count 쿼리를 별도로 준다.
     */
    @Query(value = "select t from CardTransaction t "
            + "join fetch t.category "
            + "left join fetch t.merchantAlias "
            + "left join fetch t.merchant "
            + "left join fetch t.card c "
            + "left join fetch c.cardConnection cc "
            + "left join fetch cc.cardIssuer "
            + "left join fetch t.fixedExpense "
            + "where t.member.id = :memberId and t.status <> :rejected "
            + "and t.usedDate between :startDate and :endDate "
            + "order by t.usedAt desc, t.id desc",
            countQuery = "select count(t) from CardTransaction t "
                    + "where t.member.id = :memberId and t.status <> :rejected "
                    + "and t.usedDate between :startDate and :endDate")
    Page<CardTransaction> findConsumptionHistoryPage(@Param("memberId") Long memberId,
                                                       @Param("rejected") TransactionStatus rejected,
                                                       @Param("startDate") LocalDate startDate,
                                                       @Param("endDate") LocalDate endDate,
                                                       Pageable pageable);

    /**
     * 많이 쓴 곳 TOP 5(consumeHistoryDetail.md §4.4): alias -&gt; merchant -&gt; 수기 원본명(대문자·trim)
     * 우선순위로 묶는다. Native Query라 공통 조건(§2.2)의 {@code replaced_at is null}을 직접 명시한다.
     */
    @Query(value = "select "
            + "case when ct.merchant_alias_id is not null then concat('ALIAS:', ct.merchant_alias_id) "
            + "     when ct.merchant_id is not null then concat('MERCHANT:', ct.merchant_id) "
            + "     else concat('MANUAL:', upper(trim(ct.merchant_name_raw))) end as merchantKey, "
            + "coalesce(max(ma.canonical_service_name), max(m.display_name), min(ct.merchant_name_raw)) as displayName, "
            + "sum(ct.amount - coalesce(ct.canceled_amount, 0)) as netAmount, "
            + "sum(case when (ct.amount - coalesce(ct.canceled_amount, 0)) > 0 then 1 else 0 end) as transactionCount "
            + "from card_transaction ct "
            + "left join merchant_alias ma on ma.id = ct.merchant_alias_id "
            + "left join merchant m on m.id = ct.merchant_id "
            + "where ct.member_id = :memberId and ct.status <> :rejectedStatus "
            + "and ct.fixed_expense_id is null and ct.replaced_at is null "
            + "and ct.used_date between :startDate and :endDate "
            + "group by case when ct.merchant_alias_id is not null then concat('ALIAS:', ct.merchant_alias_id) "
            + "     when ct.merchant_id is not null then concat('MERCHANT:', ct.merchant_id) "
            + "     else concat('MANUAL:', upper(trim(ct.merchant_name_raw))) end "
            + "having sum(ct.amount - coalesce(ct.canceled_amount, 0)) > 0 "
            + "order by netAmount desc, merchantKey asc "
            + "limit 5", nativeQuery = true)
    List<MerchantTop5Row> findMerchantTop5(@Param("memberId") Long memberId,
                                            @Param("rejectedStatus") String rejectedStatus,
                                            @Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);

    /**
     * 습관 코칭 규칙 계층(habitExpense.md §3): 직전 급여주기 카드 자동수집 소비성 카테고리별 순액 집계.
     * 고정지출 태그·거절 건 제외, Type 2 음수행도 포함(amount>0 필터 없음).
     */
    @Query("select t.category.id as categoryId, t.category.code as categoryCode, t.category.name as categoryName, "
            + "count(t) as transactionCount, sum(t.amount - coalesce(t.canceledAmount, 0)) as netAmount "
            + "from CardTransaction t where t.member.id = :memberId and t.source = :source "
            + "and t.status <> :rejected and t.fixedExpense is null "
            + "and t.usedDate between :startDate and :endDate and t.category.code in :categoryCodes "
            + "group by t.category.id, t.category.code, t.category.name")
    List<HabitCategoryAggregate> aggregateHabitByCategory(@Param("memberId") Long memberId,
                                                            @Param("source") TransactionSource source,
                                                            @Param("rejected") TransactionStatus rejected,
                                                            @Param("startDate") LocalDate startDate,
                                                            @Param("endDate") LocalDate endDate,
                                                            @Param("categoryCodes") List<String> categoryCodes);

    /** 위와 같은 조건의 가맹점 alias 단위 집계(habitExpense.md §3) — alias 표기 흔들림은 alias 레벨에서 이미 합산됨. */
    @Query("select t.category.id as categoryId, t.merchantAlias.id as merchantAliasId, "
            + "t.merchantAlias.canonicalServiceName as aliasName, "
            + "count(t) as transactionCount, sum(t.amount - coalesce(t.canceledAmount, 0)) as netAmount "
            + "from CardTransaction t where t.member.id = :memberId and t.source = :source "
            + "and t.status <> :rejected and t.fixedExpense is null and t.merchantAlias is not null "
            + "and t.usedDate between :startDate and :endDate and t.category.code in :categoryCodes "
            + "group by t.category.id, t.merchantAlias.id, t.merchantAlias.canonicalServiceName")
    List<HabitMerchantAliasAggregate> aggregateHabitByMerchantAlias(@Param("memberId") Long memberId,
                                                                      @Param("source") TransactionSource source,
                                                                      @Param("rejected") TransactionStatus rejected,
                                                                      @Param("startDate") LocalDate startDate,
                                                                      @Param("endDate") LocalDate endDate,
                                                                      @Param("categoryCodes") List<String> categoryCodes);

    interface AliasGroupCount {
        Long getMerchantAliasId();
        long getTransactionCount();
    }

    interface MerchantGroupCount {
        Long getMerchantId();
        long getTransactionCount();
    }

    interface DailyNetAmount {
        LocalDate getUsedDate();
        BigDecimal getNetAmount();
    }

    interface MerchantTop5Row {
        String getMerchantKey();
        String getDisplayName();
        BigDecimal getNetAmount();
        long getTransactionCount();
    }

    interface CategorySummary {
        Long getCategoryId();
        String getCategoryName();
        BigDecimal getTotalAmount();
        long getTransactionCount();
    }

    interface DailyCategoryAmount {
        LocalDate getUsedDate();
        BigDecimal getTotalAmount();
    }

    interface HabitCategoryAggregate {
        Long getCategoryId();
        String getCategoryCode();
        String getCategoryName();
        long getTransactionCount();
        BigDecimal getNetAmount();
    }

    interface HabitMerchantAliasAggregate {
        Long getCategoryId();
        Long getMerchantAliasId();
        String getAliasName();
        long getTransactionCount();
        BigDecimal getNetAmount();
    }

    /**
     * 관리자 "미식별 & 기타 가맹점 작업대"(merchant.md §2-1 전역 시드 후보): 최근 N일간 전역 alias가 없는
     * 원본 가맹점을 발생 건수 상위로 준다. biller(Apple 등)는 이름으로 신원 판별이 안 되고 전역 alias 후보가
     * 아니라(§5-D) 제외한다.
     */
    @Query(value = "select ct.merchant_id as merchantId, m.merchant_name_raw as merchantNameRaw, "
            + "max(ct.merchant_type_raw) as merchantTypeRaw, count(*) as transactionCount, "
            + "avg(ct.amount) as averageAmount "
            + "from card_transaction ct "
            + "join merchant m on m.id = ct.merchant_id "
            + "where m.merchant_alias_id is null and ct.replaced_at is null and ct.used_date >= :since "
            + "and upper(trim(m.merchant_name_raw)) not in (select upper(trim(b.name)) from biller b) "
            + "group by ct.merchant_id, m.merchant_name_raw "
            + "order by count(*) desc "
            + "limit :limit", nativeQuery = true)
    List<UnclassifiedMerchantRow> findUnclassifiedMerchants(@Param("since") LocalDate since, @Param("limit") int limit);

    interface UnclassifiedMerchantRow {
        Long getMerchantId();
        String getMerchantNameRaw();
        String getMerchantTypeRaw();
        long getTransactionCount();
        BigDecimal getAverageAmount();
    }
}
