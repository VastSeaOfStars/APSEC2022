package utils;

import java.util.Arrays;

public class CodeStingToArray {

    public static String[] ToArray(String bytecode){
        int m = bytecode.length()/2;

        if(m*2 < bytecode.length()){
            m++;
        }

        String[] strs=new String[m];

        int j = 0;
        for(int i = 0;i < bytecode.length();i++){
            if(i%2 == 0){
                strs[j] = "" + bytecode.charAt(i);
            }else{
                strs[j] = strs[j] + "" + bytecode.charAt(i);
                j++;
            }
        }
        return strs;
    }

    public static void main(String[] args) {
        String bytecode = "608060400";
        String[] temp = ToArray(bytecode);
        System.out.println(Arrays.toString(temp));
        System.out.println(temp[1]);
        System.out.println(temp[1].equals("80"));
    }

}
