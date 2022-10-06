package obfuscationmethods;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static utils.FindJumpAndChangeBValue.ChangeBValue;
import static utils.FindJumpAndChangeBValue.findDupicateInArray;
import static utils.InsertIndex.insertIndex;
import static utils.insertElement.insertElement;

/**
 * Incomplete instruction obfuscation technology: By inserting incomplete instructions, the decompiler will make an error
 */
public class IncompleteInstructionsConfuse {

    /**
     * Incomplete instruction
     * @return List<String>
     */
    public static List<String> ConstructIncompleteInstructions(){
        List<String> IncompleteInstructions = new ArrayList<>();
        IncompleteInstructions.add("01");
        IncompleteInstructions.add("600201");
        return IncompleteInstructions;
    }

    //2、构造无条件跳转指令，不完整指令放入其中，之后一起插入
    /**
     * Construct unconditional jump instructions, pay attention to the problem of offset (important)
     * @return unconditional jump instruction
     */
    public static String ConstructUnconditionalJump(){
//We jump over the invalid and just go to the push
//        PUSH1 4  // Offset 0
//        JUMP     // Offset 2 (previous instruction occupies 2 bytes)
//        INVALID  // Offset 3
//        JUMPDEST // Offset 4
//        PUSH1 1  // Offset 5

        return "60"+ "00" + "56" + ConstructIncompleteInstructions().get(1) +"5b";
    }

    //3、Instruction Insertion Position Method for Incomplete Instruction Obfuscation
    /**
     * Instruction Insertion
     * @param  bytecode
     * @return bytecode
     */
    public static String[] InsertIncompleteInstructions(String[] bytecode){

        int i = insertIndex(bytecode);
        insertElement(bytecode,ConstructUnconditionalJump(),i);

        List<Integer> array = findDupicateInArray(bytecode, i + 5);
        for (int i1 = 0; i1 < array.size(); i1++) {
            ChangeBValue(bytecode,i1,5);
        }
        return bytecode;
    }

    //tested
    public static void main(String[] args) {
        IncompleteInstructionsConfuse iic = new IncompleteInstructionsConfuse();
        List<String> rs = iic.ConstructIncompleteInstructions();
        System.out.println(rs);
        System.out.println(ConstructIncompleteInstructions().get(1));
    }
}
