# E2UConverter

z/OS 上の EBCDIC ソースファイルを UTF-8 に一括変換する CLI ツールです。
PDS メンバーをバイナリーモードで PC に転送したファイルを Git で版管理できる UTF-8 テキストへ変換します。

変換処理では、制御文字・SO/SI 不正ペア・変換不能コードなど、テキストエディターや Git での編集時に不具合を引き起こす可能性のある HEX コードを不正コードとして検知し、Markdown 形式のレポートファイルに詳細を出力します。

> [!NOTE]
> 本ツールは **IBM Bob**（AI エージェント）により設計・実装されました。実施したテストは基本的な動作確認にとどまります。本番環境での使用前に十分な検証を行った上で、**利用者自身の責任においてご利用ください**。

---

## リポジトリ概要

| 項目 | 内容 |
|------|------|
| 実装言語 | Java 11 |
| ビルドツール | Maven 3.x |
| 実行形態 | fat JAR（`e2uconverter-1.0.0-shaded.jar`） + Windows バッチファイル（`e2u.bat`） |
| 対応コードページ | IBM-930, IBM-939, IBM-1390, IBM-1399 等（Java / IBM ICU4J が対応するコードページ） |
| テスト結果 | 81 ケース / 81 合格（v1.0.0） |

### ディレクトリ構成

```
E2UConverter/
├── e2u.bat                          # Windows 実行バッチファイル
├── 01_requirements/                 # 要件定義書
├── 02_design/                       # 設計書（アーキテクチャ／クラス／シーケンス等）
├── 03_implementation/               # Mavenプロジェクト（ソースコード）
│   └── e2uconverter/
│       ├── pom.xml
│       ├── src/main/java/com/example/e2uconverter/
│       └── target/
│           └── e2uconverter-1.0.0-shaded.jar   # 実行可能JAR
├── 04_test/                         # テスト計画・テスト結果
└── 91_testdata/                     # サンプルテストデータ
```

---

## E2UConverter 機能

### 入力モード

| モード | オプション | 説明 |
|--------|-----------|------|
| 固定長レコードモード（デフォルト） | `-m fixed` | LRECL（`-l`）バイト単位でレコードを分割して変換。デフォルト LRECL は 80 バイト。 |
| バイトストリームモード | `-m stream` | EBCDIC の `x'0D'`（CR）・`x'15'`（NL）・`x'25'`（NL25）を行区切りとして処理。 |

### コマンドラインオプション一覧

| オプション | 必須/任意 | 説明 | デフォルト |
|-----------|----------|------|----------|
| `-i <path>` | **必須** | 入力ファイルまたはディレクトリのパス | — |
| `-o <dir>` | **必須** | 変換後ファイルの出力先ディレクトリ | — |
| `-c <codepage>` | 任意 | EBCDIC コードページ（Java コンバーター名） | `IBM-930` |
| `-m <mode>` | 任意 | 入力モード（`fixed` / `stream`） | `fixed` |
| `-l <length>` | 任意 | 固定長レコードモード時の LRECL（バイト数） | `80` |
| `-r` | 任意 | ディレクトリ入力時にサブディレクトリを再帰処理 | 無効 |
| `-e <ext>` | 任意 | 出力ファイルの拡張子（ドットなし、例: `txt`） | 元の拡張子を保持 / 拡張子なしの場合は `.txt` |
| `-R <file>` | 任意 | レポートの出力先ファイルパス | `<出力先ディレクトリ>/report.md` |

### 主な機能

- **一括変換**: ディレクトリを指定するとディレクトリ内のファイルを一括変換。`-r` で再帰処理も対応。
- **ディレクトリ構造の再現**: 入力ディレクトリの相対パス構造を出力先に再現。
- **不正文字検知**: 制御文字（`x'00'`〜`x'3F'` 範囲）・SO/SI 不正ペア・変換不能コード（`x'FF'`）を検知し、`?` に置換して変換を継続。
- **Markdown レポート出力**: 変換処理の結果サマリー・不正文字詳細・SBCS コード変換対応表（256 エントリ）を Markdown 形式で出力。
- **出力先の自動作成**: 出力先ディレクトリが存在しない場合、自動的に作成。
- **エラー耐性**: 個別ファイルの読み込みエラーはレポートに記録して処理を継続。

---

## E2UConverter 使用例

### 事前準備

1. **IBM Semeru Runtime 11 以上**をインストール
   - EBCDIC コードページ（IBM-930, IBM-939 等）のコンバーターは IBM Semeru に含まれています
   - IBM-1390 / IBM-1399 は IBM Semeru V11 以降には組み込まれていないため、本ツールでは **IBM ICU4J**（`icu4j` / `icu4j-charset`）を fat JAR に同梱することで対応しています
2. 本リポジトリをクローン

```bat
git clone https://github.com/<your-org>/E2UConverter.git
cd E2UConverter
```

3. JAR をビルド（既にビルド済みの JAR を使用する場合はスキップ）

```bat
cd 03_implementation\e2uconverter
mvn package -DskipTests
cd ..\..
```

---

### 例1: 単一ファイルを IBM-930 で変換（固定長・LRECL=80）

```bat
e2u.bat -i C:\work\input\MYPROG -o C:\work\output
```

- 出力: `C:\work\output\MYPROG.txt`（拡張子なしの場合は `.txt` を付与）
- レポート: `C:\work\output\report.md`

---

### 例2: ディレクトリを一括変換（再帰処理あり・IBM-939）

```bat
e2u.bat -i C:\work\input -o C:\work\output -c IBM-939 -r
```

- `C:\work\input` 配下のすべてのファイル（サブディレクトリ含む）を変換
- ディレクトリ構造を `C:\work\output` 配下に再現

---

### 例3: バイトストリームモードで変換し、拡張子を .cbl に指定

```bat
e2u.bat -i C:\work\input -o C:\work\output -m stream -e cbl
```

- EBCDIC の `x'0D'`/`x'15'`/`x'25'` を行区切りとして処理
- すべての出力ファイルに `.cbl` 拡張子を付与

---

### 例4: IBM-1399 コードページ・固定長 LRECL=72・レポートを別パスに出力

```bat
e2u.bat -i C:\work\input -o C:\work\output -c IBM-1399 -l 72 -R C:\work\report\conv_report.md
```

---

### 例5: テストデータを使用した動作確認

本リポジトリに含まれるサンプルデータで動作確認できます。

```bat
e2u.bat -i .\91_testdata\ -o .\92_testoutput\ -e txt -c IBM-1399
```

---

### レポート出力サンプル（一部）

変換後、出力先ディレクトリに `report.md` が生成されます。

```markdown
## 実行サマリー
| 項目 | 内容 |
|------|------|
| 処理日時 | 2026-07-12 10:00:00 |
| 処理対象 | C:\work\input |
| 出力先 | C:\work\output |
| 使用コードページ | IBM-930 |
| 入力モード | fixed (LRECL=80) |
| 再帰処理 | 無効 |

## 処理統計
| 処理ファイル総数 | 変換成功 | 不正文字検知 | エラー |
|----------------|---------|------------|--------|
| 3 | 2 | 1 | 0 |
```

---

## ビルド方法

```bat
cd 03_implementation\e2uconverter
mvn package
```

ビルド成果物: `03_implementation\e2uconverter\target\e2uconverter-1.0.0-shaded.jar`

---

## 動作要件

| 項目 | 要件 |
|------|------|
| Java | IBM Semeru Runtime 11 以上（EBCDIC コンバーター内蔵のため必須） |
| OS | Windows |
| Maven | 3.x（ビルド時のみ） |
