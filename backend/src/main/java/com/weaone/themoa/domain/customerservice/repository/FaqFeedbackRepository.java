package com.weaone.themoa.domain.customerservice.repository;

import com.weaone.themoa.domain.customerservice.dto.response.FaqFeedbackCountRow;
import com.weaone.themoa.domain.customerservice.entity.FaqFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FaqFeedbackRepository extends JpaRepository<FaqFeedback, Long> {

    Optional<FaqFeedback> findByFaq_IdAndMember_Id(Long faqId, Long memberId);

    List<FaqFeedback> findByFaq_IdInAndMember_Id(List<Long> faqIds, Long memberId);

    @Query("""
            select new com.weaone.themoa.domain.customerservice.dto.response.FaqFeedbackCountRow(
                f.faq.id, f.helpful, count(f))
            from FaqFeedback f
            where f.faq.id in :faqIds
            group by f.faq.id, f.helpful
            """)
    List<FaqFeedbackCountRow> countByFaqIds(@Param("faqIds") List<Long> faqIds);
}
