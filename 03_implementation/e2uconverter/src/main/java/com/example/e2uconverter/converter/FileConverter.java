package com.example.e2uconverter.converter;

import com.example.e2uconverter.cli.CliOptions;
import com.example.e2uconverter.report.*;
import com.example.e2uconverter.util.CodePageUtil;
import com.example.e2uconverter.validator.InvalidCharDetector;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 変換処理全体を制御するクラス。
 * 入力パスの走査、各ファイルの変換、レポートデータの集積を行う。
 */
public class FileConverter {

    /**
     * 変換処理のメインメソッド。
     *
     * @param options 変換オプション
     */
    public void convert(CliOptions options) {
        LocalDateTime startTime = LocalDateTime.now();
        System.out.println("[INFO] E2UConverter started.");

        Charset charset = CodePageUtil.getCharset(options.getCodePage());
        ReportData reportData = new ReportData();
        InvalidCharDetector detector = new InvalidCharDetector(options.getMode(), charset);

        // 出力先ディレクトリを作成
        File outputDir = new File(options.getOutputDir());
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            System.err.println("[ERROR] Cannot create output directory: " + outputDir.getAbsolutePath());
            System.exit(1);
            return;
        }

        // 入力ファイル列挙
        File inputPath = new File(options.getInputPath());
        List<File> files = collectFiles(inputPath, options.isRecursive());

        File baseDir = inputPath.isDirectory() ? inputPath : inputPath.getParentFile();

        // 各ファイルを変換
        for (File file : files) {
            convertFile(file, baseDir, options, charset, detector, reportData);
        }

