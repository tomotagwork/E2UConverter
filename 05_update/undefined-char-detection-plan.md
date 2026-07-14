# 未定義文字検知追加 プラン

## 概要

コードページ上で未定義のバイト値（変換結果が空文字または `?` になるバイト）を、
「5. 不正文字検知詳細」にレポートする機能を追加する。

検知理由は「未定義文字」とする（既存の「変換不能」とは区別する）。
出力ファイルの変換処理は変更しない（Java Charset の REPLACE 動作に任せる）。

---

## サブタスク一覧

---

### サブタスク 1: 要件定義の更新

**Intent**
`01_requirements/requirements.md` のセクション6（不正文字検知仕様）に、
コードページ上の未定義文字を検知対象とする旨を追記する。

**Expected Outcomes**
- 固定長モード・バイトストリームモードの双方の「制御文字・変換不能コードの検知」テーブル直後に、
  未定義文字の検知に関する記述が追加される。
- セクション9（エラー処理仕様）の「変換不能文字を検知した場合」の行に、未定義文字の扱いが追記される。

**Todo List**
1. `01_requirements/requirements.md` を開く。
2. セクション 6-2（固定長レコードモード）の制御文字テーブルの直後に以下を追記する。
   「指定コードページで変換できないバイト値（0x40〜0xFE の範囲で未定義のコード）が含まれる場合も、
   不正文字として検知し理由を「未定義文字」としてレポートする。出力ファイルは Java Charset の
   REPLACE 設定により変換される。」
3. セクション 6-3（バイトストリームモード）の制御文字テーブルの直後にも同様の記述を追記する。
4. セクション 9 のエラー処理テーブルに「コードページ上の未定義文字を検知した場合」の行を追加する。

**Relevant Context**
- `01_requirements/requirements.md` 97〜129行目（制御文字・変換不能コードの検知テーブル）
- `01_requirements/requirements.md` 244〜251行目（エラー処理仕様テーブル）

**Status**: [ ] pending

---

### サブタスク 2: InvalidCharDetector の実装変更

**Intent**
`InvalidCharDetector.detect()` メソッドに、`0x40`〜`0xFE` の範囲のバイトについて
`convertSingleByte()` を呼び出し、変換結果が `"-"` であれば「未定義文字」として
`InvalidCharEntry` を追加するロジックを追加する。

`isControlChar()` は範囲ベースの判定のため変更しない。
`detect()` の制御文字ループ後に、新しいループを追加する形で実装する。

**Expected Outcomes**
- IBM-930 で `0x41` のようなコードページ未定義バイトが含まれるレコードを `detect()` に渡すと、
  理由「未定義文字」の `InvalidCharEntry` が返る。
- 既存の制御文字（`0x00`〜`0x3F`）・`0xFF`・SO/SI 不正の検知動作は変更されない。
- `convertSingleByte()` のメソッド自体は変更しない（再利用する）。

**Todo List**
1. `InvalidCharDetector.java` を開く。
2. `detect()` メソッドの制御文字ループ（①）と SO/SI 検証（②）の間に、
   以下の処理を追加する。
   - `0x40`〜`0xFE` の範囲のバイトに対して `convertSingleByte()` を呼び出す。
   - 戻り値の `[0]`（utf8Hex）が `"-"` の場合、「未定義文字」の `InvalidCharEntry` を追加する。
3. 追加した理由文字列の定数またはリテラル「未定義文字」が `FileConverter.java` の
   置換ロジックと整合していることを確認する（今回は置換しないため、追加不要）。

**Relevant Context**
- `InvalidCharDetector.java` 41〜62行目（`detect()` メソッド）
- `InvalidCharDetector.java` 146〜172行目（`convertSingleByte()` メソッド）
- `InvalidCharDetector.java` 44〜56行目（制御文字ループ ①）
- `FileConverter.java` 189〜210行目（`replaceInvalidSoSiBytes()` — 今回変更不要だが参照確認）

**Status**: [ ] pending

---

### サブタスク 3: テスト計画の更新

**Intent**
`04_test/test_plan.md` に「コードページ未定義文字の検知」テストグループを追加する。
IBM-930 で `0x41`（未定義）を含むテストケースを固定長・バイトストリームの両モードで追加する。

**Expected Outcomes**
- 新しいテストグループ（例: TG-16）として、以下のテストケースが追加される。
  - 固定長モードで `0x41`（IBM-930 で未定義）を含むファイルを変換すると、
    レポートセクション5に（理由=未定義文字）のエントリが記録される。
  - バイトストリームモードで同様に検知される。
  - 制御文字範囲外かつ未定義でないバイト（例: IBM-930 の `0x42` など有効なバイト）は
    「未定義文字」として検知されない（陰性確認）。

**Todo List**
1. `04_test/test_plan.md` を開く。
2. 既存の最終テストグループ（TG-15）の後に TG-16 を追加する。
3. 以下のテストケースを記載する。
   - TG-16-001: IBM-930 固定長モードで `0x41`（未定義）を offset=10 に含む 80バイトレコード。
     セクション5に（行番号=1, offset=10, EBCDIC=0x41, UTF-8=-, 変換後文字=-, 理由=未定義文字）のエントリが記録されること。
   - TG-16-002: IBM-930 バイトストリームモードで `0x41`（未定義）を含む行。
     同様に「未定義文字」として検知されること。
   - TG-16-003: IBM-930 で有効な SBCS バイト（例: `0x42` = スペース など）は
     「未定義文字」として検知されないこと（陰性確認）。

**Relevant Context**
- `04_test/test_plan.md` の TG-15 セクション（357〜366行目）
- `92_testoutput/report.md` セクション6（IBM-930 で `0x41` が `-` となっていることを確認済み）

**Status**: [ ] pending

---

### サブタスク 4: JAR のリビルド

**Intent**
実装変更を反映した JAR ファイルを生成する。

**Expected Outcomes**
- `03_implementation/e2uconverter/target/e2uconverter-1.0.0-shaded.jar` が最新の実装で更新される。

**Todo List**
1. `03_implementation/e2uconverter/` ディレクトリで `mvn package` を実行する。
2. ビルドが SUCCESS であることを確認する。

**Relevant Context**
- `03_implementation/e2uconverter/pom.xml`

**Status**: [ ] pending
