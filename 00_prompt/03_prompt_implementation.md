01_requirements\フォルダ以下の要件を満たすプログラムを、02_design\ フォルダ以下に記述されている設計書に従って実装してください。
成果物は、03_implementation\フォルダ以下に作成してください。

コンバーター IBM-1390, IBM-1399 を使用できるように、以下を参考にビルド時にはpom.xmlにicu4j関連のライブラリを追加してください。
https://mvnrepository.com/artifact/com.ibm.icu/icu4j
https://mvnrepository.com/artifact/com.ibm.icu/icu4j-charset
```
    <dependency>
      <groupId>com.ibm.icu</groupId>
      <artifactId>icu4j</artifactId>
      <version>78.3</version>
    </dependency>
    <dependency>
      <groupId>com.ibm.icu</groupId>
      <artifactId>icu4j-charset</artifactId>
      <version>78.3</version>
    </dependency>
```
