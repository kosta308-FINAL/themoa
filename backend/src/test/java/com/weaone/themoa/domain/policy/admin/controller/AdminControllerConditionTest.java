package com.weaone.themoa.domain.policy.admin.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AdminControllerConditionTest {
    private final List<Class<?>> controllers = List.of(
            PolicyAdminDashboardController.class,
            PolicyAdminJobController.class,
            PolicyAdminEmbeddingController.class,
            PolicyAdminSearchController.class,
            PolicyAdminRegionController.class,
            PolicyAdminRawController.class
    );

    @Test
    @DisplayName("정책 관리자 Controller는 app.policy.admin-tools.enabled=true일 때만 등록되도록 제한되어 있다")
    void adminControllersRequireAdminToolsEnabled() {
        for (Class<?> controller : controllers) {
            ConditionalOnProperty conditional = controller.getAnnotation(ConditionalOnProperty.class);

            assertThat(conditional).isNotNull();
            assertThat(conditional.prefix()).isEqualTo("app.policy.admin-tools");
            assertThat(conditional.name()).containsExactly("enabled");
            assertThat(conditional.havingValue()).isEqualTo("true");
        }
    }

    @Test
    @DisplayName("admin-tools enabled=true이면 정책 관리자 Controller Bean이 등록된다")
    void adminControllerBeansAreRegisteredWhenAdminToolsEnabled() {
        adminContext()
                .withPropertyValues("app.policy.admin-tools.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(PolicyAdminDashboardController.class);
                    assertThat(context).hasSingleBean(PolicyAdminJobController.class);
                    assertThat(context).hasSingleBean(PolicyAdminEmbeddingController.class);
                    assertThat(context).hasSingleBean(PolicyAdminSearchController.class);
                    assertThat(context).hasSingleBean(PolicyAdminRegionController.class);
                    assertThat(context).hasSingleBean(PolicyAdminRawController.class);
                });
    }

    @Test
    @DisplayName("admin-tools enabled=false이면 정책 관리자 Controller Bean이 등록되지 않는다")
    void adminControllerBeansAreNotRegisteredWhenAdminToolsDisabled() {
        adminContext()
                .withPropertyValues("app.policy.admin-tools.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(PolicyAdminDashboardController.class));
    }

    @Test
    @DisplayName("admin-tools 설정이 없으면 정책 관리자 Controller Bean이 등록되지 않는다")
    void adminControllerBeansAreNotRegisteredWhenMissingAdminToolsProperty() {
        adminContext()
                .run(context -> assertThat(context).doesNotHaveBean(PolicyAdminDashboardController.class));
    }

    private ApplicationContextRunner adminContext() {
        return new ApplicationContextRunner().withUserConfiguration(AdminControllerTestConfig.class);
    }

    @TestConfiguration
    @Import({
            PolicyAdminDashboardController.class,
            PolicyAdminJobController.class,
            PolicyAdminEmbeddingController.class,
            PolicyAdminSearchController.class,
            PolicyAdminRegionController.class,
            PolicyAdminRawController.class
    })
    static class AdminControllerTestConfig {
        @Bean
        com.weaone.themoa.domain.policy.admin.service.AdminStatusService adminStatusService() {
            return mock(com.weaone.themoa.domain.policy.admin.service.AdminStatusService.class);
        }

        @Bean
        com.weaone.themoa.domain.policy.admin.service.AdminDashboardFacade adminDashboardFacade() {
            return mock(com.weaone.themoa.domain.policy.admin.service.AdminDashboardFacade.class);
        }

        @Bean
        com.weaone.themoa.domain.policy.admin.service.AdminJobService adminJobService() {
            return mock(com.weaone.themoa.domain.policy.admin.service.AdminJobService.class);
        }

        @Bean
        com.weaone.themoa.domain.policy.admin.service.AdminEmbeddingReadService adminEmbeddingReadService() {
            return mock(com.weaone.themoa.domain.policy.admin.service.AdminEmbeddingReadService.class);
        }

        @Bean
        com.weaone.themoa.domain.policy.youthcenter.service.YouthCenterDiagnosticService youthCenterDiagnosticService() {
            return mock(com.weaone.themoa.domain.policy.youthcenter.service.YouthCenterDiagnosticService.class);
        }

        @Bean
        com.weaone.themoa.domain.policy.admin.service.AdminSearchIndexService adminSearchIndexService() {
            return mock(com.weaone.themoa.domain.policy.admin.service.AdminSearchIndexService.class);
        }

        @Bean
        com.weaone.themoa.domain.policy.admin.service.AdminSearchDiagnosticService adminSearchDiagnosticService() {
            return mock(com.weaone.themoa.domain.policy.admin.service.AdminSearchDiagnosticService.class);
        }

        @Bean
        com.weaone.themoa.domain.policy.admin.service.AdminRegionDiagnosticsService adminRegionDiagnosticsService() {
            return mock(com.weaone.themoa.domain.policy.admin.service.AdminRegionDiagnosticsService.class);
        }

        @Bean
        com.weaone.themoa.domain.policy.admin.service.AdminRegionQueryService adminRegionQueryService() {
            return mock(com.weaone.themoa.domain.policy.admin.service.AdminRegionQueryService.class);
        }

        @Bean
        com.weaone.themoa.domain.policy.admin.service.AdminPolicyRawService adminPolicyRawService() {
            return mock(com.weaone.themoa.domain.policy.admin.service.AdminPolicyRawService.class);
        }
    }
}
