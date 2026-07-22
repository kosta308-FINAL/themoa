package com.weaone.themoa.domain.logging.service;

import com.weaone.themoa.domain.logging.dto.UnexpectedErrorEvent;
import com.weaone.themoa.domain.logging.entity.ErrorLog;
import com.weaone.themoa.domain.logging.repository.ErrorLogRepository;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 새 트랜잭션으로 {@code error_log} INSERT(managelogging.md §3-4). 요청 스레드와 분리된 실행기에서 호출된다. */
@Service
@RequiredArgsConstructor
public class ErrorLogPersistenceService {

    private final ErrorLogRepository errorLogRepository;
    private final MemberRepository memberRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(UnexpectedErrorEvent event) {
        Member member = event.memberId() == null ? null : memberRepository.getReferenceById(event.memberId());
        ErrorLog errorLog = ErrorLog.create(
                event.traceId(),
                member,
                event.httpMethod(),
                event.requestUri(),
                event.controller(),
                event.statusCode(),
                event.exceptionClass(),
                event.errorMessage(),
                event.stackTraceExcerpt(),
                event.occurredAt()
        );
        errorLogRepository.save(errorLog);
    }
}
