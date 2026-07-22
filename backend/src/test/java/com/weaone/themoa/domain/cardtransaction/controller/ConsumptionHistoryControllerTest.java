package com.weaone.themoa.domain.cardtransaction.controller;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.common.exception.GlobalExceptionHandler;
import com.weaone.themoa.common.logging.ErrorLogSanitizer;
import com.weaone.themoa.domain.cardtransaction.dto.response.CardTransactionListResponse;
import com.weaone.themoa.domain.cardtransaction.dto.response.CardTransactionResponse;
import com.weaone.themoa.domain.cardtransaction.dto.response.ConsumptionHistorySummaryResponse;
import com.weaone.themoa.domain.cardtransaction.dto.response.ConsumptionHistorySummaryResponse.CycleInfo;
import com.weaone.themoa.domain.cardtransaction.service.ConsumptionHistoryService;
import com.weaone.themoa.domain.logging.service.AsyncErrorLogRecorder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HTTP 계약 검증(consumeHistoryDetail.md §10.3). Security 필터 없이 컨트롤러+전역 예외 처리기만 조립한다. */
@ExtendWith(MockitoExtension.class)
class ConsumptionHistoryControllerTest {

    private static final Long MEMBER_ID = 1L;

    @Mock
    private ConsumptionHistoryService consumptionHistoryService;

    @Mock
    private AsyncErrorLogRecorder asyncErrorLogRecorder;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ConsumptionHistoryController controller = new ConsumptionHistoryController(consumptionHistoryService);
        GlobalExceptionHandler globalExceptionHandler =
                new GlobalExceptionHandler(new ErrorLogSanitizer(), asyncErrorLogRecorder);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(globalExceptionHandler)
                .setCustomArgumentResolvers(memberIdResolver())
                .build();
    }

    private HandlerMethodArgumentResolver memberIdResolver() {
        return new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                        && parameter.getParameterType().equals(Long.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                           NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return MEMBER_ID;
            }
        };
    }

    private ConsumptionHistorySummaryResponse summaryResponse() {
        CycleInfo cycle = new CycleInfo(31L, "2026-07", LocalDate.of(2026, 7, 5), LocalDate.of(2026, 8, 4),
                LocalDate.of(2026, 7, 15), "IN_PROGRESS", 30L, null);
        return new ConsumptionHistorySummaryResponse(cycle, BigDecimal.valueOf(1_026_650), BigDecimal.valueOf(17_000),
                null, List.of(), List.of());
    }

    @Test
    @DisplayName("budgetId 생략 시 현재 급여 주기 요약을 200으로 반환한다")
    void summaryOmittingBudgetIdReturnsCurrentCycle() throws Exception {
        given(consumptionHistoryService.getSummary(MEMBER_ID, null)).willReturn(summaryResponse());

        mockMvc.perform(get("/api/spending-guide/consumption-history/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.cycle.budgetId").value(31))
                .andExpect(jsonPath("$.data.cycle.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.cycleNetAmount").value(1026650));
    }

    @Test
    @DisplayName("budgetId를 지정하면 서비스에 그대로 전달한다")
    void summaryWithBudgetIdPassesThrough() throws Exception {
        given(consumptionHistoryService.getSummary(MEMBER_ID, 31L)).willReturn(summaryResponse());

        mockMvc.perform(get("/api/spending-guide/consumption-history/summary").param("budgetId", "31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cycle.budgetId").value(31));
    }

    @Test
    @DisplayName("타인 소유이거나 존재하지 않는 budgetId는 404와 BUDGET_NOT_FOUND를 반환한다")
    void summaryRejectsUnknownBudget() throws Exception {
        willThrow(new BusinessException(ErrorCode.BUDGET_NOT_FOUND))
                .given(consumptionHistoryService).getSummary(eq(MEMBER_ID), eq(99L));

        mockMvc.perform(get("/api/spending-guide/consumption-history/summary").param("budgetId", "99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BUDGET_NOT_FOUND"));
    }

    @Test
    @DisplayName("결제내역 기본값은 page=0, size=10이다")
    void transactionsDefaultsToFirstPageOfTen() throws Exception {
        Page<CardTransactionResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        given(consumptionHistoryService.getTransactions(eq(MEMBER_ID), isNull(), eq(0), eq(10)))
                .willReturn(CardTransactionListResponse.from(page));

        mockMvc.perform(get("/api/spending-guide/consumption-history/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(10));
    }

    @Test
    @DisplayName("size가 30을 넘거나 page가 음수면 400과 INVALID_INPUT을 반환한다")
    void transactionsRejectsOutOfRangeParams() throws Exception {
        willThrow(new BusinessException(ErrorCode.INVALID_INPUT))
                .given(consumptionHistoryService).getTransactions(eq(MEMBER_ID), isNull(), anyInt(), eq(31));

        mockMvc.perform(get("/api/spending-guide/consumption-history/transactions").param("size", "31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }
}
