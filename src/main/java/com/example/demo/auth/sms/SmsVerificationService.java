package com.example.demo.auth.sms;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SMS 인증번호 발급/검증 서비스.
 * 데모용으로 인증번호를 인메모리에 저장한다. (운영에서는 Redis 등 사용 권장)
 */
@Service
public class SmsVerificationService {

    private final SmsSender smsSender;
    private final long codeTtlSeconds;

    private final SecureRandom random = new SecureRandom();
    private final ConcurrentHashMap<String, CodeEntry> store = new ConcurrentHashMap<>();

    public SmsVerificationService(SmsSender smsSender,
                                  @Value("${app.sms.code-ttl-seconds:180}") long codeTtlSeconds) {
        this.smsSender = smsSender;
        this.codeTtlSeconds = codeTtlSeconds;
    }

    /** 인증번호를 생성해 문자로 발송한다. */
    public void sendCode(String phone) {
        String key = normalize(phone);
        String code = generateCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(codeTtlSeconds);
        store.put(key, new CodeEntry(code, expiresAt));

        smsSender.send(phone, "[인증] 인증번호는 [" + code + "] 입니다. "
                + (codeTtlSeconds / 60) + "분 내에 입력해 주세요.");
    }

    /** 인증번호 일치 여부만 확인한다. (인증 상태는 유지) */
    public boolean matches(String phone, String code) {
        CodeEntry entry = store.get(normalize(phone));
        if (entry == null || entry.isExpired()) {
            return false;
        }
        return entry.code().equals(code);
    }

    /**
     * 인증번호를 확인하고, 성공 시 해당 번호의 인증 정보를 소비(제거)한다.
     * 회원가입 최종 처리 시 사용.
     */
    public boolean verifyAndConsume(String phone, String code) {
        String key = normalize(phone);
        CodeEntry entry = store.get(key);
        if (entry == null || entry.isExpired() || !entry.code().equals(code)) {
            return false;
        }
        store.remove(key);
        return true;
    }

    private String generateCode() {
        return String.format("%06d", random.nextInt(1_000_000));
    }

    /** 하이픈 유무와 무관하게 같은 번호로 취급 */
    private String normalize(String phone) {
        return phone.replaceAll("-", "");
    }

    private record CodeEntry(String code, LocalDateTime expiresAt) {
        boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }
    }

    public Duration ttl() {
        return Duration.ofSeconds(codeTtlSeconds);
    }
}
