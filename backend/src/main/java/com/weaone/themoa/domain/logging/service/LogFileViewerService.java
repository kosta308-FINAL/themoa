package com.weaone.themoa.domain.logging.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.logging.dto.LogFileEntryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

/**
 * 관리자 페이지에서 AWS 콘솔 없이 {@code info.log}/{@code error.log}를 직접 열람하기 위한 서비스.
 *
 * <p>{@code error_log} DB 테이블(§3)이나 CloudWatch(§2-4)와는 별개 경로다 — 새 저장소를 만들지 않고
 * Logback이 이미 쓰고 있는 파일을 그대로 읽는다. {@code LOG_PATH}는 logback-spring.xml과 같은
 * 프로퍼티를 공유하므로, 로컬(상대경로 {@code logs/})과 운영 EC2(작업 디렉터리 기준 {@code logs/})에서
 * 별도 분기 없이 각자의 실제 로그 파일을 그대로 가리킨다.
 *
 * <p>오늘자 파일({@code info.log}/{@code error.log})은 미압축 상태 그대로 읽고, logback-spring.xml의
 * {@code fileNamePattern}({@code info-yyyy-MM-dd.N.log.gz})으로 롤오버된 과거 날짜는 date 파라미터로
 * 지정하면 같은 날짜의 인덱스(N)를 시간순으로 이어붙여 압축을 풀어 읽는다.
 */
@Slf4j
@Service
public class LogFileViewerService {

    private static final Set<String> ALLOWED_LEVELS = Set.of("INFO", "WARN", "ERROR");
    private static final int DEFAULT_LIMIT = 200;
    private static final int MAX_LIMIT = 1000;

    /** logback-spring.xml의 FILE_LOG_PATTERN과 짝을 이룬다. message는 줄 끝까지 통째로 캡처한다. */
    private static final Pattern ENTRY_PATTERN = Pattern.compile(
            "^timestamp=(\\S+) level=(\\S+) traceId=(\\S+) thread=(\\S+) logger=(\\S+) message=(.*)$");

    /** logback-spring.xml의 fileNamePattern(info-yyyy-MM-dd.N.log.gz / error-yyyy-MM-dd.N.log.gz)과 짝을 이룬다. */
    private static final Pattern ROLLED_FILE_PATTERN =
            Pattern.compile("^(info|error)-(\\d{4}-\\d{2}-\\d{2})\\.(\\d+)\\.log\\.gz$");

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    @Value("${LOG_PATH:logs}")
    private String logPath;

    public List<LogFileEntryResponse> readRecent(String level, String date, String keyword, Integer limit) {
        String normalizedLevel = normalizeLevel(level);
        int normalizedLimit = normalizeLimit(limit);
        String prefix = "ERROR".equals(normalizedLevel) ? "error" : "info";

        List<String> lines = (date == null || date.isBlank())
                ? readPlainLines(Path.of(logPath, prefix + ".log"))
                : readRolledLines(prefix, date);

        List<LogFileEntryResponse> entries = parse(lines).stream()
                .filter(entry -> entry.level().equalsIgnoreCase(normalizedLevel))
                .filter(entry -> matchesKeyword(entry, keyword))
                .collect(Collectors.toCollection(ArrayList::new));

        Collections.reverse(entries);
        return entries.size() > normalizedLimit ? entries.subList(0, normalizedLimit) : entries;
    }

    /** 조회 가능한 날짜(yyyy-MM-dd) 목록을 최신순으로 반환한다. 오늘(미압축)과 압축 롤오버분(.gz)을 합친다. */
    public List<String> listAvailableDates(String level) {
        String normalizedLevel = normalizeLevel(level);
        String prefix = "ERROR".equals(normalizedLevel) ? "error" : "info";
        Path dir = Path.of(logPath);

        Set<String> dates = new TreeSet<>(Comparator.reverseOrder());
        if (Files.exists(dir.resolve(prefix + ".log"))) {
            dates.add(LocalDate.now().format(DATE_FORMAT));
        }
        if (Files.isDirectory(dir)) {
            try (var stream = Files.list(dir)) {
                stream.map(p -> p.getFileName().toString())
                        .forEach(name -> {
                            Matcher matcher = ROLLED_FILE_PATTERN.matcher(name);
                            if (matcher.matches() && matcher.group(1).equals(prefix)) {
                                dates.add(matcher.group(2));
                            }
                        });
            } catch (IOException e) {
                log.warn("로그 디렉터리를 읽지 못했습니다. dir={}", dir, e);
            }
        }
        return new ArrayList<>(dates);
    }

