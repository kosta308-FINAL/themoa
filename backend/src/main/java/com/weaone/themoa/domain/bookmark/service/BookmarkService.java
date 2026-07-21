package com.weaone.themoa.domain.bookmark.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.bookmark.dto.request.BookmarkRequest;
import com.weaone.themoa.domain.bookmark.dto.response.BookmarkResponse;
import com.weaone.themoa.domain.bookmark.dto.response.BookmarkTargetResponse;
import com.weaone.themoa.domain.bookmark.entity.Bookmark;
import com.weaone.themoa.domain.bookmark.entity.BookmarkTargetType;
import com.weaone.themoa.domain.bookmark.repository.BookmarkRepository;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 북마크 등록·해제·조회.
 *
 * <p>대상 종류별 상세 조회는 {@link BookmarkTargetReader} 구현체들에 위임한다. 새로운 대상(정책 등)이
 * 생겨도 이 클래스는 수정할 필요가 없다.
 */
@Service
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final MemberRepository memberRepository;
    private final Map<BookmarkTargetType, BookmarkTargetReader> readersByType;

    public BookmarkService(BookmarkRepository bookmarkRepository,
                           MemberRepository memberRepository,
                           List<BookmarkTargetReader> targetReaders) {
        this.bookmarkRepository = bookmarkRepository;
        this.memberRepository = memberRepository;
        this.readersByType = new EnumMap<>(BookmarkTargetType.class);
        for (BookmarkTargetReader reader : targetReaders) {
            this.readersByType.put(reader.supportedType(), reader);
        }
    }

    /**
     * 북마크 등록. 이미 저장된 대상이면 아무것도 하지 않고 false를 반환한다(별표 토글에서 중복 요청이
     * 들어와도 화면에 오류가 뜨지 않도록 멱등하게 처리).
     *
     * @return 이번 호출로 새로 저장했으면 true
     */
    @Transactional
    public boolean register(Long memberId, BookmarkRequest request) {
        if (bookmarkRepository.existsByMemberIdAndTargetTypeAndTargetId(
                memberId, request.targetType(), request.targetId())) {
            return false;
        }
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        try {
            bookmarkRepository.save(Bookmark.of(member, request.targetType(), request.targetId()));
            return true;
        } catch (DataIntegrityViolationException e) {
            // 별표 연타 등으로 거의 동시에 두 번 들어오면 위 선조회를 둘 다 통과할 수 있다.
            // 그 경우 UNIQUE 제약이 최종적으로 막아주며, 결과적으로 "이미 저장됨"과 같으므로 성공으로 본다.
            return false;
        }
    }

    /** 북마크 해제. 저장돼 있지 않은 대상이어도 오류 없이 넘어간다(멱등). */
    @Transactional
    public void remove(Long memberId, BookmarkTargetType targetType, Long targetId) {
        bookmarkRepository.deleteByMemberIdAndTargetTypeAndTargetId(memberId, targetType, targetId);
    }

    /** 마이페이지 목록. 대상 종류별로 묶어 한 번씩만 조회한다(북마크 개수만큼 쿼리가 나가지 않도록). */
    @Transactional(readOnly = true)
    public List<BookmarkResponse> findAll(Long memberId) {
        List<Bookmark> bookmarks = bookmarkRepository.findByMemberIdOrderByCreatedAtDesc(memberId);
        if (bookmarks.isEmpty()) {
            return List.of();
        }

        Map<BookmarkTargetType, Set<Long>> idsByType = new EnumMap<>(BookmarkTargetType.class);
        for (Bookmark bookmark : bookmarks) {
            idsByType.computeIfAbsent(bookmark.getTargetType(), key -> new LinkedHashSet<>())
                    .add(bookmark.getTargetId());
        }

        Map<BookmarkTargetType, Map<Long, BookmarkTargetDetail>> detailsByType =
                new EnumMap<>(BookmarkTargetType.class);
        idsByType.forEach((type, ids) -> {
            BookmarkTargetReader reader = readersByType.get(type);
            detailsByType.put(type, reader == null ? Map.of() : reader.readAll(ids));
        });

        List<BookmarkResponse> responses = new ArrayList<>(bookmarks.size());
        for (Bookmark bookmark : bookmarks) {
            BookmarkTargetDetail detail = detailsByType
                    .getOrDefault(bookmark.getTargetType(), Map.of())
                    .get(bookmark.getTargetId());
            if (detail == null) {
                // 대상이 사라졌거나 아직 지원하지 않는 종류면 목록에서 제외한다(빈 카드가 보이지 않도록).
                continue;
            }
            responses.add(new BookmarkResponse(
                    bookmark.getId(),
                    bookmark.getTargetType(),
                    bookmark.getTargetId(),
                    detail.title(),
                    detail.subtitle(),
                    detail.rate(),
                    detail.termMonth(),
                    bookmark.getCreatedAt()));
        }
        return responses;
    }

    /** 검색·추천 결과에서 별표를 채울지 판단할 때 쓰는 가벼운 목록(상품 테이블을 조회하지 않는다). */
    @Transactional(readOnly = true)
    public List<BookmarkTargetResponse> findTargets(Long memberId) {
        return bookmarkRepository.findByMemberIdOrderByCreatedAtDesc(memberId).stream()
                .map(bookmark -> new BookmarkTargetResponse(bookmark.getTargetType(), bookmark.getTargetId()))
                .toList();
    }
}
