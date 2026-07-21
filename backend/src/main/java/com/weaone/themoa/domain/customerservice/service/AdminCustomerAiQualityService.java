package com.weaone.themoa.domain.customerservice.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.customerservice.dto.request.AdminCustomerAiPreviewRequest;
import com.weaone.themoa.domain.customerservice.dto.request.AdminCustomerAiSearchRequest;
import com.weaone.themoa.domain.customerservice.dto.request.AdminCustomerAiSettingsRequest;
import com.weaone.themoa.domain.customerservice.dto.response.AdminCustomerAiPreviewResponse;
import com.weaone.themoa.domain.customerservice.dto.response.AdminCustomerAiSearchResponse;
import com.weaone.themoa.domain.customerservice.dto.response.AdminCustomerAiSearchResultResponse;
import com.weaone.themoa.domain.customerservice.dto.response.AdminCustomerAiSettingsResponse;
import com.weaone.themoa.domain.customerservice.dto.response.AdminCustomerKnowledgeChunkResponse;
import com.weaone.themoa.domain.customerservice.dto.response.AdminCustomerKnowledgeFileResponse;
import com.weaone.themoa.domain.customerservice.entity.CustomerKnowledgeChunk;
import com.weaone.themoa.domain.customerservice.entity.CustomerKnowledgeFile;
import com.weaone.themoa.domain.customerservice.rag.CustomerKnowledgeDocument;
import com.weaone.themoa.domain.customerservice.rag.CustomerKnowledgeDocumentChunker;
import com.weaone.themoa.domain.customerservice.rag.CustomerKnowledgeDocumentProvider;
import com.weaone.themoa.domain.customerservice.rag.CustomerKnowledgeEmbeddingService;
import com.weaone.themoa.domain.customerservice.rag.CustomerKnowledgeRetriever;
import com.weaone.themoa.domain.customerservice.rag.CustomerKnowledgeSearchResult;
import com.weaone.themoa.domain.customerservice.rag.CustomerKnowledgeSourceType;
import com.weaone.themoa.domain.customerservice.rag.CustomerServiceRagSettingValues;
import com.weaone.themoa.domain.customerservice.repository.CustomerKnowledgeChunkRepository;
import com.weaone.themoa.domain.customerservice.repository.CustomerKnowledgeFileRepository;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class AdminCustomerAiQualityService {

    private static final long MAX_KNOWLEDGE_FILE_SIZE_BYTES = 2L * 1024 * 1024;
    private static final int TITLE_MAX_LENGTH = 150;
    private static final int CATEGORY_MAX_LENGTH = 80;

    private final CustomerServiceRagSettingService ragSettingService;
    private final CustomerKnowledgeRetriever retriever;
    private final CustomerServiceChatService chatService;
    private final CustomerKnowledgeDocumentChunker chunker;
    private final CustomerKnowledgeEmbeddingService embeddingService;
    private final CustomerKnowledgeFileRepository knowledgeFileRepository;
    private final CustomerKnowledgeChunkRepository knowledgeChunkRepository;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public AdminCustomerAiSettingsResponse settings() {
        return AdminCustomerAiSettingsResponse.from(ragSettingService.current());
    }

    @Transactional
    public AdminCustomerAiSettingsResponse updateSettings(Long adminId, AdminCustomerAiSettingsRequest request) {
        CustomerServiceRagSettingValues values = ragSettingService.update(
                adminId,
                request == null ? null : request.topK(),
                request == null ? null : request.minimumSimilarity(),
                request == null ? null : request.systemPrompt());
        return AdminCustomerAiSettingsResponse.from(values);
    }

    @Transactional(readOnly = true)
    public AdminCustomerAiSearchResponse search(AdminCustomerAiSearchRequest request) {
        CustomerServiceRagSettingValues values = requestValues(request);
        String query = requireText(request == null ? null : request.query(), 1, 500);
        List<CustomerKnowledgeSearchResult> results = retriever.retrieveVectorOnly(
                query, values.topK(), values.minimumSimilarity());
        return searchResponse(query, values, results);
    }

    @Transactional(readOnly = true)
    public AdminCustomerAiPreviewResponse preview(AdminCustomerAiPreviewRequest request) {
        CustomerServiceRagSettingValues values = previewValues(request);
        String query = requireText(request == null ? null : request.query(), 1, 500);
        List<CustomerKnowledgeSearchResult> results = retriever.retrieveVectorOnly(
                query, values.topK(), values.minimumSimilarity());
        AdminCustomerAiSearchResponse search = searchResponse(query, values, results);
        if (results.isEmpty()) {
            return new AdminCustomerAiPreviewResponse(search, "Qdrant 검색 결과가 없어 답변을 생성하지 않았습니다.", true);
        }
        String answer = chatService.generateGroundedAnswer(query, results, values.systemPrompt());
        return new AdminCustomerAiPreviewResponse(search, answer,
                answer.contains("1:1 문의") || answer.contains("확인이 필요"));
    }

    @Transactional(readOnly = true)
    public List<AdminCustomerKnowledgeFileResponse> documents() {
        return knowledgeFileRepository.findAllByOrderByCreatedAtDescIdDesc().stream()
                .map(file -> AdminCustomerKnowledgeFileResponse.of(
                        file,
                        knowledgeChunkRepository.findByKnowledgeFile_IdOrderByChunkIndexAsc(file.getId()).stream()
                                .map(AdminCustomerKnowledgeChunkResponse::from)
                                .toList()))
                .toList();
    }

    @Transactional
    public AdminCustomerKnowledgeFileResponse upload(Long adminId, String title, String category, MultipartFile file) {
        validateFile(file);
        String normalizedTitle = requireText(title, 1, TITLE_MAX_LENGTH);
        String normalizedCategory = requireText(category, 1, CATEGORY_MAX_LENGTH);
        String content = readText(file);
        List<String> chunks = chunker.chunk(content);
        if (chunks.isEmpty()) {
            throw new BusinessException(ErrorCode.CUSTOMER_KNOWLEDGE_INVALID_REQUEST);
        }
        Member admin = adminId == null ? null : memberRepository.getReferenceById(adminId);
        LocalDateTime now = LocalDateTime.now();
        CustomerKnowledgeFile knowledgeFile = knowledgeFileRepository.save(CustomerKnowledgeFile.create(
                normalizedTitle,
                normalizedCategory,
                safeFilename(file.getOriginalFilename()),
                file.getSize(),
                content,
                admin,
                now));
        List<CustomerKnowledgeChunk> savedChunks = IntStream.range(0, chunks.size())
                .mapToObj(index -> CustomerKnowledgeChunk.create(knowledgeFile, index, chunks.get(index), now))
                .toList();
        knowledgeChunkRepository.saveAll(savedChunks);
        knowledgeChunkRepository.flush();

        List<CustomerKnowledgeDocument> documents = savedChunks.stream()
                .map(this::document)
                .toList();
        savedChunks.forEach(chunk -> chunk.assignQdrantPointId(embeddingService.stableUuid(
                CustomerKnowledgeDocumentProvider.adminDocumentKnowledgeId(chunk))));
        knowledgeFile.updateChunkCount(savedChunks.size());
        int embedded = embeddingService.embedDocuments(documents);
        if (embedded == savedChunks.size()) {
            knowledgeFile.markEmbedded(LocalDateTime.now());
        } else {
            knowledgeFile.markFailed();
        }
        return AdminCustomerKnowledgeFileResponse.of(
                knowledgeFile,
                savedChunks.stream().map(AdminCustomerKnowledgeChunkResponse::from).toList());
    }

    @Transactional
    public AdminCustomerKnowledgeFileResponse reembed(Long documentId) {
        CustomerKnowledgeFile file = knowledgeFileRepository.findByIdAndActiveTrue(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_KNOWLEDGE_DOCUMENT_NOT_FOUND));
        List<CustomerKnowledgeChunk> chunks = knowledgeChunkRepository.findByKnowledgeFile_IdOrderByChunkIndexAsc(file.getId());
        int embedded = embeddingService.embedDocuments(chunks.stream().map(this::document).toList());
        if (embedded == chunks.size()) {
            file.markEmbedded(LocalDateTime.now());
        } else {
            file.markFailed();
        }
        return AdminCustomerKnowledgeFileResponse.of(
                file,
                chunks.stream().map(AdminCustomerKnowledgeChunkResponse::from).toList());
    }

    @Transactional
    public void disable(Long documentId) {
        CustomerKnowledgeFile file = knowledgeFileRepository.findByIdAndActiveTrue(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_KNOWLEDGE_DOCUMENT_NOT_FOUND));
        List<CustomerKnowledgeChunk> chunks = knowledgeChunkRepository.findByKnowledgeFile_IdOrderByChunkIndexAsc(file.getId());
        embeddingService.deleteDocuments(chunks.stream()
                .map(CustomerKnowledgeDocumentProvider::adminDocumentKnowledgeId)
                .toList());
        file.disable();
    }

    private CustomerServiceRagSettingValues requestValues(AdminCustomerAiSearchRequest request) {
        return ragSettingService.normalize(
                request == null ? null : request.topK(),
                request == null ? null : request.minimumSimilarity(),
                ragSettingService.current().systemPrompt());
    }

    private CustomerServiceRagSettingValues previewValues(AdminCustomerAiPreviewRequest request) {
        return ragSettingService.normalize(
                request == null ? null : request.topK(),
                request == null ? null : request.minimumSimilarity(),
                request == null ? null : request.systemPrompt());
    }

    private AdminCustomerAiSearchResponse searchResponse(String query, CustomerServiceRagSettingValues values,
                                                         List<CustomerKnowledgeSearchResult> results) {
        List<AdminCustomerAiSearchResultResponse> items = IntStream.range(0, results.size())
                .mapToObj(index -> AdminCustomerAiSearchResultResponse.of(index + 1, results.get(index)))
                .toList();
        return new AdminCustomerAiSearchResponse(query, values.topK(), values.minimumSimilarity(), items.size(), items);
    }

    private CustomerKnowledgeDocument document(CustomerKnowledgeChunk chunk) {
        return new CustomerKnowledgeDocument(
                CustomerKnowledgeDocumentProvider.adminDocumentKnowledgeId(chunk),
                CustomerKnowledgeSourceType.ADMIN_DOCUMENT,
                String.valueOf(chunk.getId()),
                chunk.getKnowledgeFile().getCategory(),
                chunk.getKnowledgeFile().getTitle() + " #" + (chunk.getChunkIndex() + 1),
                chunk.getContent());
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.CUSTOMER_KNOWLEDGE_INVALID_REQUEST);
        }
        if (file.getSize() > MAX_KNOWLEDGE_FILE_SIZE_BYTES) {
            throw new BusinessException(ErrorCode.CUSTOMER_KNOWLEDGE_FILE_LIMIT_EXCEEDED);
        }
        String filename = file.getOriginalFilename();
        if (!StringUtils.hasText(filename)) {
            throw new BusinessException(ErrorCode.CUSTOMER_KNOWLEDGE_FILE_TYPE_NOT_ALLOWED);
        }
        String lower = filename.toLowerCase(Locale.ROOT);
        if (!lower.endsWith(".md") && !lower.endsWith(".txt")) {
            throw new BusinessException(ErrorCode.CUSTOMER_KNOWLEDGE_FILE_TYPE_NOT_ALLOWED);
        }
    }

    private String readText(MultipartFile file) {
        try {
            return requireText(new String(file.getBytes(), StandardCharsets.UTF_8)
                    .replace("\uFEFF", ""), 1, 500_000);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.CUSTOMER_KNOWLEDGE_INVALID_REQUEST);
        }
    }

    private String requireText(String value, int minLength, int maxLength) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.length() < minLength || trimmed.length() > maxLength) {
            throw new BusinessException(ErrorCode.CUSTOMER_KNOWLEDGE_INVALID_REQUEST);
        }
        return trimmed;
    }

    private String safeFilename(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "knowledge.txt";
        }
        return filename.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }
}
