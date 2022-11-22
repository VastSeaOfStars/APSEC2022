package utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class ByteCodeInput {

    public static List readFileContent(String url) throws Exception {
        // change to List<String>, java.lang.OutOfMemoryError: Java heap space
        List<String> lines = Files.readAllLines(Paths.get(url), StandardCharsets.UTF_8);
        return lines;
    }


    //tested
    public static void main(String[] args) throws Exception {
        List l = readFileContent("F:\\obfucsacion\\src\\dataset\\example.hex");
        System.out.println(l);
    }
}
