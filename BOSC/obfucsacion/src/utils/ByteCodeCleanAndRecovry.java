package utils;

import cn.hutool.core.util.ArrayUtil;

import java.util.Arrays;

import static utils.CodeStingToArray.ToArray;


/**
 * Bytecode cleaning and recovery
 */
public class ByteCodeCleanAndRecovry {

    public static String[] byteCodeClean(String[] bytecode){
        //60 80 60 40
        //0  1  2  3
        int start = 0;
        int end = bytecode.length-1;
        for (int i = 0; i < bytecode.length; i++) {
            if (bytecode[i].equals("f3")/*&&bytecode[i+1].equals("fe")
                    || bytecode[i].equals("f3")&&bytecode[i+1].equals("00")*/){
                start = i+1;
                System.out.println("start:"+start);
                break;
            }
        }

        for (int i = 0; i < bytecode.length; i++) {
            if(bytecode[i].equals("a1") || bytecode[i].equals("a2")||
                    bytecode[i].equals("a3") || bytecode[i].equals("a4")/*&&bytecode[i+1].equals("fe")
                    || bytecode[i].equals("fd")&&bytecode[i+1].equals("00")*/){
                end = i;
                break;
            }
        }

        if(end==bytecode.length-1){
            System.out.println("无auxdata");
//            String[] rs = new String[bytecode.length-start];
//            System.arraycopy(bytecode,start+1,rs,0,bytecode.length-start);
            System.out.println(start+2);
              String[] rs = ArrayUtil.sub(bytecode, start+1, end);
              return rs; 
        }else {//有auxdata
            System.out.println("有auxdata");
//            String[] rs = new String[end-start+1];
//            System.arraycopy(bytecode,start+1,rs,0,end-start+1);
            String[] rs = ArrayUtil.sub(bytecode, start+1, end);
            return rs; 
        }
    }

    public static String[] byteCodeRecovery(String[] bytecode, String[] obfuscatedBytecode){

        int start = 0;
        int end = bytecode.length-1;
        for (int i = 0; i < bytecode.length; i++) {
            if (bytecode[i].equals("f3")/*&&bytecode[i+1].equals("fe")
                    || bytecode[i].equals("f3")&&bytecode[i+1].equals("00")*/){
                start = i+1;
                System.out.println("start:"+start);
                break;
            }
        }

        for (int i = 0; i < bytecode.length; i++) {
            if(bytecode[i].equals("a1") || bytecode[i].equals("a2")||
                    bytecode[i].equals("a3") || bytecode[i].equals("a4")/*&&bytecode[i+1].equals("fe")
                    || bytecode[i].equals("fd")&&bytecode[i+1].equals("00")*/){
                end = i;
                break;
            }
        }

//        String[] deploydata = new String[start];
//        String[] auxdata = new String[bytecode.length-end];

        if(end==bytecode.length-1){
            System.out.println("no auxdata...");
//            System.arraycopy(bytecode,0,deploydata,0,start);
//            System.arraycopy(bytecode,end,auxdata,0,bytecode.length-end);
            String[] deploy =  ArrayUtil.sub(bytecode, 0, start+1);
            String[] rs = arrayJoin(deploy, obfuscatedBytecode);
            return rs;
        }else {
            System.out.println("have auxdata...");
            String[] deploy =  ArrayUtil.sub(bytecode, 0, start+1);
            String[] auxdata =  ArrayUtil.sub(bytecode, end,bytecode.length);

//            System.arraycopy(bytecode,0,deploydata,0,start);
//            System.arraycopy(bytecode,end,auxdata,0,bytecode.length-end);

            String[] rs = arrayJoin(deploy, obfuscatedBytecode);
            String[] rs2 = arrayJoin(rs, auxdata);
            return rs2;
        }
    }

    public static String[] arrayJoin(String[] a,String[] b){

        String[] arr=new String[a.length+b.length];
        for(int i=0;i<a.length;i++){

            arr[i]=a[i];
        }
        for(int j=0;j<b.length;j++){

            arr[a.length+j]=b[j];
        }
        return arr;
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
        System.out.println(Arrays.toString(temp));

        String[] runtimedata = byteCodeClean(temp);

        String[] all = byteCodeRecovery(temp, runtimedata);

        System.out.println(Arrays.toString(runtimedata));
        System.out.println(Arrays.toString(all));
    }

}
