package com.cloudopsguard.changerequest;

import com.cloudopsguard.domain.changerequest.ChangeRequest;
import com.cloudopsguard.domain.changerequest.ChangeRequestService;
import com.cloudopsguard.domain.changerequest.dto.CreateChangeRequest;
import com.cloudopsguard.domain.common.Environment;
import com.cloudopsguard.domain.common.IacType;
import com.cloudopsguard.domain.common.Role;
import com.cloudopsguard.domain.risk.IaCChangeProvider;
import com.cloudopsguard.domain.user.User;
import com.cloudopsguard.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * create 時に planSourceRef があれば IaCChangeProvider 解決の差分を保存する配線テスト。
 * Provider は @MockBean で差し替え（実 AWS 不要）。
 */
class CreateWithPlanSourceTest extends AbstractIntegrationTest {

    @MockBean
    private IaCChangeProvider iaCChangeProvider;

    @Autowired
    private ChangeRequestService service;

    @Test
    void planSourceRefありならProvider解決の差分を保存する() {
        when(iaCChangeProvider.resolveDiffText(anyString(), any())).thenReturn("FETCHED-PLAN");
        User requester = createUser("reqPlan", Role.REQUESTER);

        CreateChangeRequest req = new CreateChangeRequest(
                "実planから作成", Environment.DEVELOPMENT, IacType.TERRAFORM,
                "S3", "dev-bucket", "理由", "概要", "manual-diff",
                null, null, "s3://bucket/plan.txt");

        ChangeRequest cr = service.create(principal(requester), req);

        assertThat(cr.getDiffText()).isEqualTo("FETCHED-PLAN");
        verify(iaCChangeProvider).resolveDiffText("s3://bucket/plan.txt", "manual-diff");
    }
}
