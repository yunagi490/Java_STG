# VSCDXSTG (Java版)

[yunagi490/VSCDXSTG](https://github.com/yunagi490/VSCDXSTG)(C++ / DxLib 製の弾幕STG)を、外部ライブラリなし(Swing / Java2D のみ)で Java に作り直したものです。

## 動かし方

JDK 17 以上が必要です。

```
mvn package
java -jar target/vscdxstg-0.1.0.jar
```

Maven を使わない場合:

```
mkdir -p target/classes
javac -encoding UTF-8 -d target/classes src/main/java/vscdxstg/*.java
cp -r src/main/resources/* target/classes/
java -cp target/classes vscdxstg.Main
```

### オプション

| オプション | 内容 |
| --- | --- |
| `--enemy-csv=PATH` | 同梱の `Enemy.csv` の代わりに外部ファイルを読む(再ビルド不要で敵配置を調整できる) |
| `--seed=N` | 乱数シード固定 |
| `--d6=N` | ボス撃破後の 1d6 を常に N にする(`--d6=6` で 666 → ExBoss をすぐ確認できるデバッグ用) |

## 操作

| キー | 動作 |
| --- | --- |
| ↑↓←→ | 移動 |
| Z | ショット(押しっぱなし)/ 決定 |
| Shift | 低速移動(当たり判定を表示) |
| P / Esc | ポーズ |

## ゲームの流れ(`ゲーム案/` の仕様を実装)

1. **ステージ開始時に 1d100** → 難易度(1〜100)。大きいほど弾が速く、発射間隔が短く、敵HPが増える。
2. `Enemy.csv` に書いた敵が順に出現。全員倒す(または画面外へ)とボスが登場。
3. **ボス撃破時に 1d6**。6 はファンブルで、次ステージは強制的に難易度 100。
4. 直近3回の 1d6 がすべて 6(666)だと、ボス撃破後に ExBoss が出現。
   - コーミャ: 近づいて撃つ → 離れて撃つ を繰り返し、たまにリスキル(復活地点を狙い撃ち)。
   - スズ: 遠距離から狙い続け、たまに消えて奇襲してくる。

## Enemy.csv

列: `敵種類,弾種類,移動パターン,発射パターン,出現時間,停止,発射,帰還,X座標,Y座標,弾スピード,HP,アイテム`
(時間の単位はフレーム、座標はプレイフィールド内。`アイテム`列は元実装と同じく読むだけで未使用)

移動パターン: 0=停止して戻る / 1=下へ直進 / 2=左に流れる / 3=右に流れる
発射パターン: 0=直線5発 / 1=直線連射 / 2=3方向 / 3=乱射

## 構成

| クラス | 元のC++ |
| --- | --- |
| `Game` | `CONTROL` |
| `Player` | `PLAYER` |
| `Enemy` | `ENEMY` |
| `ExBossKomy` / `ExBossSuzu` | `ExBoss_KOMY` / `ExBoss_Suzu` |
| `Renderer` / `Assets` | `BACK` ほか描画処理 |
| `EnemyData` | `ENEMYDATA` + CSV読み込み |

`Game` は Swing に依存しないので、画面なしでテストできます。

```
java -Djava.awt.headless=true -cp target/classes:target/test-classes vscdxstg.HeadlessSim
```
