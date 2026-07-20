package com.weaone.themoa.domain.policy.policy.region;

import java.util.List;

public record UserRegionContext(
        UserRegionResolution residence,
        UserRegionResolution workplace,
        List<UserRegionMention> mentions
) {
    public UserRegionContext {
        residence = residence == null ? UserRegionResolution.notFound() : residence;
        workplace = workplace == null ? UserRegionResolution.notFound() : workplace;
        mentions = mentions == null ? List.of() : List.copyOf(mentions);
    }

    public static UserRegionContext empty() {
        return new UserRegionContext(UserRegionResolution.notFound(), UserRegionResolution.notFound(), List.of());
    }
}
