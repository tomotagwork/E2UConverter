package com.example.e2uconverter.converter;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * バイトストリームモードの RecordReader 実装。
 * EBCDIC CR（0x0D）/ NL（0x15）/ NL25（0x25）を行区切りとして 1 行ずつ返す。
 * 行区切りコード自身は返す配列に含めない。
 */
public class StreamRecordReader implements RecordReader {

    private final InputStream inputStream;
    private boolean eof = false;

    /**
     * @param file 入力ファイル
     * @throws FileNotFoundException ファイルが存在しない場合
     */
    public StreamRecordReader(File file) throws FileNotFoundException {
        this.inputStream = new BufferedInputStream(new FileInputStream(file));
    }

    /**
     * 行区切りコードまでのバイト配列を返す（区切りコード自身は含まない）。
     * EOF の場合は {@code null} を返す。
     * ファイル末尾で行区切りなしのデータが残っている場合は、そのデータを返す。
     */
    @Override
    public byte[] readRecord() throws IOException {
        if (eof) {
            return null;
        }
        List<Byte> buffer = new ArrayList<>();
        while (true) {
            int b = inputStream.read();
            if (b == -1) {
                eof = true;
                if (buffer.isEmpty()) {
                    return null;
                }
                break;
            }
            // 行区切りコード: EBCDIC CR(0x0D), NL(0x15), NL25(0x25)
            if (b == 0x0D || b == 0x15 || b == 0x25) {
                break;
            }
            buffer.add((byte) b);
        }
        byte[] result = new byte[buffer.size()];
        for (int i = 0; i < buffer.size(); i++) {
            result[i] = buffer.get(i);
        }
        return result;
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }
}
