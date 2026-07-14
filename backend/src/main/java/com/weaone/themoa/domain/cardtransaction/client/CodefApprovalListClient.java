package com.weaone.themoa.domain.cardtransaction.client;

import com.weaone.themoa.config.CodefProperties;
import com.weaone.themoa.domain.cardconnection.client.CodefClientException;
import io.codef.api.EasyCodef;
import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * CODEF 카드 승인내역 조회 전용 클라이언트(cardtransaction.md §6). productUrl·inquiryType은 실 계정 호출로
 * 검증된 값을 그대로 쓴다(임의 단정 금지 — §6-2).
 */
@Component
@RequiredArgsConstructor
public class CodefApprovalListClient {

    private static final String PRODUCT_URL = "/v1/kr/card/p/account/approval-list";
    private static final String RESULT_CODE_SUCCESS = "CF-00000";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 조회구분: "1" = 전체 통합조회(§6-2 확정). 카드별 선택 조회는 이 기능 스코프에 없다. */
    private static final String INQUIRY_TYPE_ALL = "1";
    /** 정렬: "0" = 최신순. */
    private static final String ORDER_BY_LATEST = "0";

    private final EasyCodef easyCodef;
    private final CodefProperties codefProperties;
    private final ExecutorService codefExecutor;

    public List<CodefApprovalRecord> fetch(CodefApprovalListCommand command) {
        HashMap<String, Object> parameterMap = new HashMap<>();
        parameterMap.put("organization", command.organization());
        parameterMap.put("connectedId", command.connectedId());
        parameterMap.put("birthDate", command.birthDate() == null ? "" : command.birthDate());
        parameterMap.put("startDate", command.startDate().format(DATE_FORMAT));
        parameterMap.put("endDate", command.endDate().format(DATE_FORMAT));
        parameterMap.put("orderBy", ORDER_BY_LATEST);
        parameterMap.put("inquiryType", INQUIRY_TYPE_ALL);
        parameterMap.put("cardName", "");
        parameterMap.put("duplicateCardIdx", "0");
        parameterMap.put("cardNo", "");
        parameterMap.put("cardPassword", "");
        parameterMap.put("memberStoreInfoType", "1");

        String raw = callWithTimeout(() -> easyCodef.requestProduct(
                PRODUCT_URL, codefProperties.serviceType(), parameterMap));
        return parse(raw);
    }

    private String callWithTimeout(Callable<String> call) {
        Future<String> future = codefExecutor.submit(call);
        try {
            return future.get(codefProperties.callTimeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new CodefClientException("CODEF 응답이 지연되고 있습니다.", e);
        } catch (Exception e) {
            throw new CodefClientException("CODEF 승인내역 조회 요청에 실패했습니다.", e);
        }
    }

    private List<CodefApprovalRecord> parse(String raw) {
        JSONObject json;
        try {
            json = new JSONObject(raw);
        } catch (JSONException e) {
            throw new CodefClientException("CODEF 응답을 해석하지 못했습니다.", e);
        }

        JSONObject result = json.optJSONObject("result");
        if (result == null) {
            throw new CodefClientException("CODEF 응답 형식이 올바르지 않습니다.", null);
        }
        if (!RESULT_CODE_SUCCESS.equals(result.optString("code", ""))) {
            throw new CodefClientException("CODEF 승인내역 조회가 실패했습니다.", null);
        }

        JSONArray data = json.optJSONArray("data");
        if (data == null) {
            return List.of();
        }
        List<CodefApprovalRecord> records = new ArrayList<>();
        for (int i = 0; i < data.length(); i++) {
            JSONObject item = data.optJSONObject(i);
            if (item == null) {
                continue;
            }
            records.add(new CodefApprovalRecord(
                    item.optString("resUsedDate", ""),
                    item.optString("resUsedTime", ""),
                    item.optString("resCardNo", ""),
                    item.optString("resCardName", ""),
                    item.optString("resMemberStoreName", ""),
                    item.optString("resUsedAmount", ""),
                    item.optString("resAccountCurrency", "KRW"),
                    item.optString("resApprovalNo", ""),
                    item.optString("resHomeForeignType", ""),
                    item.optString("resMemberStoreType", ""),
                    item.optString("resMemberStoreAddr", ""),
                    item.optString("resMemberStoreCorpNo", ""),
                    item.optString("resCancelYN", "0"),
                    item.optString("resCancelAmount", ""),
                    item.optString("resKRWAmt", ""),
                    item.optString("resInstallmentMonth", "")
            ));
        }
        return records;
    }
}
