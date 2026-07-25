package com.weaone.themoa.domain.customerservice.repository;

import com.weaone.themoa.domain.customerservice.entity.CustomerServiceUnansweredQuestion;
import com.weaone.themoa.domain.customerservice.entity.UnansweredQuestionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerServiceUnansweredQuestionRepository
        extends JpaRepository<CustomerServiceUnansweredQuestion, Long> {

    /** 관리자 목록: NEW 우선, 그 안에서 최신순. */
    @Query("""
            select q from CustomerServiceUnansweredQuestion q
            where (:status is null or q.status = :status)
            order by case when q.status = com.weaone.themoa.domain.customerservice.entity.UnansweredQuestionStatus.NEW then 0 else 1 end asc,
                     q.createdAt desc, q.id desc
            """)
    Page<CustomerServiceUnansweredQuestion> searchForAdmin(@Param("status") UnansweredQuestionStatus status,
                                                             Pageable pageable);

    long countByStatus(UnansweredQuestionStatus status);
}
