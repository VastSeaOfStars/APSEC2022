package utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * data output class
 */
public class ByteCodeOutput {
    //1、Receive the obfuscated bytecode and output it as "current time + obfuscated.hex" file


    // The main function of this method is to determine whether the creation of the file is successful,
    // if successful, return true, otherwise return false
    public static boolean createFile(String destFileName,String info) {
        File file = new File(destFileName);
        if (file.exists()) {
            System.out.println("Create a single file" + destFileName + "failed, target file already exists!");
            return false;
        }
        if (destFileName.endsWith(File.separator)) {
            System.out.println("Create a single file" + destFileName + "failed, target file cannot be a directory!");
            return false;
        }


        // Determine whether the directory where the target file is located exists
        if (!file.getParentFile().exists()) {
            System.out.println("Create" + file.getName() + "The directory does not exist, it is being created!");
            if (!file.getParentFile().mkdirs()) {
                System.out.println("Failed to create the directory where the object file is located!");
                return false;
            }
        }


        // Create object file
        try {
            if (file.createNewFile()) {
                System.out.println("Create a single file" + destFileName + "succeeded！");
                java.io.OutputStream out = new FileOutputStream(file);
                out.write(info.getBytes("utf-8"));
                out.close();
                return true;
            } else {
                System.out.println("Create a single file" + destFileName + "failed！");
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Create file" + destFileName + "failed！" + e.getMessage());
            return false;
        }
    }


    public static void main(String[] args) {

        String dirName = "D:/temp";

        Date date = new Date();
        SimpleDateFormat dateFormat= new SimpleDateFormat("yyyy-MM-dd HH：mm：ss");
        String s = dateFormat.format(date);
        System.out.println(s);

        String fileName = dirName + '/'+ s+"obfuscated.hex";
        //ByteCodeOutput.createFile(fileName);
    }
}

