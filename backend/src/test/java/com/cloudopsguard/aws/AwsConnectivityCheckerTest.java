package com.cloudopsguard.aws;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AWS 疎通チェッカーの単体テスト（実 AWS 不要・StsClient をモック）。
 * 成功時は GetCallerIdentity を1回呼ぶこと、失敗時も起動を止めない（例外を伝播しない）ことを検証。
 */
class AwsConnectivityCheckerTest {

    @Test
    void 成功時はSTSのアカウントIDを取得し例外を投げない() {
        StsClient sts = mock(StsClient.class);
        when(sts.getCallerIdentity()).thenReturn(
                GetCallerIdentityResponse.builder()
                        .account("123456789012")
                        .arn("arn:aws:iam::123456789012:role/readonly")
                        .build());

        AwsConnectivityChecker checker = new AwsConnectivityChecker(sts);

        assertThatCode(() -> checker.run(null)).doesNotThrowAnyException();
        verify(sts).getCallerIdentity();
    }

    @Test
    void STS呼び出しが失敗しても起動を止めない() {
        StsClient sts = mock(StsClient.class);
        when(sts.getCallerIdentity()).thenThrow(new RuntimeException("資格情報なし"));

        AwsConnectivityChecker checker = new AwsConnectivityChecker(sts);

        assertThatCode(() -> checker.run(null)).doesNotThrowAnyException();
    }
}
