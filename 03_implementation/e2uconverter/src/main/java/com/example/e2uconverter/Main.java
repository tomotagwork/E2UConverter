package com.example.e2uconverter;

import com.example.e2uconverter.cli.CliOptions;
import com.example.e2uconverter.converter.FileConverter;
import com.example.e2uconverter.util.CodePageUtil;

import java.io.File;

/**
 * E2UConverter エントリーポイント。
 */
public class Main {

    public static void main(String[] args) {
        // 引数解析
        CliOptions options;
        try {
            options = CliOptions.parse(args);
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            CliOptions.printHelp();
            System.exit(1);
            return;
        }

        // コードページ検証
        if (!CodePageUtil.isSupported(options.getCodePage())) {
            System.err.println("ERROR: Unsupported codepage: " + options.getCodePage());
            System.exit(1);
            return;
        }

        // 入力パス存在チェック
        File inputPath = new File(options.getInputPath());
        if (!inputPath.exists()) {
            System.err.println("ERROR: Input path does not exist: " + options.getInputPath());
            System.exit(1);
            return;
        }

        // 変換処理実行
        FileConverter converter = new FileConverter();
        converter.convert(options);
    }
}
