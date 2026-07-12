package com.example.e2uconverter.cli;

import org.apache.commons.cli.*;

/**
 * コマンドラインオプションを解析・保持するクラス。
 */
public class CliOptions {

    private String inputPath;
    private String outputDir;
    private String codePage  = "IBM-930";
    private String mode      = "fixed";
    private int    lrecl     = 80;
    private boolean recursive = false;
    private String extension = null;
    private String reportPath = null;

    private CliOptions() {}

    /**
     * コマンドライン引数を解析して CliOptions インスタンスを返す。
     *
     * @param args コマンドライン引数
     * @return 解析結果
     * @throws ParseException 解析エラー時
     */
    public static CliOptions parse(String[] args) throws ParseException {
        Options opts = buildOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(opts, args);
        } catch (ParseException e) {
            throw new ParseException("Invalid argument: " + e.getMessage());
        }

        CliOptions result = new CliOptions();

        // -i (必須)
        if (!cmd.hasOption("i")) {
            throw new ParseException("-i option is required.");
        }
        result.inputPath = cmd.getOptionValue("i");

        // -o (必須)
        if (!cmd.hasOption("o")) {
            throw new ParseException("-o option is required.");
        }
        result.outputDir = cmd.getOptionValue("o");

        // -c
        if (cmd.hasOption("c")) {
            result.codePage = cmd.getOptionValue("c");
        }

        // -m
        if (cmd.hasOption("m")) {
            String m = cmd.getOptionValue("m");
            if (!"fixed".equals(m) && !"stream".equals(m)) {
                throw new ParseException("-m option must be 'fixed' or 'stream'.");
            }
            result.mode = m;
        }

        // -l
        if (cmd.hasOption("l")) {
            try {
                int l = Integer.parseInt(cmd.getOptionValue("l"));
                if (l < 1) {
                    throw new ParseException("-l option must be a positive integer.");
                }
                result.lrecl = l;
            } catch (NumberFormatException e) {
                throw new ParseException("-l option must be a positive integer.");
            }
        }

        // -r
        result.recursive = cmd.hasOption("r");

        // -e
        if (cmd.hasOption("e")) {
            result.extension = cmd.getOptionValue("e");
        }

        // -R
        if (cmd.hasOption("R")) {
            result.reportPath = cmd.getOptionValue("R");
        }

        return result;
    }

    /** 使用方法を標準出力に表示する。 */
    public static void printHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar e2uconverter.jar", buildOptions(), true);
    }

    // -----------------------------------------------------------------------
    // Getters
    // -----------------------------------------------------------------------

    public String getInputPath()  { return inputPath; }
    public String getOutputDir()  { return outputDir; }
    public String getCodePage()   { return codePage; }
    public String getMode()       { return mode; }
    public int    getLrecl()      { return lrecl; }
    public boolean isRecursive()  { return recursive; }
    public String getExtension()  { return extension; }

    /**
     * レポートパスを返す。未指定時は &lt;outputDir&gt;/report.md を返す。
     */
    public String getReportPath() {
        if (reportPath != null) {
            return reportPath;
        }
        return outputDir + java.io.File.separator + "report.md";
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private static Options buildOptions() {
        Options opts = new Options();
        opts.addOption(Option.builder("i").hasArg().argName("path")
                .desc("入力ファイルまたはディレクトリのパス（必須）").build());
        opts.addOption(Option.builder("o").hasArg().argName("dir")
                .desc("出力先ディレクトリ（必須）").build());
        opts.addOption(Option.builder("c").hasArg().argName("codepage")
                .desc("EBCDICコードページ名（デフォルト: IBM-930）").build());
        opts.addOption(Option.builder("m").hasArg().argName("mode")
                .desc("入力モード: fixed または stream（デフォルト: fixed）").build());
        opts.addOption(Option.builder("l").hasArg().argName("length")
                .desc("固定長レコードのLRECL（デフォルト: 80）").build());
        opts.addOption(Option.builder("r")
                .desc("サブディレクトリを再帰処理する").build());
        opts.addOption(Option.builder("e").hasArg().argName("ext")
                .desc("出力ファイルの拡張子（ドットなし）").build());
        opts.addOption(Option.builder("R").hasArg().argName("file")
                .desc("レポート出力先ファイルパス").build());
        return opts;
    }
}
