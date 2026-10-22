# 健康管理アプリ

食事を入力すると、栄養素（エネルギー・たんぱく質・脂質・炭水化物・食物繊維・ナトリウム）を自動で計算するアプリです。
第1段階として「食事記録と栄養計算」を実装しています。

- **Android アプリ（本命）**: Kotlin + Jetpack Compose。プロジェクトのルートがそのまま Android Studio のプロジェクトです。
- **Python 版（参考実装）**: 栄養計算の仕様確認用に残している、標準ライブラリだけで動く Web 版です。

## Android アプリ

### 開き方と実行

1. Android Studio で `C:\kait\askapp` フォルダを開く（File → Open）
2. Gradle の同期が終わったら、▶（Run）で実機またはエミュレーターに実行
3. テストは `app/src/test` の単体テストを Run する（または `./gradlew testDebugUnitTest`）

動作確認環境: Android Gradle Plugin 8.13.1 / Kotlin 2.2.21 / Gradle 9.3.1 / minSdk 26 / targetSdk 36

### 機能一覧

| タブ | 機能 |
| --- | --- |
| 記録 | 食事の記録（日付・朝昼夕間食・食品・量）、記録済みの食事の編集・削除、食事ごとと1日の合計の栄養素 |
| 食品 | 食品の追加・編集・削除、名前での検索（100gあたりの栄養素）、バーコード（番号入力・カメラ）で商品情報を取得して登録 |
| 運動 | 運動の種類と時間を記録し、消費カロリーを計算（日付ごとに表示・削除） |
| 体重 | 日付ごとの体重記録（同じ日は上書き）、削除、推移グラフ、最初の記録からの増減 |
| 目標 | 1日あたりの目標値の設定、今日の達成率（バー表示）、直近7日間の記録 |

- 食品の削除は、食事の記録で使われている場合はできません（記録を先に消す必要があります）。
- バーコードの検索は、まず端末に登録済みの食品を探します。なければ [Open Food Facts](https://world.openfoodfacts.org)（オープンな食品データベース）から取得し、内容を確認してから保存します。取得データは ODbL ライセンスの Open Food Facts のものです。
- 取得できない栄養素（食物繊維・ナトリウムなど）は 0 として入ります。保存前に確認してください。
- バーコード読み取りには Google Code Scanner（Google Play 開発者サービス）を使います。インターネット接続が必要です。
- 消費カロリーは `MET × 体重(kg) × 時間(h)` で計算します。体重は記録した日以前で最も新しいものを使い、体重の記録がなければ60kgで計算します。MET値は目安です。
- 目標値の既定値は 2000kcal / たんぱく質60g / 脂質60g / 炭水化物300g / 食物繊維20g / ナトリウム2300mg です。

### 画面の使い方（記録）

1. 日付・食事の種類（朝食・昼食・夕食・間食）を選ぶ
2. 食品と量（g）を入力して「リストに追加」を繰り返す
3. 「この食事を記録する」を押すと、食事ごとと1日の合計の栄養素が表示される
4. 記録済みの食事の「編集」で内容を変更、「削除」で取り消せる

### 構成（`app/src/main/java/com/example/healthapp/`）

```
HealthApp.kt           Application（DB 接続を共有）
MainActivity.kt        下部タブで4画面を切り替え
nutrition/Nutrition.kt 栄養計算（100g換算・合計）の純粋関数
nutrition/Progress.kt  達成率・体重増減の計算
data/HealthDatabase.kt SQLite のスキーマ（version 2）と初期データ投入
data/FoodRepository.kt 食品の保存・更新・削除・バーコード検索
data/OpenFoodFactsClient.kt バーコードから商品情報を取得（Open Food Facts）
data/MealRepository.kt 食事の保存・更新・削除・日別集計
data/BodyRepository.kt 目標値・体重の保存
data/ExerciseRepository.kt 運動記録の保存
data/ExerciseCatalog.kt 運動の種類と MET 値
nutrition/Exercise.kt  消費カロリーの計算
nutrition/Units.kt     kJ → kcal の換算
data/SeedFoods.kt      初期登録の食品データ（100gあたり）
data/Models.kt         データクラス
ui/MealScreen.kt, MealViewModel.kt   記録タブ
ui/FoodScreen.kt, FoodViewModel.kt   食品タブ
ui/ExerciseScreen.kt, ExerciseViewModel.kt 運動タブ
ui/WeightScreen.kt, WeightViewModel.kt 体重タブ
ui/GoalScreen.kt, GoalViewModel.kt   目標タブ
ui/Common.kt           日付の移動ボタンなど共通部品
ui/Format.kt           表示用の整形
```

### 注意

- 初期食品の栄養値は日本食品標準成分表を参考にした**概算値**です。正確な値が必要な場合は公式の成分表で確認してください。
- DB は version 4 です。v1〜v3 からは自動で移行されます（目標値・体重・運動の表、食品のバーコード列を追加）。

## Python 版（参考実装）

外部ライブラリは使わず、Python 標準ライブラリ（`sqlite3`, `http.server`）だけで動きます。

### 必要なもの

- Python 3.10 以上

### 起動方法

```bash
python -m health_app
```

ブラウザで http://127.0.0.1:8000 を開くと入力画面が表示されます。

オプション:

| オプション | 既定値 | 内容 |
| --- | --- | --- |
| `--port` | `8000` | ポート番号 |
| `--host` | `127.0.0.1` | 待ち受けアドレス |
| `--db` | `health.db` | SQLite のファイルパス（初回起動時に自動作成） |

## テスト

```bash
python -m unittest discover -s tests
```

## 構成

```
health_app/
  nutrition.py   栄養計算の純粋関数（100g換算・合計）
  service.py     入力検証と業務ロジック
  db.py          SQLite のスキーマと接続
  seed_foods.py  初期登録する食品データ
  server.py      HTTP サーバーと API ルーティング
  static/        入力画面（HTML/JS）
tests/           単体テスト・API テスト
```

## API

| メソッド | パス | 内容 |
| --- | --- | --- |
| GET | `/api/foods` | 食品一覧 |
| POST | `/api/foods` | 食品を追加（下記参照） |
| POST | `/api/meals` | 食事を記録し、計算結果を返す |
| GET | `/api/meals?date=YYYY-MM-DD` | 指定日の食事一覧と1日の合計 |

食事の記録例:

```json
{
  "eaten_on": "2026-10-22",
  "meal_type": "breakfast",
  "items": [
    { "food_id": 1, "grams": 150 },
    { "food_id": 7, "grams": 50 }
  ]
}
```

`meal_type` は `breakfast` / `lunch` / `dinner` / `snack` のいずれかです。

食品の追加例（値はすべて 100g あたり）:

```json
{
  "name": "オートミール",
  "kcal": 380, "protein_g": 13.7, "fat_g": 5.9,
  "carbs_g": 69.1, "fiber_g": 9.7, "sodium_mg": 2
}
```

## 注意

- `seed_foods.py` の食品データは日本食品標準成分表を参考にした**概算値**です。
  正確な値が必要な場合は公式の成分表で確認してください。
- 栄養素の計算は `nutrition.py` の `calc_nutrients` で行っています。

## 今後の予定（案）

- 目標値（カロリー・PFC など）との比較
- 体重や運動など他の健康データの記録
- 食品の検索・追加を画面から行えるようにする
