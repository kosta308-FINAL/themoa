package com.weaone.themoa.domain.cardconnection.client;

import com.weaone.themoa.config.CodefProperties;
import io.codef.api.EasyCodef;
import io.codef.api.EasyCodefUtil;
import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * CODEF 커넥션(connectedId 발급) 전용 클라이언트. 카드사 로그인 자격증명은 여기서 RSA 암호화해 CODEF로
 * 전달할 뿐 어디에도 저장하지 않는다(connection.md §1).
 */
@Component
@RequiredArgsConstructor
public class CodefCardConnectionClient {

    private static final String RESULT_CODE_SUCCESS = "CF-00000";

    private final EasyCodef easyCodef;
    private final CodefProperties codefProperties;
    private final ExecutorService codefExecutor;

    public CodefAccountResult createAccount(CodefCreateAccountCommand command) {
        HashMap<String, Object> account = new HashMap<>();
        account.put("countryCode", "KR");
        account.put("businessType", "CD");
        account.put("clientType", "P");
        account.put("organization", command.organization());
        account.put("loginType", "1");
        account.put("id", command.loginId());
        account.put("password", encrypt(command.loginPassword()));
        if (command.cardNo() != null) {
            account.put("cardNo", command.cardNo());
        }
        if (command.cardPassword() != null) {
            account.put("cardPassword", encrypt(command.cardPassword()));
        }
        if (command.birthDate() != null) {
            account.put("birthDate", command.birthDate());
        }

        List<HashMap<String, Object>> accountList = new ArrayList<>();
        accountList.add(account);
        HashMap<String, Object> parameterMap = new HashMap<>();
        parameterMap.put("accountList", accountList);

        String raw = callWithTimeout(() -> easyCodef.createAccount(codefProperties.serviceType(), parameterMap));
        return parse(raw);
    }

    private String encrypt(String plainText) {
        try {
            return EasyCodefUtil.encryptRSA(plainText, easyCodef.getPublicKey());
        } catch (Exception e) {
            throw new CodefClientException("자격증명 암호화에 실패했습니다.", e);
        }
    }

    private String callWithTimeout(Callable<String> call) {
        Future<String> future = codefExecutor.submit(call);
        try {
            return future.get(codefProperties.callTimeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new CodefClientException("CODEF 응답이 지연되고 있습니다.", e);
        } catch (Exception e) {
            throw new CodefClientException("CODEF 커넥션 등록 요청에 실패했습니다.", e);
        }
    }

    /**
     * ⚠️ 카드사 계정 잠금 임박 신호(userError, connection.md §5-2)가 CODEF 응답의 정확히 어느 JSON 경로로
     * 오는지는 공식 SDK 문서에 명시돼 있지 않다. 다건 계정 등록 응답의 실패 상세(data.errorList[])와 result 객체
     * 양쪽을 방어적으로 조회한다 — 실 계정으로 CF-12801/잠금 임박 응답을 재현해 경로를 확정해야 한다.
     */
    private CodefAccountResult parse(String raw) {
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
        String code = result.optString("code", "");
        String message = result.optString("message", "");

        if (RESULT_CODE_SUCCESS.equals(code)) {
            JSONObject data = json.optJSONObject("data");
            String connectedId = data == null ? null : data.optString("connectedId", null);
            if (connectedId == null) {
                throw new CodefClientException("CODEF 응답에 connectedId가 없습니다.", null);
            }
            return CodefAccountResult.success(connectedId, code, message);
        }

        return CodefAccountResult.failure(code, message, extractUserError(json, result));
    }

    private String extractUserError(JSONObject json, JSONObject result) {
        String fromResult = result.optString("userError", "");
        if (!fromResult.isBlank()) {
            return fromResult;
        }
        JSONObject data = json.optJSONObject("data");
        if (data == null) {
            return "";
        }
        JSONArray errorList = data.optJSONArray("errorList");
        if (errorList != null && !errorList.isEmpty()) {
            JSONObject firstError = errorList.optJSONObject(0);
            if (firstError != null) {
                return firstError.optString("userError", "");
            }
        }
        return data.optString("userError", "");
    }
}
