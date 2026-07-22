package com.weaone.themoa.common.logging;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

/**
 * 요청을 처리한 컨트롤러를 로그에 남기기 위한 헬퍼(managelogging.md §2-1). {@code MdcLoggingFilter}와
 * {@code GlobalExceptionHandler}가 공유한다.
 */
public final class ControllerNameResolver {

    public static final String UNMATCHED = "unmatched";

    private ControllerNameResolver() {
    }

    /**
     * DispatcherServlet이 라우팅에 성공하면 {@code request}에 매칭된 {@link HandlerMethod}를 담아둔다.
     * 라우팅 전에 응답이 끝난 요청(예: JWT 필터에서 바로 끝난 401)이나 매칭 실패(404)는 UNMATCHED다.
     */
    public static String resolve(HttpServletRequest request) {
        Object handler = request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);
        if (handler instanceof HandlerMethod handlerMethod) {
            return handlerMethod.getBeanType().getSimpleName() + "." + handlerMethod.getMethod().getName();
        }
        return UNMATCHED;
    }
}
