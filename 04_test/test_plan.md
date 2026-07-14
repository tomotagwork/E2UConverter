# E2UConverter テスト計画書

## 1. テスト概要

| 項目 | 内容 |
|------|------|
| テスト対象 | E2UConverter v1.0.0（`e2uconverter-1.0.0.jar`） |
| テスト方式 | ブラックボックステスト（コマンドライン実行ベース） |
| テスト範囲 | CLI引数検証 / 固定長レコードモード / バイトストリームモード / 出力ファイル / レポート / エラー処理 |
| コードページ | IBM-930（ベーステスト）/ IBM-1399（追加テスト） |

---

## 2. テスト方針・前提条件

### 2-1. テスト方針

- 全テストをブラックボックステストで実施する。
- 固定長レコードモード（`-m fixed`）とバイトストリームモード（`-m stream`）でテストケースを分けて作成する。
- 固定長レコードモードは `LRECL=80` バイトをベースとする。
- DBCS（SO/SI含む）とSBCSを含むソースをベースとする。
- 境界値分析・同値分割を用いてテストパターンを網羅的に作成する。
- 変換結果の正確性（UTF-8文字内容、LF改行、`?`置換）、不正文字検知の正確性（行番号・オフセット・HEX値・理由）、レポート出力の正確性を重点的に確認する。

### 2-2. 前提条件

- テスト実施PCにJava実行環境（JRE 11以上）が導入済みであること。
- `03_implementation/e2uconverter/target/e2uconverter-1.0.0.jar` がビルド済みであること。
- `04_test/create_test_data.py` を実行してテストデータ（`04_test/data/` 配下）を生成済みであること。
- 以降の実行コマンド例では、ワークスペースルートを `%PROJ%` と表記する（例: `set PROJ=C:\y\IBMBob_workspace\E2UConverter`）。

---

## 3. テスト環境

