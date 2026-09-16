package com.example.demo.auth.sms;

import org.junit.jupiter.api.Test;

import static com.example.demo.auth.sms.SmsVerificationService.Purpose.GUARDIAN;
import static com.example.demo.auth.sms.SmsVerificationService.Purpose.USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SmsVerificationServiceTest {

    private final OctomoClient octomoClient = mock(OctomoClient.class);
    // ttl=180s(3분), lookupWindow=30분, retention=24시간, octomo withinMinutes=5 — 운영 기본값과 동일
    private final SmsVerificationService service =
            new SmsVerificationService(octomoClient, 180, 30, 24, 5, "");

    @Test
    void issueCode로_생성한_코드가_옥토모에_도착하면_matches가_true다() {
        when(octomoClient.existsMessage(anyString(), anyString(), anyInt())).thenReturn(true);

        service.issueCode("01011112222", USER);

        assertThat(service.matches("01011112222", USER)).isTrue();
    }

    @Test
    void 옥토모에_도착_안했으면_false다() {
        when(octomoClient.existsMessage(anyString(), anyString(), anyInt())).thenReturn(false);

        service.issueCode("01011112222", USER);

        assertThat(service.matches("01011112222", USER)).isFalse();
    }

    @Test
    void 용도가_다르면_매칭되지_않는다() {
        service.issueCode("01011112222", USER);

        assertThat(service.matches("01011112222", GUARDIAN)).isFalse();
    }

    @Test
    void verifyAndConsume은_성공_후_같은_코드_재사용을_막는다() {
        when(octomoClient.existsMessage(anyString(), anyString(), anyInt())).thenReturn(true);

        service.issueCode("01011112222", USER);

        assertThat(service.verifyAndConsume("01011112222", USER)).isTrue();
        assertThat(service.verifyAndConsume("01011112222", USER)).isFalse(); // 재사용 차단
    }

    @Test
    void 발급된_코드가_없으면_옥토모를_호출하지_않고_바로_실패한다() {
        assertThat(service.matches("01099998888", USER)).isFalse();
    }

    @Test
    void dev_bypass_번호는_코드_발급_없이도_옥토모_호출_없이_통과한다() {
        SmsVerificationService bypassService =
                new SmsVerificationService(octomoClient, 180, 30, 24, 5, "010-0000-0000");

        assertThat(bypassService.matches("01000000000", USER)).isTrue();
        assertThat(bypassService.verifyAndConsume("01000000000", GUARDIAN)).isTrue();
    }

    @Test
    void 보호자_QR은_유효한_보호자_코드가_있어야_발급된다() {
        assertThat(org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> service.issueGuardianQr("01033334444")
        )).hasMessageContaining("먼저");
    }
}
