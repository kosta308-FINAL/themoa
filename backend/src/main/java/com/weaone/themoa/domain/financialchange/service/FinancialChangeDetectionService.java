package com.weaone.themoa.domain.financialchange.service;

import com.weaone.themoa.domain.bookmark.entity.Bookmark;
import com.weaone.themoa.domain.bookmark.entity.BookmarkTargetType;
import com.weaone.themoa.domain.bookmark.repository.BookmarkRepository;
import com.weaone.themoa.domain.bookmark.service.BookmarkTargetDetail;
import com.weaone.themoa.domain.bookmark.service.BookmarkTargetReader;
import com.weaone.themoa.domain.financialchange.entity.FinancialChangeNotice;
import com.weaone.themoa.domain.financialchange.entity.FinancialWatchSnapshot;
import com.weaone.themoa.domain.financialchange.repository.FinancialChangeNoticeRepository;
import com.weaone.themoa.domain.financialchange.repository.FinancialWatchSnapshotRepository;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import com.weaone.themoa.domain.notification.entity.NotificationTypeCode;
import com.weaone.themoa.domain.notification.service.NotificationService;
import com.weaone.themoa.domain.recommend.entity.RecommendSnapshot;
import com.weaone.themoa.domain.recommend.repository.RecommendSnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 관심 상품(북마크 + 최근 추천)의 변경을 감지해 알림을 만든다.
 *
 * <p>수집 배치가 상품을 갱신한 뒤 실행된다. 상품 수집 로직 자체는 건드리지 않고, 회원별로 저장해 둔
 * 기준값(스냅샷)과 현재 상품 정보를 비교하는 방식이라 수집과 감지가 서로 얽히지 않는다.
 *
 * <p>비교 후에는 기준값을 최신으로 갱신한다. 그래서 다음 변경은 "직전 알림 이후의 변화"가 된다
 * (5%→4% 알림 후 3%가 되면 다음 알림은 4%→3%).
 *
 * <p>기준값이 아직 없는 대상(방금 북마크했거나 처음 추천받은 상품)은 기준만 만들고 알림을 보내지 않는다.
 * 그러지 않으면 등록하자마자 "변경됨" 알림이 가게 된다.
 */
@Service
public class FinancialChangeDetectionService {

    private static final Logger log = LoggerFactory.getLogger(FinancialChangeDetectionService.class);
    private static final ZoneId ZONE_SEOUL = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DEDUP_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final BookmarkRepository bookmarkRepository;
    private final RecommendSnapshotRepository recommendSnapshotRepository;
    private final FinancialWatchSnapshotRepository watchSnapshotRepository;
    private final FinancialChangeNoticeRepository noticeRepository;
    private final MemberRepository memberRepository;
    private final NotificationService notificationService;
    private final Map<BookmarkTargetType, BookmarkTargetReader> readersByType;

    public FinancialChangeDetectionService(BookmarkRepository bookmarkRepository,
                                           RecommendSnapshotRepository recommendSnapshotRepository,
                                           FinancialWatchSnapshotRepository watchSnapshotRepository,
                                           FinancialChangeNoticeRepository noticeRepository,
                                           MemberRepository memberRepository,
                                           NotificationService notificationService,
                                           List<BookmarkTargetReader> targetReaders) {
        this.bookmarkRepository = bookmarkRepository;
        this.recommendSnapshotRepository = recommendSnapshotRepository;
        this.watchSnapshotRepository = watchSnapshotRepository;
        this.noticeRepository = noticeRepository;
        this.memberRepository = memberRepository;
        this.notificationService = notificationService;
        this.readersByType = new EnumMap<>(BookmarkTargetType.class);
        for (BookmarkTargetReader reader : targetReaders) {
            this.readersByType.put(reader.supportedType(), reader);
        }
    }

    /** 관심 대상 1건(회원 + 상품). */
    private record WatchTarget(Long memberId, BookmarkTargetType targetType, Long targetId) {
    }

    /**
     * 전체 회원의 관심 상품을 훑어 변경을 감지한다.
     *
     * @return 이번 실행으로 만들어진 변경 알림 건수
     */
    @Transactional
    public int detectAll() {
        Set<WatchTarget> targets = collectTargets();
        if (targets.isEmpty()) {
            return 0;
        }

        Map<BookmarkTargetType, Map<Long, BookmarkTargetDetail>> currentByType = loadCurrentDetails(targets);
        Map<WatchTarget, FinancialWatchSnapshot> snapshots = loadSnapshots(targets);

        LocalDateTime now = LocalDateTime.now(ZONE_SEOUL);
        String today = LocalDate.now(ZONE_SEOUL).format(DEDUP_DATE);
        int created = 0;

        for (WatchTarget target : targets) {
            BookmarkTargetDetail current = currentByType
                    .getOrDefault(target.targetType(), Map.of())
                    .get(target.targetId());
            if (current == null) {
                // 상품 자체를 못 찾는 경우(수집에서 빠졌거나 아직 지원하지 않는 유형)는 건너뛴다.
                continue;
            }

            FinancialWatchSnapshot snapshot = snapshots.get(target);
            if (snapshot == null) {
                // 기준값이 없으면 이번엔 기준만 세우고 알리지 않는다(등록 직후 오알림 방지).
                watchSnapshotRepository.save(FinancialWatchSnapshot.of(target.memberId(), target.targetType(),
                        target.targetId(), current.rate(), current.specialCondition(), current.discontinued(), now));
                continue;
            }

            if (!hasChanged(snapshot, current)) {
                continue;
            }

            String dedupKey = "%s:%s:%d:%s".formatted(
                    NotificationTypeCode.FINANCIAL_PRODUCT_CHANGED.name(),
                    target.targetType().name(), target.targetId(), today);

            noticeRepository.save(FinancialChangeNotice.of(
                    target.memberId(), target.targetType(), target.targetId(),
                    current.title(), current.subtitle(),
                    snapshot.getRate(), current.rate(),
                    snapshot.getSpecialCondition(), current.specialCondition(),
                    current.discontinued(), dedupKey, now));

            notify(target.memberId(), current, snapshot, dedupKey);
            snapshot.update(current.rate(), current.specialCondition(), current.discontinued(), now);
            created++;
        }

        log.info("[관심상품 변경감지] 대상 {}건, 알림 {}건 생성", targets.size(), created);
        return created;
    }

