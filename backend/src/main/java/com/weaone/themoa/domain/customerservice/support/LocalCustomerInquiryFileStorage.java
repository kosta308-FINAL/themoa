package com.weaone.themoa.domain.customerservice.support;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.config.CustomerServiceProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * 로컬 개발용 첨부파일 저장소(customerservice.md §6-2). 임시 확장자로 쓴 뒤 검증·쓰기가 끝나면 원자적으로
 * 이동하고, 경로 정규화 후 결과가 local-root 밖이면 저장·조회 모두 거부한다.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.customer-service.storage", name = "provider", havingValue = "LOCAL",
        matchIfMissing = true)
public class LocalCustomerInquiryFileStorage implements CustomerInquiryFileStorage {

    private final Path root;

    public LocalCustomerInquiryFileStorage(CustomerServiceProperties properties) {
        this.root = Path.of(properties.storage().localRoot()).toAbsolutePath().normalize();
    }

    @Override
    public void store(String objectKey, byte[] content, String contentType) {
        Path target = resolveWithinRoot(objectKey);
        Path tempFile = target.resolveSibling(target.getFileName() + "." + UUID.randomUUID() + ".part");
        try {
            Files.createDirectories(target.getParent());
            Files.write(tempFile, content);
            Files.move(tempFile, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            deleteQuietly(tempFile);
            log.warn("고객센터 첨부파일 로컬 저장 실패");
            throw new BusinessException(ErrorCode.CUSTOMER_INQUIRY_FILE_STORAGE_FAILED);
        }
    }

    @Override
    public StoredFile load(String objectKey) {
        Path source = resolveWithinRoot(objectKey);
        try {
            byte[] content = Files.readAllBytes(source);
            return new StoredFile(content, contentTypeFromExtension(objectKey));
        } catch (IOException e) {
            log.warn("고객센터 첨부파일 로컬 조회 실패");
            throw new BusinessException(ErrorCode.CUSTOMER_INQUIRY_FILE_STORAGE_FAILED);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            Files.deleteIfExists(resolveWithinRoot(objectKey));
        } catch (IOException | BusinessException e) {
            log.warn("고객센터 첨부파일 보상 삭제 실패");
        }
    }

    private Path resolveWithinRoot(String objectKey) {
        Path resolved = root.resolve(objectKey).normalize();
        if (!resolved.startsWith(root)) {
            throw new BusinessException(ErrorCode.CUSTOMER_INQUIRY_FILE_STORAGE_FAILED);
        }
        return resolved;
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // 임시 파일 정리 실패는 저장 실패 처리에 영향을 주지 않는다.
        }
    }

    private String contentTypeFromExtension(String objectKey) {
        String lower = objectKey.toLowerCase();
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        return "image/jpeg";
    }
}
