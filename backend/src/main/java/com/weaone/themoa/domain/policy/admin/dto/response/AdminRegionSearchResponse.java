package com.weaone.themoa.domain.policy.admin.dto.response;

import java.util.List;

public record AdminRegionSearchResponse(
        Integer regionId,
        String internalCode,
        String province,
        String city,
        String level,
        List<AdminExternalCodeResponse> externalCodes
) {
}
