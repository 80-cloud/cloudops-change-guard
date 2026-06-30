package com.cloudopsguard.aws;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TerraformPlanAdapter の結合テスト（LocalStack の S3・実 AWS/課金なし）。
 * バケットに plan テキストを置き、キー指定で取得できること・存在しないキーは empty を検証。
 */
@Testcontainers
class TerraformPlanAdapterLocalStackTest {

    private static final String BUCKET = "plans";

    @Container
    static final LocalStackContainer LOCALSTACK = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:3"))
            .withServices(LocalStackContainer.Service.S3);

    private static S3Client s3;
    private static CloudopsAwsProperties props;

    @BeforeAll
    static void setUp() {
        s3 = S3Client.builder()
                .endpointOverride(LOCALSTACK.getEndpoint())
                .region(Region.of(LOCALSTACK.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(LOCALSTACK.getAccessKey(), LOCALSTACK.getSecretKey())))
                .forcePathStyle(true)
                .build();
        s3.createBucket(CreateBucketRequest.builder().bucket(BUCKET).build());
        s3.putObject(PutObjectRequest.builder().bucket(BUCKET).key("dev/plan.txt").build(),
                RequestBody.fromString("# aws_db_instance.main will be destroyed"));
        props = new CloudopsAwsProperties(LOCALSTACK.getRegion(), LOCALSTACK.getEndpoint().toString(), BUCKET);
    }

    @AfterAll
    static void tearDown() {
        if (s3 != null) {
            s3.close();
        }
    }

    @Test
    void S3のplanテキストをキー指定で取得する() {
        TerraformPlanAdapter adapter = new TerraformPlanAdapter(s3, props);
        Optional<String> text = adapter.fetchPlanText("dev/plan.txt");
        assertThat(text).contains("# aws_db_instance.main will be destroyed");
    }

    @Test
    void 存在しないキーはemptyを返す() {
        TerraformPlanAdapter adapter = new TerraformPlanAdapter(s3, props);
        assertThat(adapter.fetchPlanText("does-not-exist.txt")).isEmpty();
    }
}
