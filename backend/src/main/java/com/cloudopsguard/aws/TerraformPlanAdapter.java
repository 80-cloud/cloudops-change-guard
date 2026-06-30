package com.cloudopsguard.aws;

import com.cloudopsguard.domain.risk.IaCChangeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.util.Optional;

/**
 * 実 plan テキストの取り込み口（aws プロファイル）。CI 等が S3 に保存した terraform plan の出力（テキスト）を
 * read-only で取得し、既存の TerraformDiffParser → RiskEngine に流す。取得失敗時は empty を返し、
 * 呼び出し側は手貼り diffText へ fallback する（申請フローは止めない）。
 *
 * <p>sourceRef は S3 オブジェクトキー、バケットは cloudops.aws.plan-bucket。
 * 書込/削除 API は持たない（誤操作防止・docs/AWS・IaC連携方針.md §5）。
 */
@Component
@Profile("aws")
public class TerraformPlanAdapter implements IaCChangeProvider {

    private static final Logger log = LoggerFactory.getLogger(TerraformPlanAdapter.class);

    private final S3Client s3;
    private final String planBucket;

    public TerraformPlanAdapter(S3Client s3, CloudopsAwsProperties props) {
        this.s3 = s3;
        this.planBucket = props.planBucket();
    }

    @Override
    public Optional<String> fetchPlanText(String sourceRef) {
        if (sourceRef == null || sourceRef.isBlank() || planBucket == null || planBucket.isBlank()) {
            return Optional.empty();
        }
        try {
            ResponseBytes<GetObjectResponse> object = s3.getObjectAsBytes(
                    GetObjectRequest.builder().bucket(planBucket).key(sourceRef).build());
            return Optional.of(object.asUtf8String());
        } catch (Exception e) {
            log.warn("plan 取得失敗 bucket={} key={}: {}", planBucket, sourceRef, e.toString());
            return Optional.empty();
        }
    }
}
