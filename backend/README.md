# Themoa Backend

Spring Boot 기반 백엔드 프로젝트입니다.

## 기술 스택

- **Java 17**
- **Spring Boot 3.5.16**
- (추가 예정) Spring Security, Spring Data JPA, Springdoc(Swagger)

## 패키지 구조

도메인이 많아질 것을 고려해 도메인 우선(package-by-feature) 구조로 갑니다. 각 도메인 안에서 다시 레이어(controller/service/repository/dto/entity)로 나눕니다.

```
com.weaone.themoa
├── ThemoaApplication.java
│
├── common/                          # 도메인 무관 공통 코드
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java   # @RestControllerAdvice로 예외 일괄 처리
│   │   ├── CustomException.java          # 비즈니스 예외 베이스 클래스
│   │   └── ErrorCode.java                # 에러코드/메시지 enum
│   ├── response/
│   │   ├── ApiResponse.java              # 공통 성공 응답 포맷
│   │   └── ErrorResponse.java            # 공통 에러 응답 포맷
│   └── util/                             # 날짜/문자열 등 공용 유틸 (필요해지면 추가)
│
├── config/                          # 순수 설정 클래스만 위치 (CORS 포함 인증 관련 설정은 SecurityConfig)
│   ├── SecurityConfig.java               # SecurityFilterChain, CORS 설정
│   ├── SwaggerConfig.java                # OpenAPI(springdoc) 설정
│   ├── WebConfig.java                    # interceptor, argument resolver 등 (필요해지면 추가)
│   └── JpaConfig.java                    # Auditing, QueryDSL 등
│
├── security/                        # 인증/인가 실제 로직 (config와 분리)
│   └── jwt/
│       ├── JwtTokenProvider.java         # 토큰 발급/검증
│       └── JwtAuthenticationFilter.java  # 인증 필터
│
├── aop/                             # AOP 관련
│   └── logging/
│       └── LoggingAspect.java            # 요청/응답, 실행시간 등 로깅
│
└── domain/                          # 도메인별 패키지
    └── user/
        ├── controller
        ├── service
        ├── repository
        ├── dto/
        │   ├── request
        │   └── response
        └── entity
```

## 폴더별 규칙

- **common/exception**: 커스텀 예외와 전역 예외 핸들러를 모아둡니다. 도메인 서비스에서는 `CustomException` + `ErrorCode`로 예외를 던지고, 응답 변환은 `GlobalExceptionHandler`에서 처리합니다.
- **common/response**: 컨트롤러가 반환하는 공통 응답 포맷(성공/실패)을 정의합니다.
- **config**: 빈 등록/설정만 담당하는 클래스만 둡니다. 실제 인증 로직(JWT 등)은 `security` 패키지로 분리합니다. CORS는 Security 필터체인과의 순서 문제 때문에 `WebConfig`가 아닌 `SecurityConfig`에서 설정합니다.
- **security**: JWT 토큰 발급/검증, 인증 필터 등 인증·인가와 관련된 실제 로직 클래스를 둡니다.
- **aop**: `@Aspect` 기반 클래스를 목적별 하위 패키지(`logging` 등)로 분리해 둡니다.
- **domain/{도메인명}**: 도메인별로 controller/service/repository/dto/entity를 함께 둡니다.
  - **dto/request, dto/response**: 요청/응답 DTO가 많아질 것을 고려해 처음부터 분리합니다.