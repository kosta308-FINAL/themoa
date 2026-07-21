package com.weaone.themoa.domain.merchant.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** 중복 생성된 서비스(MerchantAlias)들을 하나로 합친다. {@code sourceAliasIds}에 담긴 서비스가 없어지고
 * 그 아래 데이터가 전부 {@code targetAliasId}로 옮겨진다. */
public record MerchantAliasMergeRequest(
        @NotNull Long targetAliasId,
        @NotEmpty List<Long> sourceAliasIds
) {
}
