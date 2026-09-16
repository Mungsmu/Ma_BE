package com.example.demo.auth.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 옥토모(Octomo) MO 인증 기반 SMS 인증번호 발급/검증 서비스.
 * 서버가 문자를 발송하지 않는다 — 코드를 생성해 화면에 보여주면, 사용자가 그 코드를
 * 옥토모 대표번호(1666-3538)로 직접 전송하고, 서버는 그 수신 여부를 옥토모 API로 조회한다.
 * 데모용으로 인증 상태를 인메모리에 저장한다. (운영에서는 Redis 등 사용 권장)
 */
@Service
public class SmsVerificationService {

    private static final Logger log = LoggerFactory.getLogger(SmsVerificationService.class);

    /** 인증 용도별 코드 프리픽스. 옥토모에는 이 프리픽스가 붙은 문자열이 그대로 도착해야 매칭된다. */
    public enum Purpose {
        USER("u-"),
        GUARDIAN("g-");

        private final String prefix;

        Purpose(String prefix) {
            this.prefix = prefix;
        }

        String buildText(String code) {
            return prefix + code;
        }
    }

    private final OctomoClient octomoClient;
    private final long codeTtlSeconds;
    private final long lookupWindowMinutes;
    private final long retentionHours;
    private final int octomoWithinMinutes;
    private final String devBypassPhone;

    private final SecureRandom random = new SecureRandom();
    private final ConcurrentHashMap<String, CodeEntry> store = new ConcurrentHashMap<>();

    public SmsVerificationService(OctomoClient octomoClient,
                                  @Value("${app.sms.code-ttl-seconds:180}") long codeTtlSeconds,
                                  @Value("${app.sms.code-lookup-window-minutes:30}") long lookupWindowMinutes,
                                  @Value("${app.sms.code-retention-hours:24}") long retentionHours,
                                  @Value("${app.octomo.within-minutes:5}") int octomoWithinMinutes,
                                  @Value("${app.sms.dev-bypass-phone:}") String devBypassPhone) {
        this.octomoClient = octomoClient;
        this.codeTtlSeconds = codeTtlSeconds;
        this.lookupWindowMinutes = lookupWindowMinutes;
        this.retentionHours = retentionHours;
        this.octomoWithinMinutes = octomoWithinMinutes;
        this.devBypassPhone = devBypassPhone.isBlank() ? null : normalize(devBypassPhone);
    }

    /** 인증코드를 생성해 저장하고 반환한다. (문자 발송 없음 — 코드를 화면에 보여주기 위해 반환) */
    public String issueCode(String phone, Purpose purpose) {
        String key = normalize(phone);
        String code = generateCode();
        LocalDateTime now = LocalDateTime.now();
        store.put(key, new CodeEntry(code, purpose, now, now.plusSeconds(codeTtlSeconds), false));
        return code;
    }

    /** 인증코드 매칭(=옥토모 수신 확인) 여부만 조회한다. (매칭 상태는 소비하지 않음) */
    public boolean matches(String phone, Purpose purpose) {
        return check(phone, purpose, false);
    }

    /** 인증코드를 확인하고, 성공 시 매칭 완료로 표시(소비)한다. 회원가입/보호자 인증 최종 처리 시 사용. */
    public boolean verifyAndConsume(String phone, Purpose purpose) {
        return check(phone, purpose, true);
    }

    /** 발급된 보호자 인증코드를 SMS QR(옥토모가 대신 문자를 채워 보내는 QR)로 발급받는다. */
    public String issueGuardianQr(String phone) {
        CodeEntry entry = findValidEntry(normalize(phone), Purpose.GUARDIAN);
        if (entry == null) {
            throw new IllegalArgumentException("먼저 보호자 인증코드를 발급해 주세요.");
        }
        return octomoClient.createSmsQrCode(Purpose.GUARDIAN.buildText(entry.code()));
    }

    private boolean check(String phone, Purpose purpose, boolean consume) {
        String key = normalize(phone);
        // ponytail: 개발용 전역 우회 번호 하나뿐 — 실제 계정별 플래그가 필요해지면 Member에 컬럼 추가
        if (key.equals(devBypassPhone)) {
            return true;
        }
        CodeEntry entry = findValidEntry(key, purpose);
        if (entry == null) {
            return false;
        }

        boolean exists;
        try {
            // 옥토모는 mobileNum을 하이픈 없는 11자리 숫자로 요구한다 — 저장소 키(정규화된 번호)를 그대로 재사용.
            exists = octomoClient.existsMessage(key, purpose.buildText(entry.code()), octomoWithinMinutes);
        } catch (RestClientException e) {
            log.warn("옥토모 문자 조회 실패: phone={}, purpose={}", key, purpose, e);
            return false;
        }

        if (exists && consume) {
            store.computeIfPresent(key, (k, v) -> v.asMatched());
        }
        return exists;
    }

    /**
     * 발급된 코드 중 아직 유효한 것만 반환한다.
     * 1차 검증(존재 여부/용도 일치/이미 매칭 소비된 코드인지)과, TTL·조회 대상 기간(기본 30분) 초과 여부를
     * 여기서 전부 로컬로 걸러낸다 — 옥토모 API(네트워크 호출)는 이 검증을 통과한 경우에만 호출한다.
     */
    private CodeEntry findValidEntry(String key, Purpose purpose) {
        CodeEntry entry = store.get(key);
        if (entry == null || entry.purpose() != purpose || entry.matched()) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(entry.expiresAt()) || entry.createdAt().isBefore(now.minusMinutes(lookupWindowMinutes))) {
            return null;
        }
        return entry;
    }

    /** 1일(기본, 설정 가능) 이상 지난 코드를 매칭 완료/미매칭/만료 여부와 무관하게 저장소에서 정리한다. */
    @Scheduled(fixedRate = 60 * 60 * 1000)
    public void purgeStaleCodes() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(retentionHours);
        store.entrySet().removeIf(e -> e.getValue().createdAt().isBefore(cutoff));
    }

    private String generateCode() {
        return String.format("%06d", random.nextInt(1_000_000));
    }

    /** 하이픈 유무와 무관하게 같은 번호로 취급 */
    private String normalize(String phone) {
        return phone.replaceAll("-", "");
    }

    private record CodeEntry(String code, Purpose purpose, LocalDateTime createdAt,
                              LocalDateTime expiresAt, boolean matched) {
        CodeEntry asMatched() {
            return new CodeEntry(code, purpose, createdAt, expiresAt, true);
        }
    }

    public Duration ttl() {
        return Duration.ofSeconds(codeTtlSeconds);
    }
}
