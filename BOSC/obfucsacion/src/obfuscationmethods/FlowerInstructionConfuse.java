package obfuscationmethods;

import java.util.Arrays;
import java.util.List;

import static utils.FindJumpAndChangeBValue.ChangeBValue;
import static utils.FindJumpAndChangeBValue.findDupicateInArray;
import static utils.InsertIndex.insertIndex;
import static utils.insertElement.insertElement;

/**
 * Flower instruction obfuscation: Constructing junk instructions or invalid instructions,
 * increasing the attacker's comprehension cost
 */
public class FlowerInstructionConfuse {

    public static String constructFlowerInstructions(){
        return "565B";
    }

    public static String[] InsertFlowerInstructions(String[] bytecode,String flowerInstruction){

        int index = insertIndex(bytecode);

        //String[] array = insertElement(bytecode, flowerInstruction, index);
        //int index2 = insertIndex(bytecode);
        //329ddeaffadaffafca
        //String[] array = insertElement(insertElement(bytecode, flowerInstruction, index), "6006", index2);
        String[] array = insertElement(bytecode, "6006", index);

        List<Integer> arrays = findDupicateInArray(bytecode, index + 2);

        for (int i = 0; i < arrays.size(); i++) {
            ChangeBValue(bytecode,i,8);
        }

        return array;
    }

    public static void main(String[] args) {
        String[] emample = {"11","60","32"};
        //String[] rs = insertElement(emample,"666",2);
        String[] rs = InsertFlowerInstructions(emample,constructFlowerInstructions());
        System.out.println(Arrays.toString(rs));
    }
}
