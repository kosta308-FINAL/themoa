package com.weaone.themoa.domain.policy.rag.dto;

import com.weaone.themoa.domain.member.entity.Gender;

public enum UserGender {
    MALE,
    FEMALE;

    public static UserGender fromMemberGender(Gender gender) {
        if (gender == null) {
            return null;
        }
        return switch (gender) {
            case MALE -> MALE;
            case FEMALE -> FEMALE;
        };
    }
}
