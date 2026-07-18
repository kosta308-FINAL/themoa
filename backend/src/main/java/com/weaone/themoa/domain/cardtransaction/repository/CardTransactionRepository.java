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

    /** F-05 미납 확인 후보(troubleshooting/billerProblem.md §6): 미태깅 + 금액오차 + 결제일 윈도우. */
    @Query("select t from CardTransaction t where t.member.id = :memberId and t.fixedExpense is null "
            + "and t.status <> :canceled and t.usedDate between :startDate and :endDate "
            + "and t.amount between :minAmount and :maxAmount order by t.usedDate desc")
    List<CardTransaction> findMissedPaymentCandidates(@Param("memberId") Long memberId,
                                                        @Param("canceled") TransactionStatus canceled,
                                                        @Param("startDate") LocalDate startDate,
                                                        @Param("endDate") LocalDate endDate,
                                                        @Param("minAmount") BigDecimal minAmount,
                                                        @Param("maxAmount") BigDecimal maxAmount);

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

    interface CategorySummary {
        Long getCategoryId();
        String getCategoryName();
        BigDecimal getTotalAmount();
        long getTransactionCount();
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
}
