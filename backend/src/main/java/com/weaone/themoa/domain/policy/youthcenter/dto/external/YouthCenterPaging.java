package com.weaone.themoa.domain.policy.youthcenter.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record YouthCenterPaging(
        Integer totCount,
        Integer pageNum,
        Integer pageSize
) {
}
