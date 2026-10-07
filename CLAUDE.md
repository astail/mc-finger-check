# CLAUDE.md

Claude がこのリポジトリで作業する際の開発メモ（Paper プラグイン）。

## プラグインの目的

FingerCheck は、プレイヤーに「指のトレーニング」的なルールを課す。ルールは今後追加していく予定。
サーバー側のみで動き、クライアント MOD は不要。

- **ルール 1「走っていないと動けない」**: 前進キーを押しているのに走っていない状態が `threshold-seconds`（既定 2 秒）
  続いたら「Shift も押してね！」と注意し、W を離して押し直すか走る（またはスニークする）まで動けなくする。
  スニーク中・飛行中などは対象外。

## ビルド要件

- Java 25 + Maven。生成物は `FingerCheck-0.1.0.jar`。
- 唯一の依存は `io.papermc.paper:paper-api:26.2.build.40-alpha`（provided）。
- ローカルビルドは `./deploy.sh`（Homebrew `openjdk@25` を想定）。

## アーキテクチャ構成

- **FingerCheckPlugin**: 本体。設定読込、`runTaskTimer`（2 tick ごと）で全員を判定、注意の表示（アクションバー / チャット）と移動ロックの付け外し、
  `PlayerInputEvent` での即時解除、`PlayerJoinEvent` / `PlayerQuitEvent` での状態破棄。
- **MovementLock**: 違反中プレイヤーの移動ロック。`MOVEMENT_SPEED` に transient modifier（`MULTIPLY_SCALAR_1` -1 = 0 倍）を
  付けてクライアント側でも止め、空中移動（ジャンプ中は attribute が効かない）は `PlayerMoveEvent` で水平移動を打ち消す。
- **FingerCheckCommand**: `/fingercheck <status|reload>` の実処理とタブ補完。
- **SprintRule**: ルール 1 の判定と状態（`walkingSince` / `lastWarnedAt`）。`update` が `NONE / FIRST / REPEAT` を返す。

## 設計上の要点

- **キー入力は `Player#getCurrentInput()`**（Paper の `org.bukkit.Input`）。W は `isForward()`、走りは
  `Player#isSprinting()` に加え `Input#isSprint()`（走るキー押下中）も OK 扱い。壁に当たってバニラがダッシュを切った場合の誤検知を防ぐ。
- **誤検知を避けるため、走れない／走る意味がない状況は全て対象外**: スニーク、飛行・滑空・騎乗・水/溶岩・はしご、
  満腹度 6 以下・盲目・アイテム使用中（`isHandRaised`）、サバイバル/アドベンチャー以外。
- **注意の頻度**: アクションバーは違反継続中 1 秒ごとに再表示（数秒で消えるため）。チャットは 1 回の違反につき最初の 1 回だけ。音は鳴らさない。
  条件が崩れたら状態をリセットし、次に 2 秒続いたら再び注意する。
- **ロック中もタスク判定は続く**: ロック中も W 押しっぱなし・非ダッシュなら違反扱いのまま＝ロック継続。解除は `PlayerInputEvent` で即時
  （W を離す・走るキー・スニーク）。速度 0 でもクライアントはダッシュ開始できる（開始判定は入力の強さで速度は見ない）。
- **`fingercheck.bypass`** は `default: false`（OP にも付かない）。
