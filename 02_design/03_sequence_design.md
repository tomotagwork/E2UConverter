# シーケンス設計書 — E2UConverter

## 1. シーケンス図一覧

| No. | シーケンス名 | 概要 |
|---|---|---|
| SD-01 | 起動・引数解析シーケンス | プログラム起動から変換処理開始までの流れ |
| SD-02 | ファイル変換シーケンス（固定長モード） | 単一ファイルを固定長レコードで変換する流れ |
| SD-03 | ファイル変換シーケンス（バイトストリームモード） | 単一ファイルをバイトストリームで変換する流れ |
| SD-04 | レポート出力シーケンス | 全ファイル処理後にレポートを出力する流れ |
| SD-05 | 不正文字検知シーケンス | 1 レコードの不正文字検知の内部処理 |

---

## 2. SD-01: 起動・引数解析シーケンス

```mermaid
sequenceDiagram
    actor User
    participant Main
    participant CliOptions
    participant CodePageUtil
    participant FileConverter

    User->>Main: java -jar e2uconverter.jar [args]
    Main->>CliOptions: parse(args)
    alt 引数解析エラー（必須オプション未指定 等）
        CliOptions-->>Main: ParseException
        Main->>User: エラーメッセージ出力 (stderr)
        Main->>Main: System.exit(1)
    else 解析成功
        CliOptions-->>Main: CliOptions インスタンス
    end

    Main->>CodePageUtil: isSupported(codePage)
    alt コードページ未サポート
        CodePageUtil-->>Main: false
        Main->>User: エラーメッセージ出力 (stderr)
        Main->>Main: System.exit(1)
    else サポート済み
        CodePageUtil-->>Main: true
    end

    Main->>FileConverter: convert(options)
    FileConverter-->>Main: 処理完了
    Main->>User: 正常終了
```

---

## 3. SD-02: ファイル変換シーケンス（固定長モード）

```mermaid
sequenceDiagram
    participant FileConverter
    participant FixedRecordReader
    participant InvalidCharDetector
    participant ReportData

    FileConverter->>FileConverter: collectFiles(inputPath)
    Note over FileConverter: ファイルリスト作成（再帰オプション考慮）

    loop 各ファイル
        FileConverter->>FileConverter: resolveOutputFile(inputFile, baseDir)
        FileConverter->>FixedRecordReader: new FixedRecordReader(file, lrecl)

        loop 各レコード
            FileConverter->>FixedRecordReader: readRecord()
            alt EOF
                FixedRecordReader-->>FileConverter: null
            else レコードあり
                FixedRecordReader-->>FileConverter: byte[] record

                FileConverter->>InvalidCharDetector: detect(record, recordNo, fileName)
                InvalidCharDetector-->>FileConverter: List<InvalidCharEntry>

                FileConverter->>FileConverter: Charset 変換（変換不能→'?'置換）
                FileConverter->>FileConverter: UTF-8 ファイルに書き出し（末尾に LF 付与）
            end
        end

        FileConverter->>FixedRecordReader: close()
        FileConverter->>ReportData: addInvalidCharEntries(entries)
        FileConverter->>ReportData: addFileResult(FileResult)
        FileConverter->>FileConverter: 標準出力に進捗表示
    end
```

---

## 4. SD-03: ファイル変換シーケンス（バイトストリームモード）

```mermaid
sequenceDiagram
    participant FileConverter
    participant StreamRecordReader
    participant InvalidCharDetector
    participant ReportData

    FileConverter->>FileConverter: collectFiles(inputPath)
    Note over FileConverter: ファイルリスト作成（再帰オプション考慮）

    loop 各ファイル
        FileConverter->>FileConverter: resolveOutputFile(inputFile, baseDir)
        FileConverter->>StreamRecordReader: new StreamRecordReader(file)

        loop 各行
            FileConverter->>StreamRecordReader: readRecord()
            alt EOF
                StreamRecordReader-->>FileConverter: null
            else 行データあり
                StreamRecordReader-->>FileConverter: byte[] record（行区切りコード除く）

                FileConverter->>InvalidCharDetector: detect(record, lineNo, fileName)
                InvalidCharDetector-->>FileConverter: List<InvalidCharEntry>

                FileConverter->>FileConverter: Charset 変換（変換不能→'?'置換）
                FileConverter->>FileConverter: UTF-8 ファイルに書き出し（末尾に LF 付与）
            end
        end

        FileConverter->>StreamRecordReader: close()
        FileConverter->>ReportData: addInvalidCharEntries(entries)
        FileConverter->>ReportData: addFileResult(FileResult)
        FileConverter->>FileConverter: 標準出力に進捗表示
    end
```

---

## 5. SD-04: レポート出力シーケンス

