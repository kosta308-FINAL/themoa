package com.weaone.themoa.domain.policy.admin.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;

import static org.assertj.core.api.Assertions.assertThat;

class AdminControllerConditionTest {

    @Test
    @DisplayName("AdminController는 local profile에서만 등록되도록 제한되어 있다")
    void adminControllerRequiresLocalProfile() {
        Profile profile = AdminController.class.getAnnotation(Profile.class);

        assertThat(profile).isNotNull();
        assertThat(profile.value()).containsExactly("local");
    }

    @Test
    @DisplayName("AdminController는 app.policy.local-tools.enabled=true일 때만 등록되도록 제한되어 있다")
    void adminControllerRequiresLocalToolsEnabled() {
        ConditionalOnProperty conditional = AdminController.class.getAnnotation(ConditionalOnProperty.class);

        assertThat(conditional).isNotNull();
        assertThat(conditional.prefix()).isEqualTo("app.policy.local-tools");
        assertThat(conditional.name()).containsExactly("enabled");
        assertThat(conditional.havingValue()).isEqualTo("true");
    }
}
