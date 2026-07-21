package com.weaone.themoa.domain.bookmark.dto.request;

import com.weaone.themoa.domain.bookmark.entity.BookmarkTargetType;
import jakarta.validation.constraints.NotNull;

/** 북마크 등록 요청. 대상은 "타입 + id" 조합으로 특정한다(예·적금과 대출은 PK가 각각 독립적이라 id만으론 부족). */
public record BookmarkRequest(

        @NotNull(message = "북마크 대상 종류를 입력해 주세요.")
        BookmarkTargetType targetType,

        @NotNull(message = "북마크 대상을 입력해 주세요.")
        Long targetId
) {
}
