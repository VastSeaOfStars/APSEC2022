package obfuscationmethods;

import utils.InsertIndex;

import java.util.List;

import static utils.CodeStingToArray.ToArray;
import static utils.FindJumpAndChangeBValue.ChangeBValue;
import static utils.FindJumpAndChangeBValue.findDupicateInArray;
import static utils.InsertIndex.insertIndex;
import static utils.insertElement.insertElement;

/**
 * False branch obfuscation technology: convert unconditional jumps into conditional jumps,
 */
public class FalseBranchConfuse {
    private int offset; 
    /**
     * Retrieves whether there is a jump instruction in the object bytecode
     * @param bytecode
     * @return int
     */
    public static int isExistJump(String[] bytecode){
        if(bytecode.length == 0){
            System.out.println("The bytecode is empty, please enter a bytecode！");
        }

        for (int i = 0; i < bytecode.length; i++) {
            if (bytecode[i].equals("56")){
                return i;
            }
        }
        return -1;
    }
    /**
     * @param bytecode
     * @param index jump's index
     * @return bytecode
     */
    public static String[] changeToJumpi(String[] bytecode, int index){
//        String extra = "01";
//        bytecode[index-2] = extra;

        if(index-2>=0){
            insertElement(bytecode,"6001",index-2);
        }
        bytecode[index] = "57"; //The jump is changed to jumpi,
                                // and the jumpdest value of the last push stack remains unchanged.
        insertElement(bytecode,"60006000575b",index+1+2);
        List<Integer> arrays = findDupicateInArray(bytecode, index + 9);
        for (int i = 0; i < arrays.size(); i++) {
            ChangeBValue(bytecode,i,8);
        }
        return bytecode;
    }

    /**
     * Construct true and false branch structure and insert
     * @param bytecode
     * @param insertIndex
     * @return
     */
    public static String[] InsertJumpi(String[] bytecode,int insertIndex){
        String trueAndFalseBranch = "60006000575b";
        int i = insertIndex(bytecode);
        insertElement(bytecode,trueAndFalseBranch,i);
        List<Integer> arrays = findDupicateInArray(bytecode, insertIndex + 6);
        for (int j = 0; j < arrays.size(); j++) {
            ChangeBValue(bytecode,j,6);
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
        System.out.println(isExistJump(temp));
    }
}