```mermaid
sequenceDiagram
    participant FileConverter
    participant ReportWriter
    participant ReportData
    participant FileSystem

    FileConverter->>FileConverter: 全ファイル変換完了
    FileConverter->>ReportWriter: write(reportData, options, startTime)

    ReportWriter->>ReportData: getFileResults()
    ReportData-->>ReportWriter: List<FileResult>

    ReportWriter->>ReportData: getInvalidCharEntries()
    ReportData-->>ReportWriter: List<InvalidCharEntry>

    ReportWriter->>ReportWriter: buildSummarySection(options, startTime)
    ReportWriter->>ReportWriter: buildStatisticsSection(reportData)
    ReportWriter->>ReportWriter: buildFileListSection(reportData)
    ReportWriter->>ReportWriter: buildErrorListSection(reportData)
    ReportWriter->>ReportWriter: buildInvalidCharSection(reportData)
    ReportWriter->>ReportWriter: buildCodeTableSection(options)
    Note over ReportWriter: x'00'〜x'FF' の 256 エントリを生成

    ReportWriter->>FileSystem: report.md を UTF-8 で書き出し
    FileSystem-->>ReportWriter: 書き出し完了
    ReportWriter-->>FileConverter: 完了
```

---

## 6. SD-05: 不正文字検知シーケンス

```mermaid
sequenceDiagram
    participant FileConverter
    participant InvalidCharDetector

    FileConverter->>InvalidCharDetector: detect(record, recordNo, fileName)

    Note over InvalidCharDetector: ① 制御文字・変換不能コード検査

    loop record の各バイト b（offset i）
        alt isControlChar(b) == true
            InvalidCharDetector->>InvalidCharDetector: InvalidCharEntry 生成（理由: 制御文字 / 変換不能）
        end
    end

    Note over InvalidCharDetector: ①prime コードページ未定義文字検査（0x40 - 0xFE、DBCS 区間外）

    loop record の各バイト b（offset i）
        alt b == SO
            InvalidCharDetector->>InvalidCharDetector: inDbcs = true、スキップ
        else b == SI
            InvalidCharDetector->>InvalidCharDetector: inDbcs = false、スキップ
        else inDbcs == true
            InvalidCharDetector->>InvalidCharDetector: DBCS データバイトのためスキップ
        else 0x40 <= b <= 0xFE
            InvalidCharDetector->>InvalidCharDetector: convertSingleByte(b) 呼び出し
            alt 変換結果が "-"（コードページ未定義）
                InvalidCharDetector->>InvalidCharDetector: InvalidCharEntry 生成（理由: 未定義文字）
            end
        end
    end

    Note over InvalidCharDetector: ② SO/SI ペア検証

    InvalidCharDetector->>InvalidCharDetector: validateSoSi(record, recordNo, fileName)

    Note over InvalidCharDetector: SO/SI スタック方式で走査
    loop record の各バイト b
        alt b == 0x0E (SO)
            alt 既に SO が開いている（ネスト）
                InvalidCharDetector->>InvalidCharDetector: InvalidCharEntry 生成（理由: SO/SI不正）
            else
                InvalidCharDetector->>InvalidCharDetector: SO 位置を記録
            end
        else b == 0x0F (SI)
            alt SO が未開
                InvalidCharDetector->>InvalidCharDetector: InvalidCharEntry 生成（理由: SO/SI不正）
            else SO の直後が SI（空 DBCS）
                InvalidCharDetector->>InvalidCharDetector: InvalidCharEntry 生成（理由: SO/SI不正）
            else
                InvalidCharDetector->>InvalidCharDetector: SO/SI ペア正常、クリア
            end
        end
    end

    alt レコード末尾で SO が未クローズ
        InvalidCharDetector->>InvalidCharDetector: InvalidCharEntry 生成（理由: SO/SI不正）
    end

    InvalidCharDetector-->>FileConverter: List<InvalidCharEntry>
```

---

## 7. エラー発生時のシーケンス

### 7-1. 個別ファイル読み込みエラー時

```mermaid
sequenceDiagram
    participant FileConverter
    participant ReportData

    FileConverter->>FileConverter: convertFile(inputFile, baseDir)
    FileConverter->>FileConverter: new FixedRecordReader(file, lrecl)
    Note over FileConverter: IOException 発生
    FileConverter->>ReportData: addFileResult(FileResult[status=ERROR, errorMessage=...])
    FileConverter->>FileConverter: 標準出力にエラーファイルを表示
    FileConverter->>FileConverter: 次のファイルへ処理継続
```

### 7-2. 出力先ディレクトリが存在しない場合

```mermaid
sequenceDiagram
    participant FileConverter
    participant FileSystem

    FileConverter->>FileConverter: resolveOutputFile(inputFile, baseDir)
    FileConverter->>FileSystem: outputDir.mkdirs()
    FileSystem-->>FileConverter: ディレクトリ作成完了
    FileConverter->>FileConverter: 変換処理継続
```
