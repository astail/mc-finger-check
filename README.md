# FingerCheck

**プレイヤーに「指のトレーニング」的なルールを課す**プラグインです（Paper 用 / サーバー側のみ）。

> **クライアント MOD は不要です。** サーバーにこのプラグインを入れるだけで、バニラのクライアントでもそのまま動作します。

## ルール

### 1. 走っていないと動けない

W（前進）だけで **2 秒以上** 走らずに歩き続けると、画面に「**Shift も押してね！**」と注意が出て、**その場から動けなくなります**。
W を離して押し直すか、走れば（またはスニークすれば）また動けます。走りながら移動するのがこのサーバーのルールです。

次の場合は注意しません。

- スニーク（ゆっくり歩き）中
- 飛行中・エリトラ滑空中・乗り物に乗っている・水中/溶岩・はしご等
- そもそも走れない状況（満腹度 6 以下・盲目・食事や弓を引くなどアイテム使用中）
- クリエイティブ / スペクテイター
- 走るキーを押している（壁に当たってダッシュが切れた場合など）

## 動作要件

- サーバー: Paper 26.2（experimental チャンネル）
- Java: 25
- クライアント: バニラで可（MOD 不要・サーバー側のみ）

## コマンド

| コマンド | 説明 | 権限 |
| --- | --- | --- |
| `/fingercheck status`（`/fc`） | 現在の設定を表示 | `fingercheck.use`（既定: 全員） |
| `/fingercheck reload` | `config.yml` を再読み込み | `fingercheck.manage`（既定: OP） |

## 権限

| 権限 | 既定 | 説明 |
| --- | --- | --- |
| `fingercheck.use` | true | `/fingercheck status` |
| `fingercheck.manage` | op | `/fingercheck reload` |
| `fingercheck.bypass` | false | ルールの対象外にする（OP にも既定では付きません） |

## 設定（config.yml）

| キー | 既定 | 説明 |
| --- | --- | --- |
| `enabled` | `true` | ルールを有効にするか |
| `threshold-seconds` | `2.0` | 何秒走らずに歩いたら注意するか（0.5〜60） |
| `display` | `actionbar` | 表示先。`actionbar` / `chat` / `both` |
| `block-movement` | `true` | 注意と同時に動けなくするか（`false` なら注意だけ） |
| `message` | `Shift も押してね！` | 注意のメッセージ |

## ビルド

```bash
./deploy.sh   # target/FingerCheck-0.1.0.jar
```

## ライセンス

MIT
