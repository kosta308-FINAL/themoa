package com.weaone.themoa.domain.customerservice.rag;

import com.weaone.themoa.domain.customerservice.entity.CustomerInquiryAnswer;
import com.weaone.themoa.domain.customerservice.entity.Faq;
import com.weaone.themoa.domain.customerservice.repository.CustomerInquiryAnswerRepository;
import com.weaone.themoa.domain.customerservice.repository.FaqRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CustomerKnowledgeDocumentProvider {

    private final CustomerServiceGuideCatalog guideCatalog;
    private final FaqRepository faqRepository;
    private final CustomerInquiryAnswerRepository answerRepository;

    @Transactional(readOnly = true)
    public List<CustomerKnowledgeDocument> loadDocuments() {
        List<CustomerKnowledgeDocument> documents = new ArrayList<>();
        documents.addAll(guideCatalog.documents());
        documents.addAll(faqDocuments());
        documents.addAll(answeredInquiryDocuments());
        return documents.stream()
                .filter(document -> StringUtils.hasText(document.content()))
                .sorted(Comparator.comparing(CustomerKnowledgeDocument::id))
                .toList();
    }

    private List<CustomerKnowledgeDocument> faqDocuments() {
        return faqRepository.findAllActiveWithCategory().stream()
                .map(faq -> new CustomerKnowledgeDocument(
                        "faq:" + faq.getId(),
                        CustomerKnowledgeSourceType.FAQ,
                        String.valueOf(faq.getId()),
                        faq.getFaqCategory().getName(),
                        faq.getQuestion(),
                        """
                                질문: %s

                                답변:
                                %s
                                """.formatted(faq.getQuestion(), faq.getAnswerMarkdown())))
                .toList();
    }

    private List<CustomerKnowledgeDocument> answeredInquiryDocuments() {
        return answerRepository.findAnsweredKnowledgeSources().stream()
                .filter(answer -> StringUtils.hasText(answer.getContentMarkdown()))
                .map(this::answeredInquiryDocument)
                .toList();
    }

    private CustomerKnowledgeDocument answeredInquiryDocument(CustomerInquiryAnswer answer) {
        return new CustomerKnowledgeDocument(
                "answered-inquiry:" + answer.getId(),
                CustomerKnowledgeSourceType.ANSWERED_INQUIRY,
                String.valueOf(answer.getId()),
                answer.getInquiry().getInquiryCategory().getName(),
                answer.getInquiry().getTitle(),
                """
                        기존 문의 제목: %s
                        문의 유형: %s

                        고객센터 답변:
                        %s
                        """.formatted(
                        answer.getInquiry().getTitle(),
                        answer.getInquiry().getInquiryCategory().getName(),
                        answer.getContentMarkdown()));
    }
}