        // レポート出力
        ReportWriter.write(reportData, options, startTime);
        System.out.println("[INFO] Report: " + options.getReportPath());
        System.out.printf("[INFO] Done. Total: %d, OK: %d, WARNING: %d, ERROR: %d%n",
                reportData.getTotalFiles(),
                reportData.getSuccessCount(),
                reportData.getWarningCount(),
                reportData.getErrorCount());
    }

    /**
     * 入力パスを走査してファイルリストを収集する。
     *
     * @param inputPath 入力パス（ファイルまたはディレクトリ）
     * @param recursive サブディレクトリを再帰処理するか
     * @return ファイルリスト
     */
    private List<File> collectFiles(File inputPath, boolean recursive) {
        List<File> result = new ArrayList<>();
        if (inputPath.isFile()) {
            result.add(inputPath);
        } else if (inputPath.isDirectory()) {
            collectFromDir(inputPath, recursive, result);
        }
        return result;
    }

    private void collectFromDir(File dir, boolean recursive, List<File> result) {
        File[] entries = dir.listFiles();
        if (entries == null) return;
        for (File entry : entries) {
            if (entry.isFile()) {
                result.add(entry);
            } else if (entry.isDirectory() && recursive) {
                collectFromDir(entry, true, result);
            }
        }
    }

    /**
     * 単一ファイルを変換する。
     */
    private void convertFile(File inputFile, File baseDir,
                             CliOptions options, Charset charset,
                             InvalidCharDetector detector, ReportData reportData) {
        String inputFilePath = inputFile.getAbsolutePath();
        List<InvalidCharEntry> allEntries = new ArrayList<>();
        int totalInvalid = 0;

        // 出力ファイルパスを決定
        File outputFile = resolveOutputFile(inputFile, baseDir, options);

        // 出力先ディレクトリを作成
        File outParent = outputFile.getParentFile();
        if (outParent != null && !outParent.exists()) {
            if (!outParent.mkdirs()) {
                String msg = "出力ディレクトリ作成エラー: " + outParent.getAbsolutePath();
                System.out.println("[ERROR] ERROR   : " + inputFilePath + " - " + msg);
                reportData.addFileResult(new FileResult(inputFilePath,
                        FileResult.Status.ERROR, 0, msg));
                return;
            }
        }

        CharsetDecoder decoder = charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE)
                .replaceWith("?");

        try (RecordReader reader = createReader(inputFile, options);
             BufferedWriter writer = new BufferedWriter(
                     new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8"))) {

            int recordNo = 1;
            byte[] record;
            while ((record = reader.readRecord()) != null) {
                // 不正文字検知
                List<InvalidCharEntry> entries = detector.detect(record, recordNo, inputFilePath);
                allEntries.addAll(entries);
                totalInvalid += entries.size();

                // EBCDIC → UTF-8 変換
                // SO/SI 不正として検知されたコードを '?' に置換した上で変換する
                byte[] processedRecord = replaceInvalidSoSiBytes(record, entries, recordNo, decoder);
                String line = decodeRecord(processedRecord, decoder);

                writer.write(line);
                writer.write("\n"); // LF
                recordNo++;
            }

        } catch (IOException e) {
            String msg = "ファイル読み込みエラー: " + e.getMessage();
            System.out.println("[ERROR] ERROR   : " + inputFilePath + " - " + msg);
            reportData.addInvalidCharEntries(allEntries);
            reportData.addFileResult(new FileResult(inputFilePath,
                    FileResult.Status.ERROR, totalInvalid, msg));
            return;
        }

        reportData.addInvalidCharEntries(allEntries);

        FileResult.Status status = (totalInvalid > 0)
                ? FileResult.Status.WARNING
                : FileResult.Status.OK;
        reportData.addFileResult(new FileResult(inputFilePath, status, totalInvalid, null));

        if (status == FileResult.Status.WARNING) {
            System.out.printf("[WARN] WARNING : %s (%d invalid char(s))%n",
                    inputFilePath, totalInvalid);
        } else {
            System.out.println("[INFO] OK      : " + inputFilePath);
        }
    }

    /**
     * RecordReader を生成する。
     */
    private RecordReader createReader(File file, CliOptions options) throws FileNotFoundException {
        if ("fixed".equals(options.getMode())) {
            return new FixedRecordReader(file, options.getLrecl());
        } else {
            return new StreamRecordReader(file);
        }
    }

    /**
     * SO/SI 不正・変換不能として検知されたオフセット位置のバイトを、
     * コードページ内の '?'（U+003F）に対応する EBCDIC バイトに置換したコピーを返す。
     * 制御文字はデコーダーの REPLACE 設定で '?' に変換されるが、IBM-930 の 0xFF のように
     * Charset が有効な Unicode にマッピングする場合は REPLACE が動作しないため、
     * 変換不能エントリも明示的に置換する。
     */
    private byte[] replaceInvalidSoSiBytes(byte[] record, List<InvalidCharEntry> entries, int recordNo, CharsetDecoder decoder) {
        // SO/SI 不正 および 変換不能 エントリのオフセットセットを収集
        java.util.Set<Integer> invalidOffsets = new java.util.HashSet<>();
        for (InvalidCharEntry e : entries) {
            if (e.getRecordNo() == recordNo
                    && ("SO/SI不正".equals(e.getReason()) || "変換不能".equals(e.getReason()))) {
                invalidOffsets.add(e.getOffset());
            }
        }
        if (invalidOffsets.isEmpty()) {
            return record;
        }
        // コードページ内の '?' に対応する EBCDIC バイトを取得（例: IBM-930 では 0x6F）
        byte questionByte = getQuestionByte(decoder.charset());
        byte[] copy = record.clone();
        for (int off : invalidOffsets) {
            if (off < copy.length) {
                copy[off] = questionByte;
            }
        }
        return copy;
    }

    /**
     * 指定 Charset で '?'（U+003F）を符号化したときの先頭バイトを返す。
     * 符号化に失敗した場合は EBCDIC 標準の '?' コード 0x6F を返す。
     */
    private byte getQuestionByte(java.nio.charset.Charset cs) {
        try {
            byte[] encoded = "?".getBytes(cs);
            if (encoded.length > 0) return encoded[0];
        } catch (Exception e) {
            // ignore
        }
        return (byte) 0x6F; // EBCDIC '?' のデフォルト
    }

    /**
     * レコードバイト配列を Charset でデコードして文字列を返す。
     * 変換不能文字は '?' に置換される（decoder の REPLACE 設定による）。
     */
    private String decodeRecord(byte[] record, CharsetDecoder decoder) {
        try {
            decoder.reset();
            CharBuffer cb = decoder.decode(ByteBuffer.wrap(record));
            return cb.toString();
        } catch (Exception e) {
            // フォールバック: バイトごとに変換
            StringBuilder sb = new StringBuilder();
            for (byte b : record) {
                try {
                    decoder.reset();
                    CharBuffer cb = decoder.decode(ByteBuffer.wrap(new byte[]{b}));
                    sb.append(cb.toString());
                } catch (Exception ex) {
                    sb.append('?');
                }
            }
            return sb.toString();
        }
    }

    /**
     * 出力ファイルのパスを決定する。
     */
    private File resolveOutputFile(File inputFile, File baseDir, CliOptions options) {
        File outputDir = new File(options.getOutputDir());
        // 相対パスを計算
        String relativePath = baseDir.toURI().relativize(inputFile.toURI()).getPath();

        // 拡張子を決定して置換
        String newFileName = applyExtension(relativePath, options);

        return new File(outputDir, newFileName);
    }

    /**
     * ファイルパスに出力拡張子ルールを適用して返す。
     */
    private String applyExtension(String relativePath, CliOptions options) {
        // パスのファイル名部分だけに対して拡張子を処理
        int slashIdx = relativePath.lastIndexOf('/');
        String dirPart = (slashIdx >= 0) ? relativePath.substring(0, slashIdx + 1) : "";
        String fileName = (slashIdx >= 0) ? relativePath.substring(slashIdx + 1) : relativePath;

        String newFileName = resolveOutputExtension(fileName, options);
        return dirPart + newFileName;
    }

    /**
     * ファイル名（ディレクトリなし）に対して出力拡張子を決定する。
     */
    private String resolveOutputExtension(String inputFileName, CliOptions options) {
        if (options.getExtension() != null) {
            // -e 指定あり: 元の拡張子に関わらず置換
            int dotIdx = inputFileName.lastIndexOf('.');
            String baseName = (dotIdx >= 0) ? inputFileName.substring(0, dotIdx) : inputFileName;
            return baseName + "." + options.getExtension();
        }
        // -e 未指定
        int dotIdx = inputFileName.lastIndexOf('.');
        if (dotIdx >= 0) {
            // 拡張子あり: そのまま保持
            return inputFileName;
        } else {
            // 拡張子なし: .txt を付与
            return inputFileName + ".txt";
        }
    }
}
