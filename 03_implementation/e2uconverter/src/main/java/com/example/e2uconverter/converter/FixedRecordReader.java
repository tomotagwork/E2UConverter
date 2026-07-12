package com.example.e2uconverter.converter;

import java.io.*;

/**
 * 固定長レコードモードの RecordReader 実装。
 * ファイルを LRECL バイト単位に分割して読み込む。
 */
public class FixedRecordReader implements RecordReader {

    private final InputStream inputStream;
    private final int lrecl;

    /**
     * @param file  入力ファイル
     * @param lrecl レコード長（バイト数）
     * @throws FileNotFoundException ファイルが存在しない場合
     */
    public FixedRecordReader(File file, int lrecl) throws FileNotFoundException {
        this.inputStream = new BufferedInputStream(new FileInputStream(file));
        this.lrecl = lrecl;
    }

    /**
     * LRECL バイト読み込む。不足した場合は実際に読めたバイト数分の配列を返す。
     * EOF の場合は {@code null} を返す。
     */
    @Override
    public byte[] readRecord() throws IOException {
        byte[] buf = new byte[lrecl];
        int totalRead = 0;
        while (totalRead < lrecl) {
            int n = inputStream.read(buf, totalRead, lrecl - totalRead);
            if (n == -1) break;
            totalRead += n;
        }
        if (totalRead == 0) {
            return null; // EOF
        }
        if (totalRead == lrecl) {
            return buf;
        }
        // 最終レコードが LRECL に満たない場合
        byte[] trimmed = new byte[totalRead];
        System.arraycopy(buf, 0, trimmed, 0, totalRead);
        return trimmed;
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }
}
