# エラー処理設計書 — E2UConverter

## 1. エラーの分類

E2UConverter のエラーは、発生箇所と影響範囲によって以下の 2 種類に分類する。

| 種別 | 説明 | 処理方針 |
|---|---|---|
| **致命的エラー（Fatal Error）** | プログラム全体の処理継続が不可能なエラー。 | 標準エラー出力にメッセージを出力し、`System.exit(1)` で処理中止。 |
| **ファイルエラー（File Error）** | 個別ファイルの処理に失敗するエラー。 | そのファイルをエラーとしてレポートに記録し、次のファイルの処理を継続。 |

---

## 2. 致命的エラー一覧

| No. | エラー状況 | 検出クラス | 出力メッセージ（例） | exit code |
|---|---|---|---|---|
| FE-01 | `-i` オプション未指定 | `CliOptions` | `ERROR: -i option is required.` | 1 |
| FE-02 | `-o` オプション未指定 | `CliOptions` | `ERROR: -o option is required.` | 1 |
| FE-03 | 引数が解析できない形式 | `CliOptions` | `ERROR: Invalid argument: <詳細>` | 1 |
| FE-04 | `-l` オプションに数値以外を指定 | `CliOptions` | `ERROR: -l option must be a positive integer.` | 1 |
| FE-05 | `-m` オプションに不正な値を指定 | `CliOptions` | `ERROR: -m option must be 'fixed' or 'stream'.` | 1 |
| FE-06 | 指定されたコードページが Java で認識されない | `Main` / `CodePageUtil` | `ERROR: Unsupported codepage: IBM-9999` | 1 |
| FE-07 | `-i` で指定したパスが存在しない | `Main` / `FileConverter` | `ERROR: Input path does not exist: C:\work\input` | 1 |

---

## 3. ファイルエラー一覧

| No. | エラー状況 | 検出クラス | レポート記録内容 |
|---|---|---|---|
| PE-01 | ファイルの読み込み中に `IOException` 発生 | `FileConverter` | `ファイル読み込みエラー: <例外メッセージ>` |
| PE-02 | 出力ファイルの書き込み中に `IOException` 発生 | `FileConverter` | `ファイル書き込みエラー: <例外メッセージ>` |
| PE-03 | 出力先ディレクトリの作成に失敗 | `FileConverter` | `出力ディレクトリ作成エラー: <パス>` |

> **PE-03 について**: `mkdirs()` が `false` を返した場合（権限不足等）にファイルエラーとして扱う。

---

## 4. 不正文字検知（エラーではなく WARNING 扱い）

不正文字の検知はエラーではなく **WARNING** として扱い、変換処理は継続する。

| 検知種別 | 処理内容 |
|---|---|
| 制御文字検知 | `InvalidCharEntry` を生成してレポートに記録。変換結果には `?` を出力。 |
| SO/SI ペア不正 | `InvalidCharEntry` を生成してレポートに記録。該当コードに対する変換結果には `?` を出力。 |
| 変換不能コード | `InvalidCharEntry` を生成してレポートに記録。変換結果には `?` を出力。 |
| コードページ未定義文字 | `InvalidCharEntry` を生成してレポートに記録（理由: `未定義文字`）。SO〜SI 区間（DBCS データバイト）は対象外。出力ファイルは Java Charset の REPLACE 設定（`?` への置換）に任せる。 |

---

## 5. エラー処理フロー

### 5-1. 起動時の致命的エラー処理

```
Main.main()
  └── CliOptions.parse(args)
        ├── 必須チェック（-i / -o）→ エラー時: stderr 出力 + exit(1)
        ├── -l の数値チェック      → エラー時: stderr 出力 + exit(1)
        └── -m の値チェック        → エラー時: stderr 出力 + exit(1)

  └── CodePageUtil.isSupported(codePage)
        └── false の場合           → stderr 出力 + exit(1)

  └── 入力パスの存在チェック
        └── 存在しない場合         → stderr 出力 + exit(1)
```

### 5-2. ファイル変換中のエラー処理

```
FileConverter.convertFile(inputFile, baseDir)
  try {
    RecordReader reader = new FixedRecordReader(file, lrecl) または new StreamRecordReader(file)
    // 変換処理
    // UTF-8 書き出し
    reportData.addFileResult(new FileResult(..., Status.OK または WARNING, ...))
  } catch (IOException e) {
    reportData.addFileResult(new FileResult(..., Status.ERROR, e.getMessage()))
    // 標準出力にエラーメッセージ表示
    // 次のファイルへ継続
  }
```

---

## 6. exit code 定義

| exit code | 意味 |
|---|---|
| `0` | 正常終了（不正文字 WARNING があっても正常終了） |
| `1` | 致命的エラーによる処理中止 |

---

## 7. ログ出力方針

本ツールはロギングフレームワークを使用せず、以下の方針でコンソール出力を行う。

| 出力先 | 使用状況 |
|---|---|
| `System.out`（標準出力） | 処理進捗、INFO / WARN メッセージ |
| `System.err`（標準エラー出力） | 致命的エラーメッセージのみ |

### 出力フォーマット

```
[レベル] メッセージ
```

| レベル | 用途 |
|---|---|
| `[INFO]` | 正常処理の進捗表示（変換開始、完了、レポート出力 等） |
| `[WARN]` | 不正文字を検知した場合のファイル処理結果 |
| `[ERROR]` | ファイルエラー（処理継続）、致命的エラー（処理中止） |

---

## 8. 設計上の考慮事項

### 8-1. 出力先ディレクトリの自動作成

要件に従い、`-o` で指定した出力先ディレクトリが存在しない場合は `File.mkdirs()` で自動作成する。  
レポート出力先 (`-R`) の親ディレクトリについても同様に自動作成する。

### 8-2. ファイルエラー時の変換ファイル出力

個別ファイルの変換中に IOException が発生した場合、**途中まで書き込まれた出力ファイルは削除しない**。  
（部分的な変換結果が存在することをレポートで確認できるようにする。）

> ※ 要件に「変換不能文字が含まれる場合でも変換ファイルは必ず出力する」とあるため、途中エラーの場合も同様の方針とする。  
> ただし実装時に再検討が必要であれば、削除する設計にしてもよい。

### 8-3. コードページ検証のタイミング

コードページの検証は、ファイル変換処理を開始する**前**（起動時）に行う。  
ファイル処理中ではなく起動時に検証することで、無駄な処理を防ぐ。

### 8-4. `InvalidCharEntry` における変換後情報の取り扱い

不正文字検知時の `utf8Hex` および `convertedChar` は以下の方針で設定する。

| 状況 | `utf8Hex` | `convertedChar` |
|---|---|---|
| 変換可能（通常文字に変換された場合）| 変換後 UTF-8 の HEX 値 | 変換後文字 |
| 変換不能（`?` に置換された場合） | `-` | `-` |
| 制御文字（表示不能） | 変換後 UTF-8 の HEX 値 | `-` |
| SO/SI 不正 | `-` | `-` |
| コードページ未定義文字 | `-` | `-` |
