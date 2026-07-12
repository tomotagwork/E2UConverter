package com.example.e2uconverter.converter;

import java.io.Closeable;
import java.io.IOException;

/**
 * レコード単位にバイト配列を返す抽象インターフェース。
 */
public interface RecordReader extends Closeable {

    /**
     * 次のレコードのバイト配列を返す。
     * ファイル末尾に達した場合は {@code null} を返す。
     *
     * @return レコードのバイト配列、または EOF 時に {@code null}
     * @throws IOException 読み込みエラー時
     */
    byte[] readRecord() throws IOException;

    /**
     * リソースを解放する。
     *
     * @throws IOException クローズエラー時
     */
    @Override
    void close() throws IOException;
}
