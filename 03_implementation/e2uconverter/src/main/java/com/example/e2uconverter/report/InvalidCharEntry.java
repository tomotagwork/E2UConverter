package com.example.e2uconverter.report;

/**
 * 不正文字 1 件の情報を保持するイミュータブルオブジェクト。
 */
public class InvalidCharEntry {

    private final String fileName;
    private final int    recordNo;
    private final int    offset;
    private final String ebcdicHex;
    private final String utf8Hex;
    private final String convertedChar;
    private final String reason;

    /**
     * @param fileName      入力ファイルのパス
     * @param recordNo      行番号（1 始まり）
     * @param offset        行頭からのオフセット（0 始まり）
     * @param ebcdicHex     EBCDIC HEX 値（例: {@code 0x1F}）
     * @param utf8Hex       変換後 UTF-8 HEX 値（変換不能時は {@code -}）
     * @param convertedChar 変換後文字（表示不能時は {@code -}）
     * @param reason        検知理由
     */
    public InvalidCharEntry(String fileName, int recordNo, int offset,
                            String ebcdicHex, String utf8Hex,
                            String convertedChar, String reason) {
        this.fileName      = fileName;
        this.recordNo      = recordNo;
        this.offset        = offset;
        this.ebcdicHex     = ebcdicHex;
        this.utf8Hex       = utf8Hex;
        this.convertedChar = convertedChar;
        this.reason        = reason;
    }

    public String getFileName()      { return fileName; }
    public int    getRecordNo()      { return recordNo; }
    public int    getOffset()        { return offset; }
    public String getEbcdicHex()     { return ebcdicHex; }
    public String getUtf8Hex()       { return utf8Hex; }
    public String getConvertedChar() { return convertedChar; }
    public String getReason()        { return reason; }
}
