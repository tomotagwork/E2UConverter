package com.example.e2uconverter.validator;

import com.example.e2uconverter.report.InvalidCharEntry;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * 1 レコード（行）のバイト配列を受け取り、不正文字・制御文字・SO/SI 不正を検知する。
 */
public class InvalidCharDetector {

    private static final byte SO = (byte) 0x0E;
    private static final byte SI = (byte) 0x0F;

    private final String mode;
    private final Charset charset;

    /**
     * @param mode    入力モード（"fixed" または "stream"）
     * @param charset 変換に使用する Charset（UTF-8 変換後文字の取得に利用）
     */
    public InvalidCharDetector(String mode, Charset charset) {
        this.mode    = mode;
        this.charset = charset;
    }

    /**
     * レコードを検査し、不正文字エントリのリストを返す。
     *
     * @param record   レコードのバイト配列
     * @param recordNo 行番号（1 始まり）
     * @param fileName 入力ファイルパス
     * @return 不正文字エントリリスト（正常なら空リスト）
     */
    public List<InvalidCharEntry> detect(byte[] record, int recordNo, String fileName) {
        List<InvalidCharEntry> entries = new ArrayList<>();

        // ① 制御文字・変換不能コードの検知
        for (int i = 0; i < record.length; i++) {
            int b = record[i] & 0xFF;
            if (isControlChar(b)) {
                String ebcdicHex = String.format("0x%02X", b);
                // 1 バイトを Charset で変換して UTF-8 HEX / 変換後文字を取得
                String[] converted = convertSingleByte(record[i]);
                String reason = (b == 0xFF) ? "変換不能" : "制御文字";
                entries.add(new InvalidCharEntry(
                        fileName, recordNo, i, ebcdicHex,
                        converted[0], converted[1], reason));
            }
        }

        // ② SO/SI ペア検証
        entries.addAll(validateSoSi(record, recordNo, fileName));

        return entries;
    }

    /**
     * モードに応じた制御文字判定。
     * SO(0x0E) / SI(0x0F) は SO/SI 検証で別途評価するため除外。
     */
    private boolean isControlChar(int b) {
        if (b == 0x0E || b == 0x0F) return false; // SO/SI は除外

        if ("fixed".equals(mode)) {
            // 固定長モード: 0x00-0x0D, 0x10-0x3F, 0xFF
            return (b >= 0x00 && b <= 0x0D)
                || (b >= 0x10 && b <= 0x3F)
                || (b == 0xFF);
        } else {
            // バイトストリームモード: 0x00-0x0C, 0x10-0x14, 0x16-0x24, 0x26-0x3F, 0xFF
            // 0x0D(CR), 0x15(NL), 0x25(NL25) は行区切りとして除外
            return (b >= 0x00 && b <= 0x0C)
                || (b >= 0x10 && b <= 0x14)
                || (b >= 0x16 && b <= 0x24)
                || (b >= 0x26 && b <= 0x3F)
                || (b == 0xFF);
        }
    }

    /**
     * SO/SI ペアを検証し、不正なものを InvalidCharEntry として返す。
     */
    private List<InvalidCharEntry> validateSoSi(byte[] record, int recordNo, String fileName) {
        List<InvalidCharEntry> entries = new ArrayList<>();
        int soPos = -1; // 現在開いている SO の位置（-1 = 未開）

        for (int i = 0; i < record.length; i++) {
            byte b = record[i];
            if (b == SO) {
                if (soPos >= 0) {
                    // ネストした SO: 不正
                    entries.add(new InvalidCharEntry(
                            fileName, recordNo, i,
                            String.format("0x%02X", SO & 0xFF),
                            "-", "-", "SO/SI不正"));
                } else {
                    soPos = i;
                }
            } else if (b == SI) {
                if (soPos < 0) {
                    // SO なしで SI: 不正
                    entries.add(new InvalidCharEntry(
                            fileName, recordNo, i,
                            String.format("0x%02X", SI & 0xFF),
                            "-", "-", "SO/SI不正"));
                } else if (i == soPos + 1) {
                    // SO 直後に SI（空 DBCS）: 不正 — SO も不正として追加
                    entries.add(new InvalidCharEntry(
                            fileName, recordNo, soPos,
                            String.format("0x%02X", SO & 0xFF),
                            "-", "-", "SO/SI不正"));
                    entries.add(new InvalidCharEntry(
                            fileName, recordNo, i,
                            String.format("0x%02X", SI & 0xFF),
                            "-", "-", "SO/SI不正"));
                    soPos = -1;
                } else {
                    // 正常な SO/SI ペア
                    soPos = -1;
                }
            }
        }

        // レコード末尾で SO が未クローズ
        if (soPos >= 0) {
            entries.add(new InvalidCharEntry(
                    fileName, recordNo, soPos,
                    String.format("0x%02X", SO & 0xFF),
                    "-", "-", "SO/SI不正"));
        }

        return entries;
    }

    /**
     * 1 バイトを Charset で変換し [utf8Hex, convertedChar] を返す。
     * 変換不能（'?' に置換された場合）は ["-", "-"] を返す。
     */
    private String[] convertSingleByte(byte b) {
        try {
            CharsetDecoder decoder = charset.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPLACE)
                    .onUnmappableCharacter(CodingErrorAction.REPLACE)
                    .replaceWith("?");
            ByteBuffer bb = ByteBuffer.wrap(new byte[]{b});
            CharBuffer cb = decoder.decode(bb);
            String s = cb.toString();
            if (s.isEmpty() || s.equals("?")) {
                return new String[]{"-", "-"};
            }
            char c = s.charAt(0);
            // UTF-8 HEX
            byte[] utf8bytes = s.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            StringBuilder hexSb = new StringBuilder();
            for (byte ub : utf8bytes) {
                hexSb.append(String.format("%02X", ub & 0xFF));
            }
            String utf8Hex = "0x" + hexSb;
            // 表示可能文字かどうか
            String convertedChar = Character.isISOControl(c) ? "-" : s;
            return new String[]{utf8Hex, convertedChar};
        } catch (Exception e) {
            return new String[]{"-", "-"};
        }
    }
}