| 項目 | 内容 |
|------|------|
| OS | Windows 10/11 |
| JRE | Java 11以上 |
| JAR パス | `%PROJ%\03_implementation\e2uconverter\target\e2uconverter-1.0.0.jar` |
| テストデータ | `%PROJ%\04_test\data\` |
| テスト出力先 | `%PROJ%\04_test\output\` |

---

## 4. テストデータ仕様

テストデータは `04_test/create_test_data.py` を実行することで `04_test/data/` 配下に生成される。
各ファイルはEBCDICバイナリ形式（拡張子 `.bin`）で、テストケースIDに対応したファイル名を持つ。

### 4-1. IBM-930 SBCS 主要コード対応表（参照用）

| EBCDIC コード | UTF-8 文字 | 説明 |
|---|---|---|
| `0x40` | ` `（スペース） | EBCDIC スペース |
| `0xF0`〜`0xF9` | `0`〜`9` | 数字 |
| `0xC1`〜`0xC9` | `A`〜`I` | 英大文字 |
| `0xD1`〜`0xD9` | `J`〜`R` | 英大文字 |
| `0xE2`〜`0xE9` | `S`〜`Z` | 英大文字 |
| `0x81`〜`0x89` | `ア`〜`ケ`（半角カナ） | IBM-930のSBCS半角カナ |
| `0x5B` | `¥`（円記号） | IBM-930固有 |

### 4-2. IBM-1399 と IBM-930 の主な差異

IBM-1399 はIBM-930に対してSBCS半角カナの配置が異なる（1つずれている）。
テストに使用する代表的な差異コード:

| EBCDIC コード | IBM-930 変換結果 | IBM-1399 変換結果 |
|---|---|---|
| `0x41` | `｡`（U+FF61） | 変換不能（`?`） |
| `0x42` | `｢`（U+FF62） | `｡`（U+FF61） |
| `0x5B` | `¥`（U+00A5） | `$`（U+0024） |
| `0x81` | `ｱ`（U+FF71） | `a`（U+0061） |
| `0xE0` | `$`（U+0024） | `\`（U+005C） |

### 4-3. テストデータファイル一覧

| ファイル名 | 説明 | 使用テストケース |
|---|---|---|
| `TG-2-001.bin` | SBCSのみ 80バイト×3レコード (IBM-930) | TG-2-001 |
| `TG-2-002.bin` | DBCS含む 80バイト×2レコード (IBM-930) | TG-2-002 |
| `TG-2-003.bin` | SBCS+DBCS混在 80バイト×3レコード (IBM-930) | TG-2-003 |
| `TG-2-004.bin` | ファイルサイズがLRECLの倍数 (80×5=400バイト) | TG-2-004 |
| `TG-2-005.bin` | 最終レコードが80バイト未満（例: 80+80+45=205バイト） | TG-2-005 |
| `TG-2-006.bin` | 空ファイル（0バイト） | TG-2-006 |
| `TG-2-007.bin` | LRECL=72バイト×3レコード (IBM-930) | TG-2-007 |
| `TG-3-001.bin` | 80バイト中に 0x00 を含む（先頭バイト） | TG-3-001 |
| `TG-3-002.bin` | 80バイト中に 0x0D を含む（固定長モード不正） | TG-3-002 |
| `TG-3-003.bin` | 80バイト中に 0x10 を含む（範囲下端） | TG-3-003 |
| `TG-3-004.bin` | 80バイト中に 0x3F を含む（範囲上端） | TG-3-004 |
| `TG-3-005.bin` | 複数種の制御文字を同一レコードに含む | TG-3-005 |
| `TG-4-001.bin` | 正常なSO/SIペア（SO+4バイトDBCS+SI） | TG-4-001 |
| `TG-4-002.bin` | SOなしでSI登場 | TG-4-002 |
| `TG-4-003.bin` | SIなしでレコード末尾まで未クローズ | TG-4-003 |
| `TG-4-004.bin` | SO直後にSI（空DBCS） | TG-4-004 |
| `TG-4-005.bin` | SOのネスト（SO中にSO） | TG-4-005 |
| `TG-4-006.bin` | 1レコードに複数の正常SO/SIペア | TG-4-006 |
| `TG-5-001.bin` | 80バイト中に 0xFF を含む | TG-5-001 |
| `TG-6-001.bin` | SBCSのみ NL(0x15)区切り | TG-6-001 |
| `TG-6-002.bin` | SBCSのみ CR(0x0D)区切り | TG-6-002 |
| `TG-6-003.bin` | SBCSのみ NL25(0x25)区切り | TG-6-003 |
| `TG-6-004.bin` | 3種の行区切りコード混在 | TG-6-004 |
| `TG-6-005.bin` | DBCS含む NL(0x15)区切り | TG-6-005 |
| `TG-6-006.bin` | SBCS+DBCS混在 NL(0x15)区切り | TG-6-006 |
| `TG-6-007.bin` | 最後の行に行区切りなし（EOFで終端） | TG-6-007 |
| `TG-6-008.bin` | 空ファイル（0バイト） | TG-6-008 |
| `TG-7-001.bin` | 0x00 を含む行（NL区切り） | TG-7-001 |
| `TG-7-002.bin` | 0x0C を含む行（0x0C は不正、0x0D は行区切り） | TG-7-002 |
| `TG-7-003.bin` | 0x0D を含む（行区切りとして処理される） | TG-7-003 |
| `TG-7-004.bin` | 0x10〜0x14 の境界値を含む行 | TG-7-004 |
| `TG-7-005.bin` | 0x15 を含む（行区切りとして処理される） | TG-7-005 |
| `TG-7-006.bin` | 0x16〜0x24 の境界値を含む行 | TG-7-006 |
| `TG-7-007.bin` | 0x25 を含む（行区切りとして処理される） | TG-7-007 |
| `TG-7-008.bin` | 0x26〜0x3F の境界値を含む行 | TG-7-008 |
| `TG-7-009.bin` | 複数種の制御文字を同一行に含む | TG-7-009 |
| `TG-8-001.bin` | 正常なSO/SIペア（1行内） | TG-8-001 |
| `TG-8-002.bin` | SOなしでSI登場 | TG-8-002 |
| `TG-8-003.bin` | SI未クローズで行末 | TG-8-003 |
| `TG-8-004.bin` | SO直後にSI（空DBCS） | TG-8-004 |
| `TG-8-005.bin` | SOのネスト | TG-8-005 |
| `TG-9-001.bin` | 0xFF を含む行 | TG-9-001 |
| `TG-15-001.bin` | SBCSのみ 80バイト×2レコード (IBM-1399) | TG-15-001 |
| `TG-15-002.bin` | DBCS含む 80バイト×2レコード (IBM-1399) | TG-15-002 |
| `TG-15-003.bin` | SBCS+DBCS混在 NL(0x15)区切り (IBM-1399) | TG-15-003 |

---

## 5. テストグループ一覧

| グループID | テストグループ名 | テストケース数 |
|---|---|---|
| TG-1 | CLI引数検証（異常系） | 9 |
| TG-2 | 固定長レコードモード — 正常系 | 7 |
| TG-3 | 固定長レコードモード — 制御文字検知 | 5 |
| TG-4 | 固定長レコードモード — SO/SI不正検知 | 6 |
| TG-5 | 固定長レコードモード — 変換不能文字 | 1 |
| TG-6 | バイトストリームモード — 正常系 | 8 |
| TG-7 | バイトストリームモード — 制御文字検知 | 9 |
| TG-8 | バイトストリームモード — SO/SI不正検知 | 5 |
| TG-9 | バイトストリームモード — 変換不能文字 | 1 |
| TG-10 | 出力ファイルパス・拡張子決定 | 6 |
| TG-11 | ディレクトリ変換（再帰/非再帰） | 3 |
| TG-12 | レポート出力検証 | 6 |
| TG-13 | 致命的エラー処理 | 8 |
| TG-14 | ファイルエラー処理 | 3 |
| TG-15 | IBM-1399 コードページテスト | 4 |
| **合計** | | **81** |

---

## 6. テストケース詳細

各テストケースの表は以下の列を持つ。

| 列 | 内容 |
|---|---|
| テストID | TG-X-NNN |
| テスト名 | テスト内容の概要 |
| 前提条件 | テストデータファイル・オプション等 |
| 実行コマンド例 | 代表的なjava -jarコマンド |
| 確認項目 | 出力ファイル内容・レポート・標準出力・終了コードの期待値 |
| 合否基準 | 判定条件 |

---

### TG-1: CLI引数検証（異常系）

| テストID | テスト名 | 前提条件 | 実行コマンド例 | 確認項目 | 合否基準 |
|---|---|---|---|---|---|
| TG-1-001 | -i オプション未指定 | 出力先ディレクトリを任意に用意 | `java -jar e2uconverter-1.0.0.jar -o output` | stderrに `ERROR: -i option is required.` が出力される。終了コードが 1。 | stderr出力・終了コード1が一致 |
| TG-1-002 | -o オプション未指定 | 任意のファイルを入力に指定 | `java -jar e2uconverter-1.0.0.jar -i TG-2-001.bin` | stderrに `ERROR: -o option is required.` が出力される。終了コードが 1。 | stderr出力・終了コード1が一致 |
| TG-1-003 | -i, -o 両方未指定 | なし | `java -jar e2uconverter-1.0.0.jar` | stderrにエラーメッセージが出力される。終了コードが 1。 | stderr出力・終了コード1 |
| TG-1-004 | -l にゼロを指定 | — | `java -jar e2uconverter-1.0.0.jar -i in -o out -l 0` | stderrに `-l option must be a positive integer.` が出力される。終了コードが 1。 | stderr出力・終了コード1 |
| TG-1-005 | -l に負の値を指定 | — | `java -jar e2uconverter-1.0.0.jar -i in -o out -l -1` | stderrに `-l option must be a positive integer.` が出力される。終了コードが 1。 | stderr出力・終了コード1 |
| TG-1-006 | -l に文字列を指定 | — | `java -jar e2uconverter-1.0.0.jar -i in -o out -l abc` | stderrに `-l option must be a positive integer.` が出力される。終了コードが 1。 | stderr出力・終了コード1 |
| TG-1-007 | -m に不正値を指定 | — | `java -jar e2uconverter-1.0.0.jar -i in -o out -m binary` | stderrに `-m option must be 'fixed' or 'stream'.` が出力される。終了コードが 1。 | stderr出力・終了コード1 |
| TG-1-008 | 不正なコードページを指定 | 任意の入力ファイルを用意 | `java -jar e2uconverter-1.0.0.jar -i TG-2-001.bin -o output -c IBM-9999` | stderrに `ERROR: Unsupported codepage: IBM-9999` が出力される。終了コードが 1。 | stderr出力・終了コード1 |
| TG-1-009 | 存在しない入力パスを指定 | — | `java -jar e2uconverter-1.0.0.jar -i C:\notexist\file.bin -o output` | stderrに入力パス不存在のエラーメッセージが出力される。終了コードが 1。 | stderr出力・終了コード1 |

---

### TG-2: 固定長レコードモード — 正常系（IBM-930, LRECL=80）

| テストID | テスト名 | 前提条件 | 実行コマンド例 | 確認項目 | 合否基準 |
|---|---|---|---|---|---|
| TG-2-001 | SBCSのみ 複数レコード正常変換 | `TG-2-001.bin`（SBCS 80バイト×3レコード） | `java -jar e2uconverter-1.0.0.jar -i TG-2-001.bin -o output -c IBM-930 -m fixed -l 80` | 出力ファイルが3行のUTF-8テキスト。各行末がLF。EBCDIC文字がUnicode文字に正しく変換されている。レポートのステータスがOK。標準出力に `[INFO] OK` が表示される。終了コード0。 | 全確認項目一致 |
| TG-2-002 | DBCS含む 正常変換 | `TG-2-002.bin`（SO+DBCS4バイト+SI含む80バイト×2レコード） | `java -jar e2uconverter-1.0.0.jar -i TG-2-002.bin -o output -c IBM-930 -m fixed -l 80` | 出力ファイルが2行。DBCSコード（漢字等）がUnicode文字に正しく変換されている。SO/SIはUTF-8出力に含まれない。レポートステータスOK。終了コード0。 | 全確認項目一致 |
| TG-2-003 | SBCS+DBCS混在 正常変換 | `TG-2-003.bin`（SBCS文字とDBCS文字が混在した80バイト×3レコード） | `java -jar e2uconverter-1.0.0.jar -i TG-2-003.bin -o output -c IBM-930 -m fixed -l 80` | 出力ファイルが3行。SBCSとDBCS（漢字）が混在したUTF-8テキスト。各行末がLF。レポートステータスOK。終了コード0。 | 全確認項目一致 |
| TG-2-004 | ファイルサイズがLRECLの倍数（最終レコードが80バイト） | `TG-2-004.bin`（80×5=400バイト） | `java -jar e2uconverter-1.0.0.jar -i TG-2-004.bin -o output -c IBM-930 -m fixed -l 80` | 出力ファイルが5行。レポートのステータスOK。終了コード0。 | 全確認項目一致 |
| TG-2-005 | 最終レコードが80バイト未満（短縮レコード） | `TG-2-005.bin`（80+80+45=205バイト）、最終レコード45バイト | `java -jar e2uconverter-1.0.0.jar -i TG-2-005.bin -o output -c IBM-930 -m fixed -l 80` | 出力ファイルが3行。最終行は45文字分（80バイト分ではない）。各行末がLF。レポートステータスOK。終了コード0。 | 全確認項目一致 |
| TG-2-006 | 空ファイル入力 | `TG-2-006.bin`（0バイト） | `java -jar e2uconverter-1.0.0.jar -i TG-2-006.bin -o output -c IBM-930 -m fixed -l 80` | 出力ファイルが0バイト（空）。レポートステータスOK、不正文字件数0。終了コード0。 | 全確認項目一致 |
| TG-2-007 | LRECL=72 で正常変換 | `TG-2-007.bin`（72バイト×3レコード） | `java -jar e2uconverter-1.0.0.jar -i TG-2-007.bin -o output -c IBM-930 -m fixed -l 72` | 出力ファイルが3行。各行が72バイト分のUTF-8文字数。レポートステータスOK。終了コード0。 | 全確認項目一致 |

---

### TG-3: 固定長レコードモード — 制御文字検知（IBM-930, LRECL=80）

| テストID | テスト名 | 前提条件 | 実行コマンド例 | 確認項目 | 合否基準 |
|---|---|---|---|---|---|
| TG-3-001 | 0x00（制御文字の最小値）を検知 | `TG-3-001.bin`（1レコード80バイト、offset=10 に 0x00） | `java -jar e2uconverter-1.0.0.jar -i TG-3-001.bin -o output -c IBM-930 -m fixed -l 80` | 出力ファイルの該当位置が `?` に置換。レポートステータスWARNING。セクション5に（行番号=1, offset=10, EBCDIC=0x00, 理由=制御文字）のエントリ。標準出力に `[WARN] WARNING` が表示される。終了コード0。 | 全確認項目一致 |
| TG-3-002 | 0x0D（CR: 固定長モードでは不正）を検知 | `TG-3-002.bin`（1レコード80バイト、offset=5 に 0x0D） | `java -jar e2uconverter-1.0.0.jar -i TG-3-002.bin -o output -c IBM-930 -m fixed -l 80` | 0x0D が制御文字として検知される（固定長モードでは行区切りとして扱わない）。レポートステータスWARNING。セクション5に（行番号=1, offset=5, EBCDIC=0x0D, 理由=制御文字）のエントリ。終了コード0。 | 全確認項目一致 |
| TG-3-003 | 0x10（制御文字範囲の下端）を検知 | `TG-3-003.bin`（1レコード80バイト、offset=0 に 0x10） | `java -jar e2uconverter-1.0.0.jar -i TG-3-003.bin -o output -c IBM-930 -m fixed -l 80` | レポートステータスWARNING。セクション5に（行番号=1, offset=0, EBCDIC=0x10, 理由=制御文字）のエントリ。終了コード0。 | 全確認項目一致 |
| TG-3-004 | 0x3F（制御文字範囲の上端）を検知 | `TG-3-004.bin`（1レコード80バイト、offset=79 に 0x3F） | `java -jar e2uconverter-1.0.0.jar -i TG-3-004.bin -o output -c IBM-930 -m fixed -l 80` | レポートステータスWARNING。セクション5に（行番号=1, offset=79, EBCDIC=0x3F, 理由=制御文字）のエントリ。終了コード0。 | 全確認項目一致 |
| TG-3-005 | 複数種の制御文字を1レコードに含む | `TG-3-005.bin`（1レコード80バイト、0x01, 0x0D, 0x20, 0x3F を各1個含む） | `java -jar e2uconverter-1.0.0.jar -i TG-3-005.bin -o output -c IBM-930 -m fixed -l 80` | セクション5に4件の不正文字エントリ（それぞれの行番号・offset・EBCDIC値・理由が正確）。レポートの不正文字検知総件数が4。終了コード0。 | 全確認項目一致 |

---

### TG-4: 固定長レコードモード — SO/SI不正検知（IBM-930, LRECL=80）

| テストID | テスト名 | 前提条件 | 実行コマンド例 | 確認項目 | 合否基準 |
|---|---|---|---|---|---|
| TG-4-001 | 正常なSO/SIペア | `TG-4-001.bin`（SBCS... + 0x0E + DBCS4バイト + 0x0F + SBCS... 計80バイト） | `java -jar e2uconverter-1.0.0.jar -i TG-4-001.bin -o output -c IBM-930 -m fixed -l 80` | SO/SI不正として検知されない。レポートステータスOK。不正文字件数0。終了コード0。 | 全確認項目一致 |
| TG-4-002 | SOなしでSI登場 | `TG-4-002.bin`（SIコード 0x0F が先頭付近に含まれ、直前にSOなし） | `java -jar e2uconverter-1.0.0.jar -i TG-4-002.bin -o output -c IBM-930 -m fixed -l 80` | セクション5に（EBCDIC=0x0F, 理由=SO/SI不正）のエントリ。レポートステータスWARNING。終了コード0。 | 全確認項目一致 |
| TG-4-003 | SIなしでレコード末尾まで未クローズ | `TG-4-003.bin`（SOコード 0x0E がレコード中央付近にあり、SIなし） | `java -jar e2uconverter-1.0.0.jar -i TG-4-003.bin -o output -c IBM-930 -m fixed -l 80` | セクション5に（EBCDIC=0x0E, 理由=SO/SI不正）のエントリ（SOの位置のオフセットが記録される）。レポートステータスWARNING。終了コード0。 | 全確認項目一致 |
| TG-4-004 | SO直後にSI（空DBCS） | `TG-4-004.bin`（0x0E の直後に 0x0F が続く） | `java -jar e2uconverter-1.0.0.jar -i TG-4-004.bin -o output -c IBM-930 -m fixed -l 80` | セクション5にSO（0x0E）とSI（0x0F）の2件のエントリ（両方とも理由=SO/SI不正）。レポートステータスWARNING。終了コード0。 | 全確認項目一致 |
| TG-4-005 | SOのネスト（SO中にSO） | `TG-4-005.bin`（0x0E + DBCS2バイト + 0x0E + DBCS2バイト + 0x0F） | `java -jar e2uconverter-1.0.0.jar -i TG-4-005.bin -o output -c IBM-930 -m fixed -l 80` | セクション5に2番目のSO（0x0E）が（理由=SO/SI不正）として記録される。レポートステータスWARNING。終了コード0。 | 全確認項目一致 |
| TG-4-006 | 1レコードに複数の正常SO/SIペア | `TG-4-006.bin`（SBCS + SO+DBCS4バイト+SI + SBCS + SO+DBCS2バイト+SI + SBCS 計80バイト） | `java -jar e2uconverter-1.0.0.jar -i TG-4-006.bin -o output -c IBM-930 -m fixed -l 80` | SO/SI不正として検知されない。レポートステータスOK。不正文字件数0。終了コード0。 | 全確認項目一致 |

---

### TG-5: 固定長レコードモード — 変換不能文字（IBM-930, LRECL=80）

| テストID | テスト名 | 前提条件 | 実行コマンド例 | 確認項目 | 合否基準 |
|---|---|---|---|---|---|
| TG-5-001 | 0xFF（変換不能）を検知 | `TG-5-001.bin`（1レコード80バイト、offset=40 に 0xFF） | `java -jar e2uconverter-1.0.0.jar -i TG-5-001.bin -o output -c IBM-930 -m fixed -l 80` | 出力ファイルの該当位置が `?` に置換。レポートステータスWARNING。セクション5に（行番号=1, offset=40, EBCDIC=0xFF, UTF-8=-, 変換後文字=-, 理由=変換不能）のエントリ。終了コード0。 | 全確認項目一致 |

---

### TG-6: バイトストリームモード — 正常系（IBM-930）

| テストID | テスト名 | 前提条件 | 実行コマンド例 | 確認項目 | 合否基準 |
|---|---|---|---|---|---|
| TG-6-001 | SBCSのみ NL(0x15)区切りで正常変換 | `TG-6-001.bin`（SBCS文字列 + 0x15 を3行分） | `java -jar e2uconverter-1.0.0.jar -i TG-6-001.bin -o output -c IBM-930 -m stream` | 出力ファイルが3行のUTF-8テキスト。各行末がLF。EBCDIC文字がUnicodeに正しく変換。レポートステータスOK。終了コード0。 | 全確認項目一致 |
| TG-6-002 | SBCSのみ CR(0x0D)区切りで正常変換 | `TG-6-002.bin`（SBCS文字列 + 0x0D を3行分） | `java -jar e2uconverter-1.0.0.jar -i TG-6-002.bin -o output -c IBM-930 -m stream` | 出力ファイルが3行。0x0D が行区切りとして処理され、不正文字として検知されない。各行末がLF。レポートステータスOK。終了コード0。 | 全確認項目一致 |
| TG-6-003 | SBCSのみ NL25(0x25)区切りで正常変換 | `TG-6-003.bin`（SBCS文字列 + 0x25 を3行分） | `java -jar e2uconverter-1.0.0.jar -i TG-6-003.bin -o output -c IBM-930 -m stream` | 出力ファイルが3行。0x25 が行区切りとして処理される。各行末がLF。レポートステータスOK。終了コード0。 | 全確認項目一致 |
| TG-6-004 | 3種の行区切りコード混在 | `TG-6-004.bin`（0x0D/0x15/0x25 を各1回使用した3行分） | `java -jar e2uconverter-1.0.0.jar -i TG-6-004.bin -o output -c IBM-930 -m stream` | 出力ファイルが3行。全行区切りコードがLFに変換。レポートステータスOK。終了コード0。 | 全確認項目一致 |
| TG-6-005 | DBCS含む NL(0x15)区切りで正常変換 | `TG-6-005.bin`（SO+DBCS4バイト+SI を含む行 + 0x15 を2行分） | `java -jar e2uconverter-1.0.0.jar -i TG-6-005.bin -o output -c IBM-930 -m stream` | 出力ファイルが2行。DBCSコードが漢字に変換。SO/SIはUTF-8出力に含まれない。レポートステータスOK。終了コード0。 | 全確認項目一致 |
| TG-6-006 | SBCS+DBCS混在 正常変換 | `TG-6-006.bin`（SBCS文字 + SO+DBCS+SI + SBCS文字 + 0x15 を3行分） | `java -jar e2uconverter-1.0.0.jar -i TG-6-006.bin -o output -c IBM-930 -m stream` | 出力ファイルが3行。SBCS/DBCS混在テキスト。各行末がLF。レポートステータスOK。終了コード0。 | 全確認項目一致 |
| TG-6-007 | 最後の行に行区切りなし（EOFで終端） | `TG-6-007.bin`（行1: SBCS+0x15, 行2: SBCSのみ（0x15なし、EOFで終端）） | `java -jar e2uconverter-1.0.0.jar -i TG-6-007.bin -o output -c IBM-930 -m stream` | 出力ファイルが2行。最終行も出力される（行末にLFが付与される）。レポートステータスOK。終了コード0。 | 全確認項目一致 |
| TG-6-008 | 空ファイル入力 | `TG-6-008.bin`（0バイト） | `java -jar e2uconverter-1.0.0.jar -i TG-6-008.bin -o output -c IBM-930 -m stream` | 出力ファイルが0バイト。レポートステータスOK、不正文字件数0。終了コード0。 | 全確認項目一致 |

---

### TG-7: バイトストリームモード — 制御文字検知（IBM-930）

| テストID | テスト名 | 前提条件 | 実行コマンド例 | 確認項目 | 合否基準 |
|---|---|---|---|---|---|
| TG-7-001 | 0x00（制御文字）を検知 | `TG-7-001.bin`（SBCS行 + 0x15, 当該行内のoffset=3 に 0x00） | `java -jar e2uconverter-1.0.0.jar -i TG-7-001.bin -o output -c IBM-930 -m stream` | 0x00 が制御文字として検知。セクション5に（理由=制御文字）エントリ。レポートステータスWARNING。終了コード0。 | 全確認項目一致 |
| TG-7-002 | 0x0C（不正）と 0x0D（行区切り）の境界確認 | `TG-7-002.bin`（行1: SBCS+0x0C+SBCS+0x15, 行2: SBCS+0x0D+SBCS+0x15） | `java -jar e2uconverter-1.0.0.jar -i TG-7-002.bin -o output -c IBM-930 -m stream` | 行1の0x0C は不正文字として検知（理由=制御文字）。行2の0x0D は行区切りとして処理され、不正文字として検知されない。出力ファイルは3行（0x0Dで1行が分割される）。 | 全確認項目一致 |
| TG-7-003 | 0x0D は行区切りとして処理される（不正検知なし） | `TG-7-003.bin`（0x0D のみが入ったデータ） | `java -jar e2uconverter-1.0.0.jar -i TG-7-003.bin -o output -c IBM-930 -m stream` | 不正文字として検知されない。出力ファイルは空行またはLFのみ。レポートステータスOK。終了コード0。 | 全確認項目一致 |
| TG-7-004 | 0x10〜0x14 の境界値を検知 | `TG-7-004.bin`（行1に 0x10, 行2に 0x14 を含む） | `java -jar e2uconverter-1.0.0.jar -i TG-7-004.bin -o output -c IBM-930 -m stream` | 0x10 と 0x14 が制御文字として検知。セクション5に2件のエントリ。レポートステータスWARNING。終了コード0。 | 全確認項目一致 |
| TG-7-005 | 0x15 は行区切りとして処理される（不正検知なし） | `TG-7-005.bin`（0x15 のみを含む） | `java -jar e2uconverter-1.0.0.jar -i TG-7-005.bin -o output -c IBM-930 -m stream` | 不正文字として検知されない。レポートステータスOK。終了コード0。 | 全確認項目一致 |
| TG-7-006 | 0x16〜0x24 の境界値を検知 | `TG-7-006.bin`（行1に 0x16, 行2に 0x24 を含む） | `java -jar e2uconverter-1.0.0.jar -i TG-7-006.bin -o output -c IBM-930 -m stream` | 0x16 と 0x24 が制御文字として検知。セクション5に2件のエントリ。レポートステータスWARNING。終了コード0。 | 全確認項目一致 |
| TG-7-007 | 0x25 は行区切りとして処理される（不正検知なし） | `TG-7-007.bin`（0x25 のみを含む） | `java -jar e2uconverter-1.0.0.jar -i TG-7-007.bin -o output -c IBM-930 -m stream` | 不正文字として検知されない。レポートステータスOK。終了コード0。 | 全確認項目一致 |
| TG-7-008 | 0x26〜0x3F の境界値を検知 | `TG-7-008.bin`（行1に 0x26, 行2に 0x3F を含む） | `java -jar e2uconverter-1.0.0.jar -i TG-7-008.bin -o output -c IBM-930 -m stream` | 0x26 と 0x3F が制御文字として検知。セクション5に2件のエントリ。レポートステータスWARNING。終了コード0。 | 全確認項目一致 |
| TG-7-009 | 複数種の制御文字を同一行に含む | `TG-7-009.bin`（1行内に 0x01, 0x11, 0x17, 0x27 を各1個含む） | `java -jar e2uconverter-1.0.0.jar -i TG-7-009.bin -o output -c IBM-930 -m stream` | セクション5に4件の不正文字エントリ。レポートの不正文字検知総件数が4。終了コード0。 | 全確認項目一致 |

---

### TG-8: バイトストリームモード — SO/SI不正検知（IBM-930）

| テストID | テスト名 | 前提条件 | 実行コマンド例 | 確認項目 | 合否基準 |
|---|---|---|---|---|---|
| TG-8-001 | 正常なSO/SIペア（1行内） | `TG-8-001.bin`（0x0E+DBCS4バイト+0x0F+SBCS+0x15 の行） | `java -jar e2uconverter-1.0.0.jar -i TG-8-001.bin -o output -c IBM-930 -m stream` | SO/SI不正として検知されない。レポートステータスOK。不正文字件数0。終了コード0。 | 全確認項目一致 |
| TG-8-002 | SOなしでSI登場 | `TG-8-002.bin`（行内に SIコード 0x0F のみ存在し、SOなし） | `java -jar e2uconverter-1.0.0.jar -i TG-8-002.bin -o output -c IBM-930 -m stream` | セクション5に（EBCDIC=0x0F, 理由=SO/SI不正）のエントリ。レポートステータスWARNING。終了コード0。 | 全確認項目一致 |
| TG-8-003 | SI未クローズで行末（行をまたぐ場合） | `TG-8-003.bin`（行1: 0x0E+DBCS2バイト+0x15（SIなし）、行2: SBCS+0x15） | `java -jar e2uconverter-1.0.0.jar -i TG-8-003.bin -o output -c IBM-930 -m stream` | 行1のSOが未クローズとして検知（行区切りで行が切れ、SO未クローズ）。セクション5に（行番号=1, EBCDIC=0x0E, 理由=SO/SI不正）のエントリ。レポートステータスWARNING。終了コード0。 | 全確認項目一致 |
| TG-8-004 | SO直後にSI（空DBCS） | `TG-8-004.bin`（行内に 0x0E + 0x0F が連続） | `java -jar e2uconverter-1.0.0.jar -i TG-8-004.bin -o output -c IBM-930 -m stream` | セクション5にSO（0x0E）とSI（0x0F）の2件のエントリ（理由=SO/SI不正）。レポートステータスWARNING。終了コード0。 | 全確認項目一致 |
| TG-8-005 | SOのネスト | `TG-8-005.bin`（行内に 0x0E + DBCS2バイト + 0x0E + DBCS2バイト + 0x0F） | `java -jar e2uconverter-1.0.0.jar -i TG-8-005.bin -o output -c IBM-930 -m stream` | 2番目の0x0Eが（理由=SO/SI不正）として検知。レポートステータスWARNING。終了コード0。 | 全確認項目一致 |

---

### TG-9: バイトストリームモード — 変換不能文字（IBM-930）

| テストID | テスト名 | 前提条件 | 実行コマンド例 | 確認項目 | 合否基準 |
|---|---|---|---|---|---|
| TG-9-001 | 0xFF（変換不能）を検知 | `TG-9-001.bin`（SBCS行 + 0x15, 当該行内のoffset=5 に 0xFF） | `java -jar e2uconverter-1.0.0.jar -i TG-9-001.bin -o output -c IBM-930 -m stream` | 出力ファイルの該当位置が `?` に置換。セクション5に（offset=5, EBCDIC=0xFF, UTF-8=-, 変換後文字=-, 理由=変換不能）のエントリ。レポートステータスWARNING。終了コード0。 | 全確認項目一致 |

---

### TG-10: 出力ファイルパス・拡張子決定（IBM-930）

| テストID | テスト名 | 前提条件 | 実行コマンド例 | 確認項目 | 合否基準 |
|---|---|---|---|---|---|
| TG-10-001 | 拡張子なし + -e未指定 → .txt付与 | 入力ファイル名 `MYPROG`（拡張子なし） | `java -jar e2uconverter-1.0.0.jar -i MYPROG -o output -c IBM-930` | 出力ファイルが `output\MYPROG.txt` として生成される。 | ファイルパスが一致 |
| TG-10-002 | 拡張子ありファイル + -e未指定 → 元の拡張子保持 | 入力ファイル名 `MYPROG.cbl` | `java -jar e2uconverter-1.0.0.jar -i MYPROG.cbl -o output -c IBM-930` | 出力ファイルが `output\MYPROG.cbl` として生成される。 | ファイルパスが一致 |
| TG-10-003 | 拡張子なし + -e指定 → 指定拡張子に | 入力ファイル名 `MYPROG` | `java -jar e2uconverter-1.0.0.jar -i MYPROG -o output -c IBM-930 -e cbl` | 出力ファイルが `output\MYPROG.cbl` として生成される。 | ファイルパスが一致 |
| TG-10-004 | 拡張子ありファイル + -e指定 → 指定拡張子に置換 | 入力ファイル名 `MYPROG.bin` | `java -jar e2uconverter-1.0.0.jar -i MYPROG.bin -o output -c IBM-930 -e txt` | 出力ファイルが `output\MYPROG.txt` として生成される（元の.binは置換される）。 | ファイルパスが一致 |
| TG-10-005 | ディレクトリ指定時のサブディレクトリ構造再現 | 入力ディレクトリ構造: `input\src\MYPROG` | `java -jar e2uconverter-1.0.0.jar -i input -o output -c IBM-930` | 出力ファイルが `output\src\MYPROG.txt` として生成される。入力ディレクトリ構造が再現される。 | ファイルパス・ディレクトリ構造が一致 |
| TG-10-006 | 出力先ディレクトリが存在しない場合は自動作成 | 出力先ディレクトリが存在しないパスを指定 | `java -jar e2uconverter-1.0.0.jar -i TG-2-001.bin -o output\newdir\subdir -c IBM-930` | 出力先ディレクトリが自動作成され、変換ファイルが出力される。終了コード0。 | ディレクトリ作成・ファイル出力が確認できる |

---

### TG-11: ディレクトリ変換（再帰/非再帰）

| テストID | テスト名 | 前提条件 | 実行コマンド例 | 確認項目 | 合否基準 |
|---|---|---|---|---|---|
| TG-11-001 | ディレクトリ指定・-rなし → 直下ファイルのみ変換 | `input\` 直下に2ファイル、`input\sub\` 配下に1ファイルを用意 | `java -jar e2uconverter-1.0.0.jar -i input -o output -c IBM-930` | `output\` 直下に2ファイルのみ変換出力。`output\sub\` 配下は生成されない。レポートのファイル総数が2。 | 全確認項目一致 |
| TG-11-002 | ディレクトリ指定・-rあり → サブディレクトリも再帰変換 | `input\` 直下に2ファイル、`input\sub\` 配下に1ファイルを用意 | `java -jar e2uconverter-1.0.0.jar -i input -o output -c IBM-930 -r` | `output\` 直下に2ファイル、`output\sub\` 配下に1ファイルが変換出力。レポートのファイル総数が3。 | 全確認項目一致 |
| TG-11-003 | レポートのサマリーに再帰処理の有効/無効が反映される | TG-11-001 および TG-11-002 の実行結果 | — | TG-11-001のレポートセクション1に「再帰処理: 無効」、TG-11-002のレポートに「再帰処理: 有効」と記載される。 | レポート内容が一致 |

---

### TG-12: レポート出力検証（IBM-930）

| テストID | テスト名 | 前提条件 | 実行コマンド例 | 確認項目 | 合否基準 |
|---|---|---|---|---|---|
| TG-12-001 | -R未指定 → デフォルトパスに出力 | `TG-2-001.bin` を変換 | `java -jar e2uconverter-1.0.0.jar -i TG-2-001.bin -o output -c IBM-930` | `output\report.md` が生成される。ファイルが有効なMarkdown形式でUTF-8エンコード。 | ファイルの存在・エンコードが確認できる |
| TG-12-002 | -R に相対パス指定 | `TG-2-001.bin` を変換 | `java -jar e2uconverter-1.0.0.jar -i TG-2-001.bin -o output -c IBM-930 -R reports\my_report.md` | `reports\my_report.md` が生成される（存在しないディレクトリは自動作成）。 | ファイルの存在・パスが一致 |
| TG-12-003 | レポートセクション1（実行サマリー）の内容確認 | `TG-2-003.bin` を変換 | `java -jar e2uconverter-1.0.0.jar -i TG-2-003.bin -o output -c IBM-930 -m fixed -l 80` | レポートセクション1に処理日時・処理対象パス・出力先・使用コードページ（IBM-930）・入力モード（fixed LRECL=80）・再帰処理（無効）が正確に記載される。 | セクション1の各項目が仕様通り |
| TG-12-004 | レポートセクション2（処理統計）の内容確認 | `TG-3-001.bin`（WARNING）と `TG-2-001.bin`（OK）を同一ディレクトリに配置して一括変換 | `java -jar e2uconverter-1.0.0.jar -i inputdir -o output -c IBM-930 -m fixed -l 80` | レポートセクション2の「処理ファイル総数=2」「変換成功ファイル数=1」「不正文字検知ファイル数=1」「不正文字検知総件数=1（または以上）」「エラーファイル数=0」が正確に記載される。 | セクション2の数値が一致 |
| TG-12-005 | レポートセクション5（不正文字検知詳細）の内容確認 | `TG-3-001.bin`（offset=10 に 0x00 を含む） | `java -jar e2uconverter-1.0.0.jar -i TG-3-001.bin -o output -c IBM-930 -m fixed -l 80` | セクション5に（ファイル名, 行番号=1, オフセット=10, EBCDIC=0x00, 理由=制御文字）のエントリが正確に記載される。 | セクション5のエントリ内容が一致 |
| TG-12-006 | レポートセクション6（SBCSコード変換対応表）の内容確認 | `TG-2-001.bin` を変換 | `java -jar e2uconverter-1.0.0.jar -i TG-2-001.bin -o output -c IBM-930 -m fixed -l 80` | セクション6に256エントリ（0x00〜0xFF）が存在する。`コードページ: IBM-930` が記載される。0x41 の行に IBM-930 での変換結果が記載される。 | セクション6のエントリ数・コードページ名が一致 |

---

### TG-13: 致命的エラー処理

| テストID | テスト名 | 前提条件 | 実行コマンド例 | 確認項目 | 合否基準 |
|---|---|---|---|---|---|
| TG-13-001 | -i 未指定（FE-01） | — | `java -jar e2uconverter-1.0.0.jar -o output` | stderrに `-i option is required.` が含まれる。終了コード1。標準出力にはメッセージなし。 | stderr・終了コード1 |
| TG-13-002 | -o 未指定（FE-02） | 任意のファイルを用意 | `java -jar e2uconverter-1.0.0.jar -i TG-2-001.bin` | stderrに `-o option is required.` が含まれる。終了コード1。 | stderr・終了コード1 |
| TG-13-003 | 不正な引数形式（FE-03） | — | `java -jar e2uconverter-1.0.0.jar -i -o output` | stderrにエラーメッセージが出力される。終了コード1。 | stderr・終了コード1 |
| TG-13-004 | -l に数値以外を指定（FE-04） | — | `java -jar e2uconverter-1.0.0.jar -i in -o out -l xyz` | stderrに `-l option must be a positive integer.` が出力される。終了コード1。 | stderr・終了コード1 |
| TG-13-005 | -m に不正値を指定（FE-05） | — | `java -jar e2uconverter-1.0.0.jar -i in -o out -m FIXED` | stderrに `-m option must be 'fixed' or 'stream'.` が出力される。終了コード1。（大文字小文字を区別することを確認） | stderr・終了コード1 |
| TG-13-006 | 未サポートコードページ（FE-06） | 任意の入力ファイルを用意 | `java -jar e2uconverter-1.0.0.jar -i TG-2-001.bin -o output -c IBM-XXXX` | stderrに `Unsupported codepage: IBM-XXXX` が含まれる。終了コード1。 | stderr・終了コード1 |
| TG-13-007 | 存在しない入力ファイルパス（FE-07） | 存在しないパスを指定 | `java -jar e2uconverter-1.0.0.jar -i C:\nonexistent\file.bin -o output` | stderrに入力パス不存在のエラーメッセージが出力される。終了コード1。 | stderr・終了コード1 |
| TG-13-008 | 存在しない入力ディレクトリパス（FE-07） | 存在しないディレクトリを指定 | `java -jar e2uconverter-1.0.0.jar -i C:\nonexistent\dir -o output` | stderrに入力パス不存在のエラーメッセージが出力される。終了コード1。 | stderr・終了コード1 |

---

### TG-14: ファイルエラー処理

> **テスト前のセットアップ（再テスト時も必要）**
> TG-14-001 / TG-14-003 は読み取り権限なしファイルを使用する。Git はファイルの NTFS パーミッションを保存しないため、クローン後または権限リセット後に以下のコマンドで権限を剥奪してからテストを実施すること。
>
> ```powershell
> # TG-14-001 用（NOACCESS.bin の読み取りを拒否）
> icacls "04_test\output\TG-14-001\inputdir\NOACCESS.bin" /deny "$env:USERNAME:(R)"
> # TG-14-003 用（NOACCESS.bin の読み取りを拒否）
> icacls "04_test\output\TG-14-003\errordir\NOACCESS.bin" /deny "$env:USERNAME:(R)"
> ```
>
> テスト完了後に権限を元に戻す場合は `/reset` を使用する。
>
> ```powershell
> icacls "04_test\output\TG-14-001\inputdir\NOACCESS.bin" /reset
> icacls "04_test\output\TG-14-003\errordir\NOACCESS.bin" /reset
> ```

| テストID | テスト名 | 前提条件 | 実行コマンド例 | 確認項目 | 合否基準 |
|---|---|---|---|---|---|
| TG-14-001 | 読み取り権限なしファイルが含まれる場合は継続処理 | 入力ディレクトリに正常ファイル（`TG-2-001.bin`）と読み取り権限なしファイルを配置（上記セットアップ参照） | `java -jar e2uconverter-1.0.0.jar -i inputdir -o output -c IBM-930` | 権限なしファイルはERRORとしてレポートに記録される。正常ファイルは変換完了（OK）。処理が継続されレポートが出力される。終了コード0。標準出力に `[ERROR] ERROR` と `[INFO] OK` の両方が表示される。 | 全確認項目一致 |
| TG-14-002 | エラーファイルがある場合のレポートセクション4 | TG-14-001 の実行結果 | — | レポートのセクション4（エラーファイル一覧）にエラーファイルのパスとエラー内容が記載される。エラーがない場合の `（なし）` 表示が確認できる（別途OK-onlyの変換を実行して確認）。 | セクション4の内容が仕様通り |
| TG-14-003 | 全ファイルがエラーでも終了コードが0 | 読み取り権限なしファイルのみを含むディレクトリを入力に指定（上記セットアップ参照） | `java -jar e2uconverter-1.0.0.jar -i errordir -o output -c IBM-930` | 全ファイルがERRORとしてレポートに記録される。レポートファイルは生成される（セクション2のエラーファイル数が正確）。終了コード0（ファイルエラーは致命的エラーではない）。 | 全確認項目一致 |

---

### TG-15: IBM-1399 コードページテスト

| テストID | テスト名 | 前提条件 | 実行コマンド例 | 確認項目 | 合否基準 |
|---|---|---|---|---|---|
| TG-15-001 | IBM-1399 固定長モード SBCS正常変換 | `TG-15-001.bin`（IBM-1399 SBCSコードを使用した80バイト×2レコード。0x42=`｡`, 0x81=`a` 等を含む） | `java -jar e2uconverter-1.0.0.jar -i TG-15-001.bin -o output -c IBM-1399 -m fixed -l 80` | IBM-1399のコード体系でUnicode変換される（0x42 → `｡`（U+FF61）、0x81 → `a`）。IBM-930の変換結果とは異なる。出力ファイルが2行。レポートステータスOK。終了コード0。 | IBM-1399変換結果が正確、IBM-930と異なることを確認 |
| TG-15-002 | IBM-1399 固定長モード DBCS含む正常変換 | `TG-15-002.bin`（IBM-1399 DBCS+SBCSを含む80バイト×2レコード） | `java -jar e2uconverter-1.0.0.jar -i TG-15-002.bin -o output -c IBM-1399 -m fixed -l 80` | DBCS文字が正しくUnicode変換される。SO/SIはUTF-8出力に含まれない。レポートステータスOK。終了コード0。 | 全確認項目一致 |
| TG-15-003 | IBM-1399 バイトストリームモード正常変換 | `TG-15-003.bin`（IBM-1399 SBCS+DBCSを含むNL(0x15)区切りの3行） | `java -jar e2uconverter-1.0.0.jar -i TG-15-003.bin -o output -c IBM-1399 -m stream` | IBM-1399のコード体系で正しく変換される（0x5B → `$`（U+0024）、IBM-930では `¥`）。出力ファイルが3行。レポートステータスOK。終了コード0。 | IBM-1399変換結果が正確 |
| TG-15-004 | IBM-1399 レポートセクション6の確認 | TG-15-001 の実行結果 | — | レポートのセクション6に `コードページ: IBM-1399` が記載される。0x41 の行の変換結果が `-`（変換不能）、0x42 の行が `｡`（U+FF61）として記載される（IBM-930とは異なる対応表）。 | セクション6のコードページ名・変換内容がIBM-1399に対応 |

---

### TG-16: コードページ未定義文字の検知（IBM-930）

| テストID | テスト名 | 前提条件 | 実行コマンド例 | 確認項目 | 合否基準 |
|---|---|---|---|---|---|
| TG-16-001 | IBM-930 固定長モードで未定義バイト（0x57）を検知 | `TG-16-001.bin`（1レコード80バイト、offset=10 に 0x57） | `java -jar e2uconverter-1.0.0.jar -i TG-16-001.bin -o output -c IBM-930 -m fixed -l 80` | レポートステータスWARNING。セクション5に（行番号=1, offset=10, EBCDIC=0x57, UTF-8=-, 変換後文字=-, 理由=未定義文字）のエントリが記録される。終了コード0。 | 全確認項目一致 |
| TG-16-002 | IBM-930 バイトストリームモードで未定義バイト（0x57）を検知 | `TG-16-002.bin`（SBCS行 + 0x15 区切り、当該行内の offset=3 に 0x57） | `java -jar e2uconverter-1.0.0.jar -i TG-16-002.bin -o output -c IBM-930 -m stream` | レポートステータスWARNING。セクション5に（行番号=1, offset=3, EBCDIC=0x57, UTF-8=-, 変換後文字=-, 理由=未定義文字）のエントリが記録される。終了コード0。 | 全確認項目一致 |
| TG-16-003 | IBM-930 で有効な SBCS バイトは未定義文字として検知されない（陰性確認） | `TG-16-003.bin`（1レコード80バイト、offset=10 に IBM-930 で有効な SBCS バイト 0x40 を含む） | `java -jar e2uconverter-1.0.0.jar -i TG-16-003.bin -o output -c IBM-930 -m fixed -l 80` | 0x40 は未定義文字として検知されない。レポートステータスOK。不正文字件数0。終了コード0。 | 全確認項目一致 |

---

## 7. 合格基準

- 全84テストケースについて、確認項目が期待通りであること。
- テストNG（不合格）となったケースは、不具合として記録し、修正後に再テストを実施すること。
- 終了コード、stderr/stdout出力、出力ファイル内容、レポートの全ての確認項目を満たすこと。

---

## 付録A: 実行コマンド共通設定

```bat
rem ワークスペースルートへ移動
cd C:\y\IBMBob_workspace\E2UConverter

