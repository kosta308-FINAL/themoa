package com.weaone.themoa.domain.customerservice.rag;

import com.weaone.themoa.domain.customerservice.entity.CustomerInquiryAnswer;
import com.weaone.themoa.domain.customerservice.entity.CustomerKnowledgeChunk;
import com.weaone.themoa.domain.customerservice.entity.Faq;
import com.weaone.themoa.domain.customerservice.repository.CustomerInquiryAnswerRepository;
import com.weaone.themoa.domain.customerservice.repository.CustomerKnowledgeChunkRepository;
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
    private final CustomerKnowledgeChunkRepository knowledgeChunkRepository;

    @Transactional(readOnly = true)
    public List<CustomerKnowledgeDocument> loadDocuments() {
        List<CustomerKnowledgeDocument> documents = new ArrayList<>();
        documents.addAll(guideCatalog.documents());
        documents.addAll(faqDocuments());
        documents.addAll(answeredInquiryDocuments());
        documents.addAll(adminDocumentChunks());
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
                        faqContent(faq)))
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
                answeredInquiryContent(answer));
    }

    private String faqContent(Faq faq) {
        return """
                문서유형: 고객센터 FAQ
                카테고리: %s
                사용자 질문 표현: %s

                운영 기준 및 답변 내용:
                %s
                """.formatted(
                faq.getFaqCategory().getName(),
                faq.getQuestion(),
                faq.getAnswerMarkdown()).trim();
    }

    private String answeredInquiryContent(CustomerInquiryAnswer answer) {
        return """
                문서유형: 기존 1:1 문의 답변
                카테고리: %s
                기존 문의 제목: %s

                운영 기준 및 관리자 답변:
                %s
                """.formatted(
                answer.getInquiry().getInquiryCategory().getName(),
                answer.getInquiry().getTitle(),
                answer.getContentMarkdown()).trim();
    }

    private List<CustomerKnowledgeDocument> adminDocumentChunks() {
        return knowledgeChunkRepository.findByKnowledgeFile_ActiveTrueOrderByKnowledgeFile_IdAscChunkIndexAsc().stream()
                .filter(chunk -> StringUtils.hasText(chunk.getContent()))
                .map(this::adminDocumentChunk)
                .toList();
    }

    private CustomerKnowledgeDocument adminDocumentChunk(CustomerKnowledgeChunk chunk) {
        return new CustomerKnowledgeDocument(
                adminDocumentKnowledgeId(chunk),
                CustomerKnowledgeSourceType.ADMIN_DOCUMENT,
                String.valueOf(chunk.getId()),
                chunk.getKnowledgeFile().getCategory(),
                chunk.getKnowledgeFile().getTitle() + " #" + (chunk.getChunkIndex() + 1),
                chunk.getContent());
    }

    public static String adminDocumentKnowledgeId(CustomerKnowledgeChunk chunk) {
        return "admin-document:" + chunk.getId();
    }
}
