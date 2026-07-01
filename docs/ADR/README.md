# ADR（アーキテクチャ決定記録）

設計上の重要な判断と、その理由・代替案・結果を短く記録する。

- [ADR-0001 変更申請の状態遷移をサーバー側で一元管理する](0001-server-side-state-machine.md)
- [ADR-0002 所有権のないリソースは404で応答する](0002-not-found-for-unowned-resources.md)
- [ADR-0003 適用の成否とサービス正常性を分けて扱う](0003-separate-apply-success-from-service-health.md)
- [ADR-0004 外部状態の取得を Port/Adapter で抽象化する](0004-port-adapter-for-external-state.md)
- [ADR-0005 ランタイム/フレームワークのバージョン方針（Java 25 と Spring Boot）](0005-runtime-version-strategy.md)
