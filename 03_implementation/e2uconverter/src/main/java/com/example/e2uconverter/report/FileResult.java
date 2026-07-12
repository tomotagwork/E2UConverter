package com.example.e2uconverter.report;

/**
 * 単一ファイルの変換結果を保持するイミュータブルオブジェクト。
 */
public class FileResult {

    /** 処理ステータス */
    public enum Status {
        /** 不正文字なしで変換成功 */
        OK,
        /** 不正文字を検知したが変換完了 */
        WARNING,
        /** 処理失敗 */
        ERROR
    }

    private final String inputFilePath;
    private final Status status;
    private final int    invalidCharCount;
    private final String errorMessage;

    /**
     * @param inputFilePath   入力ファイルのパス
     * @param status          ステータス
     * @param invalidCharCount 不正文字検知件数
     * @param errorMessage    エラーメッセージ（エラー時のみ、他は {@code null}）
     */
    public FileResult(String inputFilePath, Status status,
                      int invalidCharCount, String errorMessage) {
        this.inputFilePath    = inputFilePath;
        this.status           = status;
        this.invalidCharCount = invalidCharCount;
        this.errorMessage     = errorMessage;
    }

    public String getInputFilePath()   { return inputFilePath; }
    public Status getStatus()          { return status; }
    public int    getInvalidCharCount() { return invalidCharCount; }
    public String getErrorMessage()    { return errorMessage; }
}
