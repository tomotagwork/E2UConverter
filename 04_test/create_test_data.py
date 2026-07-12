#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
E2UConverter テストデータ生成スクリプト
04_test/data/ 配下にテストケースID対応のEBCDICバイナリファイルを生成する。

使用方法:
    python create_test_data.py

出力先:
    04_test/data/TG-X-NNN.bin

コードページ: IBM-930 (TG-1〜TG-14), IBM-1399 (TG-15)

IBM-930 主要SBCSコード対応（バイト値）:
  0x40 = スペース
  0xC1-0xC9 = A-I
  0xD1-0xD9 = J-R
  0xE2-0xE9 = S-Z
  0xF0-0xF9 = 0-9
  0x41-0x49 = ｡｢｣､･ｦｧｨｩ (半角カナ)
  0x51-0x59 = ｪｫｬｭｮｯｰｱｲ
  0x81-0x89 = ｱｲｳｴｵｶｷｸｹ
  0x5B = ¥（円記号）
  0x5C = *
  0x60 = -
  0x61 = /
  0x80 = ]
  0x0E = SO (DBCS開始)
  0x0F = SI (DBCS終了)

IBM-930 DBCS コード（0x0E..0x0F 内の2バイトペア例）:
  日: 0x45 0x62
  本: 0x45 0x66
  語: 0x48 0xE7
  テ: 0x43 0x94
  ス: 0x43 0x8E
  ト: 0x43 0x95

IBM-1399 主要SBCSコード対応:
  0x40 = スペース
  0x41 = 変換不能 (IBM-930では｡)
  0x42 = ｡ (IBM-930では｢)
  0x5B = $ (IBM-930では¥)
  0x81 = a (IBM-930ではｱ)
  0xC1-0xC9 = A-I (IBM-930と同じ)
  0xF0-0xF9 = 0-9 (IBM-930と同じ)
