package utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * data input class
 */
public class ByteCodeInput {
    //private String fileName;

    //1、read file method

    /**
     * Read bytecode filexxx.hex
     * @param url
     * @return List<String>
     * @throws Exception
     */
    public static List readFileContent(String url) throws Exception {
        // change to List<String>, java.lang.OutOfMemoryError: Java heap space
        List<String> lines = Files.readAllLines(Paths.get(url), StandardCharsets.UTF_8);
        return lines;
    }


    //tested
    public static void main(String[] args) throws Exception {
        List l = readFileContent("F:\\桌面文件\\区块链研究汇报\\字节码混淆APSEC2022" +
                "\\ChiWen\\obfucsacion\\src\\dataset\\example.hex");
        System.out.println(l);
    }
}
