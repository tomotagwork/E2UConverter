package com.example.e2uconverter.report;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 全変換処理の結果を集積するクラス。シングルスレッド前提。
 */
public class ReportData {

    private final List<FileResult>        fileResults         = new ArrayList<>();
    private final List<InvalidCharEntry>  invalidCharEntries  = new ArrayList<>();

    /** ファイル結果を追加する。 */
    public void addFileResult(FileResult result) {
        fileResults.add(result);
    }

    /** 不正文字エントリリストを追加する。 */
    public void addInvalidCharEntries(List<InvalidCharEntry> entries) {
        invalidCharEntries.addAll(entries);
    }

    /** 処理ファイル総数を返す。 */
    public int getTotalFiles() {
        return fileResults.size();
    }

    /** 変換成功ファイル数（ステータス OK）を返す。 */
    public int getSuccessCount() {
        return (int) fileResults.stream()
                .filter(r -> r.getStatus() == FileResult.Status.OK)
                .count();
    }

    /** 不正文字検知ファイル数（ステータス WARNING）を返す。 */
    public int getWarningCount() {
        return (int) fileResults.stream()
                .filter(r -> r.getStatus() == FileResult.Status.WARNING)
                .count();
    }

    /** エラーファイル数（ステータス ERROR）を返す。 */
    public int getErrorCount() {
        return (int) fileResults.stream()
                .filter(r -> r.getStatus() == FileResult.Status.ERROR)
                .count();
    }

    /** 不正文字検知総件数を返す。 */
    public int getTotalInvalidCharCount() {
        return invalidCharEntries.size();
    }

    /** ファイル結果リストを返す（不変ビュー）。 */
    public List<FileResult> getFileResults() {
        return Collections.unmodifiableList(fileResults);
    }

    /** 不正文字エントリリストを返す（不変ビュー）。 */
    public List<InvalidCharEntry> getInvalidCharEntries() {
        return Collections.unmodifiableList(invalidCharEntries);
    }
}
