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

### 画面の使い方

1. 日付・食事の種類（朝食・昼食・夕食・間食）を選ぶ
2. 食品と量（g）を入力して「リストに追加」を繰り返す
3. 「この食事を記録する」を押すと、食事ごとと1日の合計の栄養素が表示される

### 構成（`app/src/main/java/com/example/healthapp/`）

```
MainActivity.kt        画面の起動
nutrition/Nutrition.kt 栄養計算（100g換算・合計）の純粋関数
data/HealthDatabase.kt SQLite のスキーマと初期データ投入（SQLiteOpenHelper）
data/MealRepository.kt 食事の保存・日別集計
data/SeedFoods.kt      初期登録の食品データ（100gあたり）
data/Models.kt         データクラス
ui/MealViewModel.kt    画面の状態管理
ui/MealScreen.kt       Compose の画面
ui/Format.kt           表示用の整形
```

### 注意

- 初期食品の栄養値は日本食品標準成分表を参考にした**概算値**です。正確な値が必要な場合は公式の成分表で確認してください。
- 食品の追加は現時点では画面からできず、`SeedFoods.kt` を編集して追加します（DB は初回作成時にのみ投入されるため、追加後はアプリのデータを消去して再インストールが必要です）。

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