rem テストデータの生成（初回のみ）
python 04_test\create_test_data.py

rem JAR パス
set JAR=03_implementation\e2uconverter\target\e2uconverter-1.0.0.jar

rem テスト出力先（テストごとにサブディレクトリを作成する）
set OUT=04_test\output

rem 実行例（TG-2-001）
java -jar %JAR% -i 04_test\data\TG-2-001.bin -o %OUT%\TG-2-001 -c IBM-930 -m fixed -l 80

rem 終了コードの確認
echo 終了コード: %ERRORLEVEL%
```

> **注意**: テストデータは `.bin` 拡張子付きのファイル名のため、`-e` オプション未指定の場合、出力ファイルも `.bin` 拡張子のまま生成される（拡張子ありのルールが適用される）。TG-2 等の確認時は出力ファイル名が `TG-2-001.bin` になることに注意。拡張子変換テスト（TG-10）は拡張子なし・拡張子ありの両パターンで個別に確認する。

---

## 付録B: 不正文字検知範囲 早見表

### 固定長モード（`-m fixed`）での検知対象

| コード範囲 | 検知 | 備考 |
|---|---|---|
| `0x00`〜`0x0D` | ✓ 制御文字 | 0x0D（CR）も固定長では不正 |
| `0x0E` | SO/SI検証 | DBCS開始コード |
| `0x0F` | SO/SI検証 | DBCS終了コード |
| `0x10`〜`0x3F` | ✓ 制御文字 | |
| `0x40`〜`0xFE` | 正常範囲 | 通常EBCDIC文字 |
| `0xFF` | ✓ 変換不能 | |

### バイトストリームモード（`-m stream`）での検知対象

| コード範囲 | 検知 | 備考 |
|---|---|---|
| `0x00`〜`0x0C` | ✓ 制御文字 | |
| `0x0D` | 行区切り | CR（不正検知対象外） |
| `0x0E` | SO/SI検証 | |
| `0x0F` | SO/SI検証 | |
| `0x10`〜`0x14` | ✓ 制御文字 | |
| `0x15` | 行区切り | NL（不正検知対象外） |
| `0x16`〜`0x24` | ✓ 制御文字 | |
| `0x25` | 行区切り | NL25（不正検知対象外） |
| `0x26`〜`0x3F` | ✓ 制御文字 | |
| `0x40`〜`0xFE` | 正常範囲 | |
| `0xFF` | ✓ 変換不能 | |
