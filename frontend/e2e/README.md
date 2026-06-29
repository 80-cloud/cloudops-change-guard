# E2E テスト（Playwright）

申請→承認→実施→監査の通しをブラウザ操作で自動検証する土台。現状は読み取り中心の回帰スイート。

## 前提（実行前に3サービスを起動）
- DB: `docker-compose up -d`（5436）
- backend: `cd backend && ./gradlew bootRun`（8084・空DBならデモ用の申請データが入る）
- frontend: `cd frontend && npm run dev`（5177）

CI 常設は backend と DB が必要で重いため、当面はローカル実行のみ。

## 実行

```
cd frontend
npm run e2e
npm run e2e:ui
```

## 補足
- 認証は seed ユーザー（admin / req1 / rev1 / rev2 / op1）を使う。
- 一覧・承認待ちのテストはデモ用データ（空DB起動時に投入される4件）の存在を前提にする。
  手元でデータを大きく変えた場合は `docker-compose down -v` で初期状態に戻してから実行する。
