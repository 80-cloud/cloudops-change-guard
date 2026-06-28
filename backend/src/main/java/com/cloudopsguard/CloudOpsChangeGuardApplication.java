package com.cloudopsguard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * CloudOps Change Guard 起動クラス。
 * AWS 変更申請の起案→承認→実施→監査を、サーバー側の状態機械と RBAC で強制する。
 */
@SpringBootApplication
@ConfigurationPropertiesScan   // @ConfigurationProperties（JwtProperties / SeedProperties）を有効化
public class CloudOpsChangeGuardApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloudOpsChangeGuardApplication.class, args);
    }
}