    /** 북마크와 최근 추천 기록을 합쳐 감지 대상을 만든다(같은 상품이 겹쳐도 한 번만). */
    private Set<WatchTarget> collectTargets() {
        Set<WatchTarget> targets = new LinkedHashSet<>();
        for (Bookmark bookmark : bookmarkRepository.findAll()) {
            targets.add(new WatchTarget(bookmark.getMember().getId(),
                    bookmark.getTargetType(), bookmark.getTargetId()));
        }
        for (RecommendSnapshot snapshot : recommendSnapshotRepository.findAll()) {
            BookmarkTargetType type = parseType(snapshot.getTargetType());
            if (type != null) {
                targets.add(new WatchTarget(snapshot.getMember().getId(), type, snapshot.getTargetId()));
            }
        }
        return targets;
    }

    /** 대상 상품의 현재 정보를 유형별로 한 번에 조회한다(대상 수만큼 쿼리가 나가지 않도록). */
    private Map<BookmarkTargetType, Map<Long, BookmarkTargetDetail>> loadCurrentDetails(Set<WatchTarget> targets) {
        Map<BookmarkTargetType, Set<Long>> idsByType = new EnumMap<>(BookmarkTargetType.class);
        for (WatchTarget target : targets) {
            idsByType.computeIfAbsent(target.targetType(), key -> new LinkedHashSet<>()).add(target.targetId());
        }
        Map<BookmarkTargetType, Map<Long, BookmarkTargetDetail>> currentByType =
                new EnumMap<>(BookmarkTargetType.class);
        idsByType.forEach((type, ids) -> {
            BookmarkTargetReader reader = readersByType.get(type);
            currentByType.put(type, reader == null ? Map.of() : reader.readAll(ids));
        });
        return currentByType;
    }

    private Map<WatchTarget, FinancialWatchSnapshot> loadSnapshots(Set<WatchTarget> targets) {
        Set<Long> memberIds = new LinkedHashSet<>();
        targets.forEach(target -> memberIds.add(target.memberId()));

        Map<WatchTarget, FinancialWatchSnapshot> snapshots = new LinkedHashMap<>();
        for (Long memberId : memberIds) {
            for (FinancialWatchSnapshot snapshot : watchSnapshotRepository.findByMemberId(memberId)) {
                snapshots.put(new WatchTarget(snapshot.getMemberId(), snapshot.getTargetType(),
                        snapshot.getTargetId()), snapshot);
            }
        }
        return snapshots;
    }

    /** 금리·우대조건·판매종료 중 하나라도 달라지면 변경으로 본다(변동 폭 기준은 두지 않는다). */
    private boolean hasChanged(FinancialWatchSnapshot snapshot, BookmarkTargetDetail current) {
        boolean rateChanged = !equalsRate(snapshot.getRate(), current.rate());
        boolean conditionChanged = !Objects.equals(
                nullToEmpty(snapshot.getSpecialCondition()), nullToEmpty(current.specialCondition()));
        boolean discontinuedChanged = snapshot.isDiscontinued() != current.discontinued();
        return rateChanged || conditionChanged || discontinuedChanged;
    }

    /** BigDecimal은 3.5와 3.50이 equals로는 다르므로 값 비교(compareTo)를 쓴다. */
    private boolean equalsRate(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.compareTo(b) == 0;
    }

    private void notify(Long memberId, BookmarkTargetDetail current, FinancialWatchSnapshot snapshot,
                        String dedupKey) {
        Member member = memberRepository.getReferenceById(memberId);
        notificationService.createIfAbsent(member, NotificationTypeCode.FINANCIAL_PRODUCT_CHANGED,
                buildMessage(current, snapshot), null, null, dedupKey);
    }

    /** 가장 중요한 변화 하나를 문장으로 만든다(판매종료 > 금리 > 우대조건). */
    private String buildMessage(BookmarkTargetDetail current, FinancialWatchSnapshot snapshot) {
        String product = "%s(%s)".formatted(current.title(), current.subtitle());
        if (current.discontinued() && !snapshot.isDiscontinued()) {
            return product + " 상품이 판매종료되었어요.";
        }
        if (!equalsRate(snapshot.getRate(), current.rate())) {
            return "%s 금리가 %s → %s로 변경되었어요.".formatted(
                    product, formatRate(snapshot.getRate()), formatRate(current.rate()));
        }
        return product + " 우대조건이 변경되었어요.";
    }

    private String formatRate(BigDecimal rate) {
        return rate == null ? "정보 없음" : rate.stripTrailingZeros().toPlainString() + "%";
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private BookmarkTargetType parseType(String value) {
        try {
            return BookmarkTargetType.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
