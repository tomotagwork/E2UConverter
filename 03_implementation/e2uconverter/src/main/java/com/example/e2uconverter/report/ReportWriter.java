package com.example.e2uconverter.report;

import com.example.e2uconverter.cli.CliOptions;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ReportData と CliOptions を受け取り、Markdown 形式のレポートファイルを生成する。
 */
public class ReportWriter {

    private static final DateTimeFormatter DTF =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ReportWriter() {}

    /**
     * レポートファイルを生成する。
     *
     * @param data      集積済みレポートデータ
     * @param options   変換オプション
     * @param startTime 処理開始日時
     */
    public static void write(ReportData data, CliOptions options, LocalDateTime startTime) {
        StringBuilder sb = new StringBuilder();
        sb.append("# E2UConverter 変換レポート\n\n");
        sb.append(buildSummarySection(options, startTime));
        sb.append(buildStatisticsSection(data));
        sb.append(buildFileListSection(data));
        sb.append(buildErrorListSection(data));
        sb.append(buildInvalidCharSection(data));
        sb.append(buildCodeTableSection(options));

        String reportPath = options.getReportPath();
        File reportFile = new File(reportPath);
        // 親ディレクトリを自動作成
        File parentDir = reportFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        try (Writer writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(reportFile), "UTF-8"))) {
            writer.write(sb.toString());
        } catch (IOException e) {
            System.err.println("[ERROR] Failed to write report: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // セクション生成
    // -----------------------------------------------------------------------

    private static String buildSummarySection(CliOptions options, LocalDateTime startTime) {
        String modeStr = "fixed".equals(options.getMode())
                ? "fixed (LRECL=" + options.getLrecl() + ")"
                : "stream";
        String recursiveStr = options.isRecursive() ? "有効" : "無効";

        return "## 1. 実行サマリー\n\n"
                + "| 項目 | 内容 |\n"
                + "|------|------|\n"
                + "| 処理日時 | " + startTime.format(DTF) + " |\n"
                + "| 処理対象 | " + options.getInputPath() + " |\n"
                + "| 出力先 | " + options.getOutputDir() + " |\n"
                + "| 使用コードページ | " + options.getCodePage() + " |\n"
                + "| 入力モード | " + modeStr + " |\n"
                + "| 再帰処理 | " + recursiveStr + " |\n\n";
    }

    private static String buildStatisticsSection(ReportData data) {
        return "## 2. 処理統計\n\n"
                + "| 項目 | 件数 |\n"
                + "|------|------|\n"
                + "| 処理ファイル総数 | " + data.getTotalFiles() + " |\n"
                + "| 変換成功ファイル数 | " + data.getSuccessCount() + " |\n"
                + "| 不正文字検知ファイル数 | " + data.getWarningCount() + " |\n"
                + "| 不正文字検知総件数 | " + data.getTotalInvalidCharCount() + " |\n"
                + "| エラーファイル数 | " + data.getErrorCount() + " |\n\n";
    }

    private static String buildFileListSection(ReportData data) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 3. 処理ファイル一覧\n\n");
        sb.append("| ファイル名 | ステータス | 不正文字検知件数 |\n");
        sb.append("|-----------|-----------|----------------|\n");
        for (FileResult r : data.getFileResults()) {
            sb.append("| ").append(escape(r.getInputFilePath()))
              .append(" | ").append(r.getStatus().name())
              .append(" | ").append(r.getInvalidCharCount())
              .append(" |\n");
        }
        sb.append("\n");
        return sb.toString();
    }

    private static String buildErrorListSection(ReportData data) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 4. エラーファイル一覧\n\n");
        sb.append("| ファイル名 | エラー内容 |\n");
        sb.append("|-----------|----------|\n");
        boolean hasError = false;
        for (FileResult r : data.getFileResults()) {
            if (r.getStatus() == FileResult.Status.ERROR) {
                hasError = true;
                sb.append("| ").append(escape(r.getInputFilePath()))
                  .append(" | ").append(escape(r.getErrorMessage()))
                  .append(" |\n");
            }
        }
        if (!hasError) {
            sb.append("| （なし） | |\n");
        }
        sb.append("\n");
        return sb.toString();
    }

    private static String buildInvalidCharSection(ReportData data) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 5. 不正文字検知詳細\n\n");
        sb.append("| ファイル名 | 行番号 | オフセット | EBCDIC HEX値 | UTF-8 HEX値 | 変換後文字 | 検知理由 |\n");
        sb.append("|-----------|-------|-----------|-------------|------------|----------|--------|\n");
        if (data.getInvalidCharEntries().isEmpty()) {
            sb.append("| （なし） | | | | | | |\n");
        } else {
            for (InvalidCharEntry e : data.getInvalidCharEntries()) {
                sb.append("| ").append(escape(e.getFileName()))
                  .append(" | ").append(e.getRecordNo())
                  .append(" | ").append(e.getOffset())
                  .append(" | ").append(e.getEbcdicHex())
                  .append(" | ").append(e.getUtf8Hex())
                  .append(" | ").append(escape(e.getConvertedChar()))
                  .append(" | ").append(e.getReason())
                  .append(" |\n");
            }
        }
        sb.append("\n");
        return sb.toString();
    }

    private static String buildCodeTableSection(CliOptions options) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 6. SBCS コード変換対応表（付録）\n\n");
        sb.append("コードページ: ").append(options.getCodePage()).append("\n\n");
        sb.append("| EBCDIC HEX値 | UTF-8 HEX値 | UTF-8文字 | 逆変換 EBCDIC HEX値 |\n");
        sb.append("|-------------|------------|----------|-------------------|\n");

        Charset cs;
        try {
            cs = Charset.forName(options.getCodePage());
        } catch (Exception e) {
            sb.append("（コードページ取得エラー）\n");
            return sb.toString();
        }

        CharsetDecoder decoder = cs.newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE)
                .replaceWith("?");
        CharsetEncoder encoder = cs.newEncoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE)
                .replaceWith(new byte[]{(byte) 0x3F}); // '?'

        for (int code = 0x00; code <= 0xFF; code++) {
            String ebcdicHex = String.format("0x%02X", code);
            byte[] singleByte = {(byte) code};

            // EBCDIC → UTF-8
            String utf8Hex;
            String utf8Char;
            String reverseHex;
            try {
                CharBuffer cb = decoder.decode(ByteBuffer.wrap(singleByte));
                decoder.reset();
                String decoded = cb.toString();
                if (decoded.isEmpty() || decoded.equals("?")) {
                    utf8Hex  = "-";
                    utf8Char = "-";
                    reverseHex = "-";
                } else {
                    char c = decoded.charAt(0);
                    byte[] utf8bytes = decoded.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    StringBuilder hexSb = new StringBuilder("0x");
                    for (byte ub : utf8bytes) {
                        hexSb.append(String.format("%02X", ub & 0xFF));
                    }
                    utf8Hex  = hexSb.toString();
                    utf8Char = Character.isISOControl(c) ? "-" : decoded;

                    // 逆変換: UTF-8 → EBCDIC
                    try {
                        ByteBuffer bb = encoder.encode(cb.rewind());
                        encoder.reset();
                        StringBuilder revSb = new StringBuilder("0x");
                        while (bb.hasRemaining()) {
                            revSb.append(String.format("%02X", bb.get() & 0xFF));
                        }
                        reverseHex = revSb.toString();
                    } catch (Exception ex) {
                        reverseHex = "-";
                    }
                }
            } catch (Exception ex) {
                decoder.reset();
                utf8Hex    = "-";
                utf8Char   = "-";
                reverseHex = "-";
            }

            sb.append("| ").append(ebcdicHex)
              .append(" | ").append(utf8Hex)
              .append(" | ").append(escape(utf8Char))
              .append(" | ").append(reverseHex)
              .append(" |\n");
        }
        sb.append("\n");
        return sb.toString();
    }

    /** Markdown テーブル内の特殊文字をエスケープする。 */
    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("|", "\\|").replace("\n", " ").replace("\r", "");
    }
}
