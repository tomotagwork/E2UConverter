package com.example.e2uconverter.util;

import java.nio.charset.Charset;

/**
 * コードページ検証ユーティリティ。
 * IBM-1390 / IBM-1399 等の ICU4J 提供コードページにも対応する。
 */
public class CodePageUtil {

    private CodePageUtil() {}

    /**
     * 指定コードページが利用可能かどうかを返す。
     *
     * @param codePage コードページ名
     * @return サポート済みの場合 {@code true}
     */
    public static boolean isSupported(String codePage) {
        if (codePage == null || codePage.isEmpty()) {
            return false;
        }
        // ICU4J の Charset プロバイダを確実にロードする
        ensureIcuCharsetProvider();
        try {
            Charset.forName(codePage);
            return true;
        } catch (IllegalArgumentException e) {
            // UnsupportedCharsetException は IllegalArgumentException のサブクラス
            return false;
        }
    }

    /**
     * 指定コードページの {@link Charset} を返す。
     *
     * @param codePage コードページ名
     * @return Charset インスタンス
     * @throws IllegalArgumentException サポートされていない場合
     */
    public static Charset getCharset(String codePage) {
        ensureIcuCharsetProvider();
        try {
            return Charset.forName(codePage);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported codepage: " + codePage, e);
        }
    }

    /**
     * ICU4J の CharsetProvider を ServiceLoader 経由でロードする。
     * クラスロード時に一度だけ呼べばよいが、static メソッドから安全に呼び出せるようにする。
     */
    private static volatile boolean icuLoaded = false;

    private static void ensureIcuCharsetProvider() {
        if (icuLoaded) return;
        synchronized (CodePageUtil.class) {
            if (icuLoaded) return;
            // ICU4J Charset プロバイダを強制ロード
            try {
                Class.forName("com.ibm.icu.charset.CharsetICU");
            } catch (ClassNotFoundException ignored) {
                // ICU4J が存在しない環境では何もしない
            }
            icuLoaded = true;
        }
    }
}
