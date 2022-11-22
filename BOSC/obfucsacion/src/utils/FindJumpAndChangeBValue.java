package utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static utils.CodeStingToArray.ToArray;

public class FindJumpAndChangeBValue {

    public static List<Integer> findDupicateInArray(String[] bytecode, int index) {

        int count=0;
        List<Integer> indexList = new ArrayList<Integer>();
        for(int j=index;j<bytecode.length;j++) {

            if(bytecode[j].equals("56") || bytecode[j].equals("57")) {
                count++;
            }

            if(count==1) {
                indexList.add(j);
                count = 0;
            }
        }

        return indexList;
    }

    public static String[] ChangeBValue(String[] bytecode,int index, int value){
        //Be aware of hexadecimal and decimal issues
        if (index == 0){
            System.out.println("continue……");
        }else {
            bytecode[index-1] = String.valueOf(Integer.toHexString(Integer.parseInt(bytecode[index-1],16) + value));
        }

        return bytecode;
    }

    public static void main(String[] args) {
        String bytecode = "608060405234801561001057600080fd5b50610150806100206000396000f3fe60806040" +
                "5234801561001057600080fd5b50600436106100365760003560e01c80632e64cec114" +
                "61003b5780636057361d14610059575b600080fd5b610043610075565b60405161005091906100" +
                "d9565b60405180910390f35b610073600480360381019061006e919061009d565b61007e565b005b600" +
                "08054905090565b8060008190555050565b60008135905061009781610103565b92915050565b6000602082" +
                "840312156100b3576100b26100fe565b5b60006100c184828501610088565b91505092915050565b6100d381610" +
                "0f4565b82525050565b60006020820190506100ee60008301846100ca565b92915050565b6000819050919050565b600" +
                "080fd5b61010c816100f4565b811461011757600080fd5b5056fea2646970667358221220404e37f487a89a932dca5e" +
                "77faaf6ca2de3b991f93d230604b1b8daaef64766264736f6c63430008070033";
        String[] temp = ToArray(bytecode);
        List<Integer> a = findDupicateInArray(temp, 0);
        System.out.println(a);

        String[] arr = {"60","5b","56","60"};
        String[] s = ChangeBValue(arr, 2, 1);//ok
        System.out.println(Arrays.toString(s));
    }
}