"""

import os
import struct

# 出力ディレクトリ
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
OUTPUT_DIR = os.path.join(SCRIPT_DIR, "data")


def write_bin(filename: str, data: bytes) -> None:
    """指定ファイルにバイナリデータを書き込む。"""
    path = os.path.join(OUTPUT_DIR, filename)
    with open(path, "wb") as f:
        f.write(data)
    print(f"  生成: {filename} ({len(data)} bytes)")


def make_fixed_record(content: bytes, lrecl: int = 80) -> bytes:
    """指定バイト列を LRECL バイトに整形する（スペース 0x40 でパディング）。"""
    if len(content) >= lrecl:
        return content[:lrecl]
    return content + bytes([0x40] * (lrecl - len(content)))


# -----------------------------------------------------------------------
# IBM-930 SBCS 文字列ヘルパー
# -----------------------------------------------------------------------

# "ABC 123" in IBM-930
ABC_123 = bytes([0xC1, 0xC2, 0xC3, 0x40, 0xF1, 0xF2, 0xF3])

# "HELLO WORLD" in IBM-930
HELLO_WORLD = bytes([
    0xC8, 0xC5, 0xD3, 0xD3, 0xD6, 0x40,
    0xE6, 0xD6, 0xD9, 0xD3, 0xC4
])

# "LINE1" "LINE2" "LINE3" in IBM-930
LINE1 = bytes([0xD3, 0xC9, 0xD5, 0xC5, 0xF1])  # LINE1
LINE2 = bytes([0xD3, 0xC9, 0xD5, 0xC5, 0xF2])  # LINE2
LINE3 = bytes([0xD3, 0xC9, 0xD5, 0xC5, 0xF3])  # LINE3

# IBM-930 DBCS: 日本語テスト (0x0E 45 62 45 66 48 E7 43 94 43 8E 43 95 0F)
DBCS_NIHONGO = bytes([
    0x0E,
    0x45, 0x62,  # 日
    0x45, 0x66,  # 本
    0x48, 0xE7,  # 語
    0x0F
])
DBCS_TEST = bytes([
    0x0E,
    0x43, 0x94,  # テ
    0x43, 0x8E,  # ス
    0x43, 0x95,  # ト
    0x0F
])

# IBM-1399 固有 SBCS
IBM1399_SBCS_SAMPLE = bytes([
    0xC1, 0xC2, 0xC3, 0x40,       # A B C スペース (IBM-930と同じ)
    0x42, 0x43, 0x44, 0x40,       # ｡ ｢ ｣ スペース (IBM-1399固有の半角カナ位置)
    0x5B, 0x5C, 0x40,             # $ * スペース (IBM-1399固有; IBM-930では¥)
    0xF1, 0xF2, 0xF3              # 1 2 3
])

# IBM-1399 DBCS: 日本語テスト (0x0E 45 62 45 66 48 E7 43 94 43 8E FE FE 0F)
DBCS_NIHONGO_1399 = bytes([
    0x0E,
    0x45, 0x62,  # 日
    0x45, 0x66,  # 本
    0x48, 0xE7,  # 語
    0x0F
])


def main():
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    print(f"テストデータ出力先: {OUTPUT_DIR}")
    print()

    # -----------------------------------------------------------------------
    # TG-2: 固定長レコードモード — 正常系
    # -----------------------------------------------------------------------
    print("[TG-2] 固定長レコードモード 正常系")

    # TG-2-001: SBCSのみ 80バイト×3レコード
    rec1 = make_fixed_record(LINE1 + bytes([0x40]) + ABC_123)
    rec2 = make_fixed_record(LINE2 + bytes([0x40]) + HELLO_WORLD)
    rec3 = make_fixed_record(LINE3 + bytes([0x40]) + ABC_123)
    write_bin("TG-2-001.bin", rec1 + rec2 + rec3)

    # TG-2-002: DBCS含む 80バイト×2レコード
    rec1 = make_fixed_record(LINE1 + bytes([0x40]) + DBCS_NIHONGO)
    rec2 = make_fixed_record(LINE2 + bytes([0x40]) + DBCS_TEST)
    write_bin("TG-2-002.bin", rec1 + rec2)

    # TG-2-003: SBCS+DBCS混在 80バイト×3レコード
    rec1 = make_fixed_record(LINE1 + bytes([0x40]) + DBCS_NIHONGO + bytes([0x40]) + ABC_123)
    rec2 = make_fixed_record(LINE2 + bytes([0x40]) + DBCS_TEST + bytes([0x40]) + HELLO_WORLD)
    rec3 = make_fixed_record(LINE3 + bytes([0x40]) + ABC_123 + DBCS_NIHONGO)
    write_bin("TG-2-003.bin", rec1 + rec2 + rec3)

    # TG-2-004: ファイルサイズがLRECLの倍数（80×5=400バイト）
    data = b""
    for i in range(5):
        rec = make_fixed_record(bytes([0xD3, 0xC9, 0xD5, 0xC5, 0xF0 + i]) + bytes([0x40]) + ABC_123)
        data += rec
    write_bin("TG-2-004.bin", data)

    # TG-2-005: 最終レコードが80バイト未満（80+80+45=205バイト）
    rec1 = make_fixed_record(LINE1 + bytes([0x40]) + ABC_123)
    rec2 = make_fixed_record(LINE2 + bytes([0x40]) + HELLO_WORLD)
    # 最終レコード: 45バイト（パディングなし）
    rec3_content = LINE3 + bytes([0x40]) + ABC_123
    rec3_short = rec3_content[:45] if len(rec3_content) >= 45 else rec3_content + bytes([0x40] * (45 - len(rec3_content)))
    write_bin("TG-2-005.bin", rec1 + rec2 + rec3_short)

    # TG-2-006: 空ファイル
    write_bin("TG-2-006.bin", b"")

    # TG-2-007: LRECL=72バイト×3レコード
    rec1 = make_fixed_record(LINE1 + bytes([0x40]) + ABC_123, lrecl=72)
    rec2 = make_fixed_record(LINE2 + bytes([0x40]) + HELLO_WORLD, lrecl=72)
    rec3 = make_fixed_record(LINE3 + bytes([0x40]) + ABC_123, lrecl=72)
    write_bin("TG-2-007.bin", rec1 + rec2 + rec3)

    # -----------------------------------------------------------------------
    # TG-3: 固定長レコードモード — 制御文字検知
    # -----------------------------------------------------------------------
    print("[TG-3] 固定長レコードモード 制御文字検知")

    # TG-3-001: offset=10 に 0x00
    rec = bytearray(make_fixed_record(LINE1 + bytes([0x40]) + ABC_123))
    rec[10] = 0x00
    write_bin("TG-3-001.bin", bytes(rec))

    # TG-3-002: offset=5 に 0x0D（CR: 固定長モードでは不正）
    rec = bytearray(make_fixed_record(LINE1 + bytes([0x40]) + ABC_123))
    rec[5] = 0x0D
    write_bin("TG-3-002.bin", bytes(rec))

    # TG-3-003: offset=0 に 0x10（制御文字範囲下端）
    rec = bytearray(make_fixed_record(LINE1 + bytes([0x40]) + ABC_123))
    rec[0] = 0x10
    write_bin("TG-3-003.bin", bytes(rec))

    # TG-3-004: offset=79 に 0x3F（制御文字範囲上端）
    rec = bytearray(make_fixed_record(LINE1 + bytes([0x40]) + ABC_123))
    rec[79] = 0x3F
    write_bin("TG-3-004.bin", bytes(rec))

    # TG-3-005: 複数種の制御文字（0x01, 0x0D, 0x20, 0x3F）を各1個含む
    rec = bytearray(make_fixed_record(LINE1 + bytes([0x40]) + ABC_123))
    rec[1]  = 0x01
    rec[15] = 0x0D
    rec[30] = 0x20
    rec[50] = 0x3F
    write_bin("TG-3-005.bin", bytes(rec))

    # -----------------------------------------------------------------------
    # TG-4: 固定長レコードモード — SO/SI不正検知
    # -----------------------------------------------------------------------
    print("[TG-4] 固定長レコードモード SO/SI不正検知")

    # TG-4-001: 正常なSO/SIペア（SO+DBCS4バイト+SI）
    content = LINE1 + bytes([0x40]) + DBCS_NIHONGO + bytes([0x40]) + ABC_123
    write_bin("TG-4-001.bin", make_fixed_record(content))

    # TG-4-002: SOなしでSI登場（offset=10 に 0x0F のみ）
    rec = bytearray(make_fixed_record(LINE1 + bytes([0x40]) * 20 + ABC_123))
    rec[10] = 0x0F  # SI のみ（SOなし）
    write_bin("TG-4-002.bin", bytes(rec))

    # TG-4-003: SIなし（SOがレコード末尾まで未クローズ）
    # offset=20 に SO を置き、SI は置かない
    rec = bytearray(make_fixed_record(LINE1 + bytes([0x40]) * 15))
    rec[20] = 0x0E  # SO のみ、SI なし
    # 以降のバイトをSBCS文字で埋める（DBCSっぽいバイトだが SI がない）
    for i in range(21, 30):
        rec[i] = 0x45  # 不明バイト（DBCSのつもりだがSIなし）
    write_bin("TG-4-003.bin", bytes(rec))

    # TG-4-004: SO直後にSI（空DBCS）
    rec = bytearray(make_fixed_record(LINE1 + bytes([0x40]) * 15))
    rec[10] = 0x0E  # SO
    rec[11] = 0x0F  # SI（直後）
    write_bin("TG-4-004.bin", bytes(rec))

    # TG-4-005: SOのネスト（SO中にSO）
    rec = bytearray(make_fixed_record(LINE1 + bytes([0x40]) * 20))
    rec[10] = 0x0E  # SO（1つ目）
    rec[11] = 0x45  # DBCS 1バイト目
    rec[12] = 0x62  # DBCS 2バイト目（日）
    rec[13] = 0x0E  # SO（2つ目: ネスト → 不正）
    rec[14] = 0x45
    rec[15] = 0x66
    rec[16] = 0x0F  # SI
    write_bin("TG-4-005.bin", bytes(rec))

    # TG-4-006: 1レコードに複数の正常SO/SIペア
    content = (
        LINE1 + bytes([0x40]) +
        DBCS_NIHONGO +          # SO+4バイト+SI
        bytes([0x40]) +
        DBCS_TEST +             # SO+6バイト+SI
        bytes([0x40]) +
        ABC_123
    )
    write_bin("TG-4-006.bin", make_fixed_record(content))

    # -----------------------------------------------------------------------
    # TG-5: 固定長レコードモード — 変換不能文字
    # -----------------------------------------------------------------------
    print("[TG-5] 固定長レコードモード 変換不能文字")

    # TG-5-001: offset=40 に 0xFF
    rec = bytearray(make_fixed_record(LINE1 + bytes([0x40]) + ABC_123))
    rec[40] = 0xFF
    write_bin("TG-5-001.bin", bytes(rec))

    # -----------------------------------------------------------------------
    # TG-6: バイトストリームモード — 正常系
    # -----------------------------------------------------------------------
    print("[TG-6] バイトストリームモード 正常系")
    NL   = 0x15  # EBCDIC NL
    CR   = 0x0D  # EBCDIC CR
    NL25 = 0x25  # EBCDIC NL25

    # TG-6-001: SBCSのみ NL(0x15)区切り 3行
    line1 = LINE1 + bytes([0x40]) + ABC_123
    line2 = LINE2 + bytes([0x40]) + HELLO_WORLD
    line3 = LINE3 + bytes([0x40]) + ABC_123
    write_bin("TG-6-001.bin",
              line1 + bytes([NL]) + line2 + bytes([NL]) + line3 + bytes([NL]))

    # TG-6-002: SBCSのみ CR(0x0D)区切り 3行
    write_bin("TG-6-002.bin",
              line1 + bytes([CR]) + line2 + bytes([CR]) + line3 + bytes([CR]))

    # TG-6-003: SBCSのみ NL25(0x25)区切り 3行
    write_bin("TG-6-003.bin",
              line1 + bytes([NL25]) + line2 + bytes([NL25]) + line3 + bytes([NL25]))

    # TG-6-004: 3種の行区切りコード混在（行1:CR, 行2:NL, 行3:NL25）
    write_bin("TG-6-004.bin",
              line1 + bytes([CR]) + line2 + bytes([NL]) + line3 + bytes([NL25]))

    # TG-6-005: DBCS含む NL(0x15)区切り 2行
    dbcs_line1 = LINE1 + bytes([0x40]) + DBCS_NIHONGO
    dbcs_line2 = LINE2 + bytes([0x40]) + DBCS_TEST
    write_bin("TG-6-005.bin",
              dbcs_line1 + bytes([NL]) + dbcs_line2 + bytes([NL]))

    # TG-6-006: SBCS+DBCS混在 NL(0x15)区切り 3行
    mixed1 = LINE1 + bytes([0x40]) + DBCS_NIHONGO + bytes([0x40]) + ABC_123
    mixed2 = LINE2 + bytes([0x40]) + DBCS_TEST + bytes([0x40]) + HELLO_WORLD
    mixed3 = LINE3 + bytes([0x40]) + ABC_123 + DBCS_NIHONGO
    write_bin("TG-6-006.bin",
              mixed1 + bytes([NL]) + mixed2 + bytes([NL]) + mixed3 + bytes([NL]))

    # TG-6-007: 最後の行に行区切りなし（EOFで終端）
    write_bin("TG-6-007.bin",
              line1 + bytes([NL]) + line2)  # line2 の後に行区切りなし

    # TG-6-008: 空ファイル
    write_bin("TG-6-008.bin", b"")

    # -----------------------------------------------------------------------
    # TG-7: バイトストリームモード — 制御文字検知
    # -----------------------------------------------------------------------
    print("[TG-7] バイトストリームモード 制御文字検知")

    # TG-7-001: 行内の offset=3 に 0x00
    line_with_ctrl = bytearray(LINE1 + bytes([0x40]) + ABC_123)
    line_with_ctrl[3] = 0x00
    write_bin("TG-7-001.bin", bytes(line_with_ctrl) + bytes([NL]))

    # TG-7-002: 行1に0x0C（不正）、行2に0x0D（行区切りとして別処理）
    line_0x0C = bytearray(LINE1 + bytes([0x40]) + ABC_123)
    line_0x0C[5] = 0x0C  # 不正文字
    # 行2: SBCSの中に0x0Dを置く（0x0Dで行が分割される）
    line_0x0D = LINE2 + bytes([0x40]) + ABC_123
    # 0x0D は行区切り → 行2が2行に分かれる
    write_bin("TG-7-002.bin",
              bytes(line_0x0C) + bytes([NL]) + line_0x0D[:5] + bytes([CR]) + line_0x0D[5:] + bytes([NL]))

    # TG-7-003: 0x0D のみ（行区切りとして処理、不正検知なし）
    write_bin("TG-7-003.bin", bytes([CR]))

    # TG-7-004: 0x10（下端）と 0x14（上端）を含む行
    line_0x10 = bytearray(LINE1 + bytes([0x40]) + ABC_123)
    line_0x10[2] = 0x10  # 制御文字範囲下端
    line_0x14 = bytearray(LINE2 + bytes([0x40]) + ABC_123)
    line_0x14[2] = 0x14  # 0x10-0x14 範囲上端
    write_bin("TG-7-004.bin",
              bytes(line_0x10) + bytes([NL]) + bytes(line_0x14) + bytes([NL]))

    # TG-7-005: 0x15 のみ（行区切りとして処理、不正検知なし）
    write_bin("TG-7-005.bin", bytes([NL]))

    # TG-7-006: 0x16（下端）と 0x24（上端）を含む行
    line_0x16 = bytearray(LINE1 + bytes([0x40]) + ABC_123)
    line_0x16[4] = 0x16
    line_0x24 = bytearray(LINE2 + bytes([0x40]) + ABC_123)
    line_0x24[4] = 0x24
    write_bin("TG-7-006.bin",
              bytes(line_0x16) + bytes([NL]) + bytes(line_0x24) + bytes([NL]))

    # TG-7-007: 0x25 のみ（行区切りとして処理、不正検知なし）
    write_bin("TG-7-007.bin", bytes([NL25]))

    # TG-7-008: 0x26（下端）と 0x3F（上端）を含む行
    line_0x26 = bytearray(LINE1 + bytes([0x40]) + ABC_123)
    line_0x26[6] = 0x26
    line_0x3F = bytearray(LINE2 + bytes([0x40]) + ABC_123)
    line_0x3F[6] = 0x3F
    write_bin("TG-7-008.bin",
              bytes(line_0x26) + bytes([NL]) + bytes(line_0x3F) + bytes([NL]))

    # TG-7-009: 1行内に 0x01, 0x11, 0x17, 0x27 を各1個含む
    line_multi = bytearray(LINE1 + bytes([0x40]) * 20)
    line_multi[2]  = 0x01
    line_multi[5]  = 0x11
    line_multi[10] = 0x17
    line_multi[15] = 0x27
    write_bin("TG-7-009.bin", bytes(line_multi) + bytes([NL]))

    # -----------------------------------------------------------------------
    # TG-8: バイトストリームモード — SO/SI不正検知
    # -----------------------------------------------------------------------
    print("[TG-8] バイトストリームモード SO/SI不正検知")

    # TG-8-001: 正常なSO/SIペア（1行内）
    line_ok = LINE1 + bytes([0x40]) + DBCS_NIHONGO + bytes([0x40]) + ABC_123
    write_bin("TG-8-001.bin", line_ok + bytes([NL]))

    # TG-8-002: SOなしでSI登場
    line_si_only = bytearray(LINE1 + bytes([0x40]) + ABC_123)
    line_si_only[8] = 0x0F  # SI のみ（SOなし）
    write_bin("TG-8-002.bin", bytes(line_si_only) + bytes([NL]))

    # TG-8-003: SI未クローズで行末（行1: SO+DBCS2バイト+NL で終端）
    line_unclosed = bytes([
        0xD3, 0xC9,  # LI
        0x0E,        # SO
        0x45, 0x62,  # 日
    ])  # SI なし、NL で行終端
    line_normal = LINE2 + bytes([0x40]) + ABC_123
    write_bin("TG-8-003.bin",
              line_unclosed + bytes([NL]) + line_normal + bytes([NL]))

    # TG-8-004: SO直後にSI（空DBCS）
    line_empty_dbcs = bytearray(LINE1 + bytes([0x40]) + ABC_123)
    line_empty_dbcs[5] = 0x0E  # SO
    line_empty_dbcs[6] = 0x0F  # SI（直後）
    write_bin("TG-8-004.bin", bytes(line_empty_dbcs) + bytes([NL]))

    # TG-8-005: SOのネスト
    line_nested = bytearray(LINE1 + bytes([0x40]) * 15)
    line_nested[5]  = 0x0E   # SO（1つ目）
    line_nested[6]  = 0x45
    line_nested[7]  = 0x62   # 日
    line_nested[8]  = 0x0E   # SO（2つ目: ネスト → 不正）
    line_nested[9]  = 0x45
    line_nested[10] = 0x66   # 本
    line_nested[11] = 0x0F   # SI
    write_bin("TG-8-005.bin", bytes(line_nested) + bytes([NL]))

    # -----------------------------------------------------------------------
    # TG-9: バイトストリームモード — 変換不能文字
    # -----------------------------------------------------------------------
    print("[TG-9] バイトストリームモード 変換不能文字")

    # TG-9-001: offset=5 に 0xFF
    line_ff = bytearray(LINE1 + bytes([0x40]) + ABC_123)
    line_ff[5] = 0xFF
    write_bin("TG-9-001.bin", bytes(line_ff) + bytes([NL]))

    # -----------------------------------------------------------------------
    # TG-15: IBM-1399 コードページテスト
    # -----------------------------------------------------------------------
    print("[TG-15] IBM-1399 コードページテスト")

    # TG-15-001: IBM-1399 SBCS 80バイト×2レコード
    # 0x42=｡, 0x43=｢, 0x5B=$, 0xC1=A, 0xF1=1 など IBM-1399 のコードを使用
    ibm1399_sbcs1 = bytes([
        0xC1, 0xC2, 0xC3, 0x40,  # A B C スペース (IBM-1399でも A B C)
        0x42, 0x43, 0x44, 0x40,  # ｡ ｢ ｣ スペース (IBM-1399の半角カナ; IBM-930では｢ ｣ ､)
        0x5B, 0x5C, 0x40,        # $ * スペース (IBM-1399固有; IBM-930では ¥ *)
        0xF1, 0xF2, 0xF3         # 1 2 3
    ])
    ibm1399_sbcs2 = bytes([
        0xD1, 0xD2, 0xD3, 0x40,  # J K L スペース
        0x81, 0x82, 0x83, 0x40,  # a b c スペース (IBM-1399; IBM-930ではｱ ｲ ｳ)
        0xE2, 0xE3, 0xE4, 0x40,  # S T U スペース
        0xF7, 0xF8, 0xF9         # 7 8 9
    ])
    write_bin("TG-15-001.bin",
              make_fixed_record(ibm1399_sbcs1) + make_fixed_record(ibm1399_sbcs2))

    # TG-15-002: IBM-1399 DBCS+SBCS 80バイト×2レコード
    # IBM-1399のDBCSは IBM-930と同じバイト列を使用（漢字体系は共通部分が多い）
    rec1_1399 = make_fixed_record(ibm1399_sbcs1 + bytes([0x40]) + DBCS_NIHONGO_1399)
    rec2_1399 = make_fixed_record(ibm1399_sbcs2 + bytes([0x40]) + bytes([
        0x0E,
        0x43, 0x94,  # テ
        0x43, 0x8E,  # ス
        0x43, 0x95,  # ト
        0x0F
    ]))
    write_bin("TG-15-002.bin", rec1_1399 + rec2_1399)

    # TG-15-003: IBM-1399 バイトストリームモード NL(0x15)区切り 3行
    line_1399_1 = ibm1399_sbcs1 + bytes([0x40]) + DBCS_NIHONGO_1399
    line_1399_2 = ibm1399_sbcs2 + bytes([0x40]) + bytes([
        0x0E, 0x43, 0x94, 0x43, 0x8E, 0x43, 0x95, 0x0F
    ])
    line_1399_3 = bytes([
        0x5B, 0x40,              # $ スペース (IBM-1399固有; IBM-930では ¥)
        0xC5, 0xD5, 0xC4, 0x40, # E N D スペース
        0xF0, 0xF1, 0xF2         # 0 1 2
    ])
    write_bin("TG-15-003.bin",
              line_1399_1 + bytes([NL]) + line_1399_2 + bytes([NL]) + line_1399_3 + bytes([NL]))

    # -----------------------------------------------------------------------
    # サマリー
    # -----------------------------------------------------------------------
    files = [f for f in os.listdir(OUTPUT_DIR) if f.endswith(".bin")]
    print(f"\n生成完了: {len(files)} ファイル → {OUTPUT_DIR}")


if __name__ == "__main__":
    main()
