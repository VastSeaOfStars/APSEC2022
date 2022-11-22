package utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class InsertIndex {
    //5b jumpdest

    //f3 return

    //fd revert

    public static int insertIndex(String[] bytecode){
        String jumpdestIndex = "5b";
        String returnIndex = "f3";
        String revertIndex = "fd";

        int count=0;
        List<Integer> indexList = new ArrayList<Integer>();
        for(int j=0;j<bytecode.length;j++) {

            if(bytecode[j].equals("5b") || bytecode[j].equals("f3") ||
               bytecode[j].equals("fd") || bytecode[j].equals("fe") ||
               bytecode[j].equals("3d") || bytecode[j].equals("ff") ||
               bytecode[j].equals("00") || bytecode[j].equals("32")){
                count++;
            }

            if(count==1) {

                indexList.add(j);
                count = 0;
            }
        }

        Random r = new Random();
        // 2 3 5 4
        // 0 1 2 3
        //bug: When indexList.size() is set to 0, that is, there are none of the above three places,
        // and there is no place to insert,
        //bug :Exception in thread "main" java.lang.IllegalArgumentException: bound must be positive
        //Solution: increase the insertion place
        System.out.println(Arrays.toString(bytecode));
        int i = r.nextInt(indexList.size());
        return indexList.get(i);
    }

    public static void main(String[] args) {
        String[] exam = {"60","00","60","5b","08","fd","f3"};
        System.out.println(insertIndex(exam));
    }
}
