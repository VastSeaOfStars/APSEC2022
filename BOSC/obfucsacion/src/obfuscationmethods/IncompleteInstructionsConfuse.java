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

    public static List<String> ConstructIncompleteInstructions(){
        List<String> IncompleteInstructions = new ArrayList<>();
        IncompleteInstructions.add("01");
        IncompleteInstructions.add("600201");
        return IncompleteInstructions;
    }

    public static String ConstructUnconditionalJump(){
        return "60"+ "00" + "56" + ConstructIncompleteInstructions().get(1) +"5b";
    }

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