    private List<String> readPlainLines(Path file) {
        if (!Files.exists(file)) {
            return List.of();
        }
        try {
            return Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("로그 파일을 읽지 못했습니다. file={}", file, e);
            return List.of();
        }
    }

    /** 같은 날짜에 여러 인덱스(0,1,2…)로 롤오버된 압축 파일을, 인덱스 오름차순(=시간순)으로 이어붙여 읽는다. */
    private List<String> readRolledLines(String prefix, String date) {
        Path dir = Path.of(logPath);
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        List<Path> files;
        try (var stream = Files.list(dir)) {
            files = stream
                    .filter(p -> matchesRolled(p.getFileName().toString(), prefix, date))
                    .sorted(Comparator.comparingInt(p -> rolledIndex(p.getFileName().toString())))
                    .toList();
        } catch (IOException e) {
            log.warn("로그 디렉터리를 읽지 못했습니다. dir={}", dir, e);
            return List.of();
        }

        List<String> lines = new ArrayList<>();
        for (Path file : files) {
            lines.addAll(readGzipLines(file));
        }
        return lines;
    }

    private boolean matchesRolled(String name, String prefix, String date) {
        Matcher matcher = ROLLED_FILE_PATTERN.matcher(name);
        return matcher.matches() && matcher.group(1).equals(prefix) && matcher.group(2).equals(date);
    }

    private int rolledIndex(String name) {
        Matcher matcher = ROLLED_FILE_PATTERN.matcher(name);
        return matcher.matches() ? Integer.parseInt(matcher.group(3)) : Integer.MAX_VALUE;
    }

    private List<String> readGzipLines(Path file) {
        try (var in = new GZIPInputStream(Files.newInputStream(file));
                var reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return reader.lines().toList();
        } catch (IOException e) {
            log.warn("압축 로그 파일을 읽지 못했습니다. file={}", file, e);
            return List.of();
        }
    }

    /** info.log/error.log 모두 한 줄에 한 엔트리가 기본이지만, ERROR의 StackTrace처럼 뒤따르는 줄은 앞 엔트리의 message에 이어붙인다. */
    private List<LogFileEntryResponse> parse(List<String> lines) {
        List<LogFileEntryResponse> result = new ArrayList<>();
        String timestamp = null;
        String level = null;
        String traceId = null;
        String thread = null;
        String logger = null;
        StringBuilder message = null;

        for (String line : lines) {
            Matcher matcher = ENTRY_PATTERN.matcher(line);
            if (matcher.matches()) {
                if (message != null) {
                    result.add(new LogFileEntryResponse(timestamp, level, traceId, thread, logger, message.toString()));
                }
                timestamp = matcher.group(1);
                level = matcher.group(2);
                traceId = matcher.group(3);
                thread = matcher.group(4);
                logger = matcher.group(5);
                message = new StringBuilder(matcher.group(6));
            } else if (message != null) {
                message.append('\n').append(line);
            }
        }
        if (message != null) {
            result.add(new LogFileEntryResponse(timestamp, level, traceId, thread, logger, message.toString()));
        }
        return result;
    }

    private boolean matchesKeyword(LogFileEntryResponse entry, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String lower = keyword.toLowerCase();
        return entry.message().toLowerCase().contains(lower)
                || entry.logger().toLowerCase().contains(lower)
                || entry.traceId().toLowerCase().contains(lower);
    }

    private String normalizeLevel(String level) {
        if (level == null || !ALLOWED_LEVELS.contains(level.toUpperCase())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        return level.toUpperCase();
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
