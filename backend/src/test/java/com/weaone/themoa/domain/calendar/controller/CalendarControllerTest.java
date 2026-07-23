package com.weaone.themoa.domain.calendar.controller;

import com.weaone.themoa.domain.calendar.dto.request.CalendarScheduleCreateRequest;
import com.weaone.themoa.domain.calendar.dto.request.CalendarScheduleUpdateRequest;
import com.weaone.themoa.domain.calendar.dto.response.CalendarEventListResponse;
import com.weaone.themoa.domain.calendar.dto.response.CalendarEventResponse;
import com.weaone.themoa.domain.calendar.dto.response.CalendarScheduleResponse;
import com.weaone.themoa.domain.calendar.entity.CalendarEventType;
import com.weaone.themoa.domain.calendar.service.CalendarQueryService;
import com.weaone.themoa.domain.calendar.service.CalendarScheduleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CalendarControllerTest {

    @Test
    void eventsReturnsApiResponse() throws Exception {
        CalendarQueryService queryService = mock(CalendarQueryService.class);
        CalendarScheduleService scheduleService = mock(CalendarScheduleService.class);
        LocalDate startDate = LocalDate.of(2026, 8, 1);
        LocalDate endDate = LocalDate.of(2026, 8, 31);
        given(queryService.getEvents(7L, startDate, endDate)).willReturn(new CalendarEventListResponse(
                startDate,
                endDate,
                List.of(new CalendarEventResponse(
                        "USER_SCHEDULE:3",
                        CalendarEventType.USER_SCHEDULE,
                        LocalDate.of(2026, 8, 5),
                        "지원서 제출",
                        null,
                        3L,
                        true
                ))
        ));

        mockMvc(queryService, scheduleService)
                .perform(get("/api/calendar/events")
                        .param("startDate", "2026-08-01")
                        .param("endDate", "2026-08-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].eventType").value("USER_SCHEDULE"))
                .andExpect(jsonPath("$.data.items[0].editable").value(true));
    }

    @Test
    void createReturnsCreated() throws Exception {
        CalendarQueryService queryService = mock(CalendarQueryService.class);
        CalendarScheduleService scheduleService = mock(CalendarScheduleService.class);
        CalendarScheduleResponse response = response();
        given(scheduleService.create(eq(7L), eq(new CalendarScheduleCreateRequest(
                "지원서 제출",
                LocalDate.of(2026, 8, 5)
        )))).willReturn(response);

        mockMvc(queryService, scheduleService)
                .perform(post("/api/calendar/schedules")
                        .contentType("application/json")
                        .content("""
                                {
                                  "title": "지원서 제출",
                                  "scheduleDate": "2026-08-05"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("지원서 제출"));
    }

    @Test
    void updateReturnsUpdatedSchedule() throws Exception {
        CalendarQueryService queryService = mock(CalendarQueryService.class);
        CalendarScheduleService scheduleService = mock(CalendarScheduleService.class);
        CalendarScheduleResponse response = new CalendarScheduleResponse(
                3L,
                "지원서 최종 제출",
                LocalDate.of(2026, 8, 6),
                LocalDateTime.of(2026, 8, 1, 10, 0),
                LocalDateTime.of(2026, 8, 2, 10, 0)
        );
        given(scheduleService.update(eq(7L), eq(3L), eq(new CalendarScheduleUpdateRequest(
                "지원서 최종 제출",
                LocalDate.of(2026, 8, 6)
        )))).willReturn(response);

        mockMvc(queryService, scheduleService)
                .perform(patch("/api/calendar/schedules/3")
                        .contentType("application/json")
                        .content("""
                                {
                                  "title": "지원서 최종 제출",
                                  "scheduleDate": "2026-08-06"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(3))
                .andExpect(jsonPath("$.data.scheduleDate").value("2026-08-06"));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        CalendarQueryService queryService = mock(CalendarQueryService.class);
        CalendarScheduleService scheduleService = mock(CalendarScheduleService.class);

        mockMvc(queryService, scheduleService)
                .perform(delete("/api/calendar/schedules/3"))
                .andExpect(status().isNoContent());

        verify(scheduleService).delete(7L, 3L);
    }

    @Test
    void controllerDependsOnlyOnServices() {
        boolean hasRepositoryField = List.of(CalendarController.class.getDeclaredFields()).stream()
                .anyMatch(field -> field.getType().getSimpleName().endsWith("Repository"));

        assertThat(hasRepositoryField).isFalse();
    }

    private MockMvc mockMvc(CalendarQueryService queryService, CalendarScheduleService scheduleService) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return MockMvcBuilders
                .standaloneSetup(new CalendarController(queryService, scheduleService))
                .setCustomArgumentResolvers(new TestAuthenticationPrincipalResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    private CalendarScheduleResponse response() {
        return new CalendarScheduleResponse(
                3L,
                "지원서 제출",
                LocalDate.of(2026, 8, 5),
                LocalDateTime.of(2026, 8, 1, 10, 0),
                LocalDateTime.of(2026, 8, 1, 10, 0)
        );
    }

    private static class TestAuthenticationPrincipalResolver implements HandlerMethodArgumentResolver {
        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                    && parameter.getParameterType().equals(Long.class);
        }

        @Override
        public Object resolveArgument(MethodParameter parameter,
                                      ModelAndViewContainer mavContainer,
                                      NativeWebRequest webRequest,
                                      WebDataBinderFactory binderFactory) {
            return 7L;
        }
    }
}
