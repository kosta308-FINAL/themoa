package com.weaone.themoa.config;

import com.weaone.themoa.common.logging.MdcLoggingFilter;
import com.weaone.themoa.security.handler.JwtAccessDeniedHandler;
import com.weaone.themoa.security.handler.JwtAuthenticationEntryPoint;
import com.weaone.themoa.security.jwt.JwtAuthenticationFilter;
import com.weaone.themoa.security.oauth.HttpCookieOAuth2AuthorizationRequestRepository;
import com.weaone.themoa.security.oauth.KakaoLoginFailureHandler;
import com.weaone.themoa.security.oauth.KakaoLoginSuccessHandler;
import com.weaone.themoa.security.oauth.KakaoOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /** 인증 없이 열어 두는 경로. 여기 없는 경로는 전부 인증이 필요하다. */
    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/auth/signup",
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/logout",
            "/api/auth/email/code",
            "/api/auth/email/code/verify",
            "/api/auth/oauth/exchange",
            "/api/auth/oauth/kakao/complete-signup"
    };

    /**
     * OAuth2Login이 처리하는 GET 경로. 기본값(/oauth2/authorization/**, /login/oauth2/code/**)을
     * /api 하위로 옮겼다 — ALB 리스너 규칙이 "/api/*"만 Spring EC2로 보내기 때문이다
     * (distribution/distributionSetting.md §10.2).
     */
    private static final String[] OAUTH_ENDPOINTS = {
            "/api/oauth2/authorization/**",
            "/api/login/oauth2/code/**"
    };

    private static final String[] DOCS_ENDPOINTS = {
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    private static final String[] HEALTH_ENDPOINTS = {
            "/api/health"
    };

    private static final RegexRequestMatcher POLICY_DETAIL_ENDPOINT =
            new RegexRequestMatcher("^/api/policies/[0-9]+$", HttpMethod.GET.name());
    private static final RegexRequestMatcher POLICY_ADMIN_ENDPOINT =
            new RegexRequestMatcher("^/api/policies/admin(/.*)?$", null);

    private final MdcLoggingFilter mdcLoggingFilter;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final Environment environment;
    private final KakaoOAuth2UserService kakaoOAuth2UserService;
    private final KakaoLoginSuccessHandler kakaoLoginSuccessHandler;
    private final KakaoLoginFailureHandler kakaoLoginFailureHandler;
    private final HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 인증은 Authorization 헤더(Bearer)로, Refresh 쿠키는 SameSite=Strict라 cross-site 요청에 실리지 않는다.
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(request -> corsConfiguration()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(HttpMethod.GET, HEALTH_ENDPOINTS).permitAll();
                    auth.requestMatchers(HttpMethod.POST, PUBLIC_ENDPOINTS).permitAll();
                    auth.requestMatchers(HttpMethod.GET, OAUTH_ENDPOINTS).permitAll();
                    auth.requestMatchers(DOCS_ENDPOINTS).permitAll();
                    auth.requestMatchers(POLICY_ADMIN_ENDPOINT).hasRole("ADMIN");
                    if (isPolicyLocalToolsEnabled()) {
                        auth.requestMatchers(HttpMethod.POST, "/api/policies/search").permitAll();
                        auth.requestMatchers(POLICY_DETAIL_ENDPOINT).permitAll();
                    }
                    auth.requestMatchers("/api/admin/**").hasRole("ADMIN");
                    // PolicyAdmin* 컨트롤러가 전부 /api/policies/admin 아래 매핑되어 있는데
                    // "/api/admin/**" 패턴은 이 경로를 포함하지 않는다 — 명시적으로 한 번 더 막는다.
                    auth.requestMatchers("/api/policies/admin/**").hasRole("ADMIN");
                    auth.requestMatchers(HttpMethod.POST, "/api/financial-products/embeddings/rebuild")
                            .hasRole("ADMIN");
                    auth.anyRequest().authenticated();
                })
                .exceptionHandling(handler -> handler
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler)
                )
                // 카카오 로그인(auth.md §6). 인가 URL 생성·code 교환은 Spring이 처리하고, 그 뒤 커스텀
                // 분기(기존 회원 로그인 vs 신규가입)는 KakaoLoginSuccessHandler가 이어받는다.
                // state는 HttpSession이 아니라 쿠키에 둔다(authorizationRequestRepository) — 이 프로젝트의
                // 인증은 완전히 stateless가 원칙이라 핸드셰이크 동안도 서버 세션을 만들지 않는다.
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(endpoint -> endpoint
                                .baseUri("/api/oauth2/authorization")
                                .authorizationRequestRepository(authorizationRequestRepository))
                        .redirectionEndpoint(endpoint -> endpoint.baseUri("/api/login/oauth2/code/*"))
                        .userInfoEndpoint(endpoint -> endpoint.userService(kakaoOAuth2UserService))
                        .successHandler(kakaoLoginSuccessHandler)
                        .failureHandler(kakaoLoginFailureHandler)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(mdcLoggingFilter, JwtAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterRegistration() {
        FilterRegistrationBean<JwtAuthenticationFilter> registration =
                new FilterRegistrationBean<>(jwtAuthenticationFilter);
        registration.setEnabled(false);
        return registration;
    }

    /** Spring Security 체인 안에서만 실행되도록, 서블릿 컨테이너의 일반 필터 등록은 막는다(traceId가 요청당 한 번만 생성되게). */
    @Bean
    FilterRegistrationBean<MdcLoggingFilter> mdcLoggingFilterRegistration() {
        FilterRegistrationBean<MdcLoggingFilter> registration = new FilterRegistrationBean<>(mdcLoggingFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private boolean isPolicyLocalToolsEnabled() {
        return environment.acceptsProfiles(Profiles.of("local"))
                && environment.getProperty("app.policy.local-tools.enabled", Boolean.class, false);
    }

    private List<String> corsAllowedOrigins() {
        String configuredOrigins = environment.getProperty("app.cors.allowed-origins", "");
        if (configuredOrigins.isBlank() && environment.acceptsProfiles(Profiles.of("local"))) {
            configuredOrigins = "http://localhost:5173";
        }
        return Arrays.stream(configuredOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
    }

    private CorsConfiguration corsConfiguration() {
        List<String> allowedOrigins = corsAllowedOrigins();
        if (allowedOrigins.isEmpty()) {
            return null;
        }

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        return config;
    }
}
