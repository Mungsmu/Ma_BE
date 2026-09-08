package com.example.demo.auth.sms;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * 옥토모(Octomo) MO 문자 인증 API 클라이언트.
 * 옥토모는 발신(MT) 서비스가 아니라 "옥토모 대표번호(1666-3538)로 도착한 문자를 조회"하는
 * 수신(MO) 서비스다 — 우리가 사용자에게 문자를 보내는 게 아니라, 사용자가 보낸 문자가
 * 도착했는지를 조회한다. API Key는 서버에서만 사용한다(브라우저에 노출 금지).
 */
@Component
public class OctomoClient {

    private final RestClient restClient;
    private final String apiKey;

    public OctomoClient(@Value("${app.octomo.base-url}") String baseUrl,
                        @Value("${app.octomo.api-key}") String apiKey) {
        this.apiKey = apiKey;
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    /** mobileNum 에서 옥토모 대표번호로 text 그대로 담긴 문자가 withinMinutes 분 내에 도착했는지 조회 */
    public boolean existsMessage(String mobileNum, String text, int withinMinutes) {
        ExistsResponse response = restClient.post()
                .uri("/octomo/v1/public/message/exists")
                .header("Authorization", "Octomo " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("mobileNum", mobileNum, "text", text, "withinMinutes", withinMinutes))
                .retrieve()
                .body(ExistsResponse.class);
        return response != null && response.exists();
    }

    /** text를 본문으로 담은 SMS QR(PNG data URL, "data:image/png;base64,...")을 발급받는다. */
    public String createSmsQrCode(String text) {
        QrResponse response = restClient.post()
                .uri("/octomo/v1/public/message/qr-code")
                .header("Authorization", "Octomo " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("text", text))
                .retrieve()
                .body(QrResponse.class);
        if (response == null) {
            throw new IllegalStateException("옥토모 QR 발급 응답이 비어 있습니다.");
        }
        return response.qrCode();
    }

    private record ExistsResponse(boolean exists) {
    }

    private record QrResponse(String qrCode) {
    }
}
