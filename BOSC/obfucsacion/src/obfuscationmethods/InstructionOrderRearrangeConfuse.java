package obfuscationmethods;

import java.util.*;

/**
 * Instruction sequence rearrangement confusion technology: mainly to change the execution order of some mutually
 */
public class InstructionOrderRearrangeConfuse {

    //find independent instructions
    //Independent instructions: CODESIZE (38 to get the size of the code running in the current environment)
    // GASPRICE (3A gets the gas price in the current environment)
    // ADDRESS (30 to get the current execution account address)
    // TIMESTAMP (42 get the timestamp of the block)
    // NUMBER (43 gets the block number)
    // DIFFICULTY (44 get block difficulty)
    // GASLIMIT (45 get block gas limit)
    // CHAINID (46 gets the id of the chain)
    //And the instructions after these instructions need to have no input value type instructions, otherwise the input value as the latter instruction is not independent.
    // Randomly rearrange the order of these instructions

    public static String[] constructIndependentInstruction(){
        String[] IndependentInstruction = {"38","3A","30","42","43","44","45","46"};
        return IndependentInstruction;
    }

    public static String[] OrderRearrange(String[] bytecode,String[] IndependentInstruction){
        List<Integer> rs = new ArrayList<>();
        for (int i = 0; i < bytecode.length; i++) {
            for (int j = 0; j < IndependentInstruction.length; j++) {
                if (bytecode[i].equals(IndependentInstruction[j])){
                    //if(bytecode[i+1].equals(IndependentInstruction[j])){
                        rs.add(i);
                    //}
                }
            }
        }

        //Use Collections.shuffle to achieve out-of-order sorting
        // 2  5  6  7
        // 5  6  7  2
        // 0  1  2  3
        Collections.shuffle(rs);

        //int i = r.nextInt(rs.size());
        if (rs.size()%2==0) {
            for (int j = 0; j < rs.size(); j=j+2) {
                String temp = bytecode[rs.get(j)];
                bytecode[rs.get(j)] = bytecode[rs.get(j+1)];
                bytecode[rs.get(j+1)] = temp;
            }
        }else {
            for (int j = 0; j < rs.size()-1; j=j+2) {
                String temp = bytecode[rs.get(j)];
                bytecode[rs.get(j)] = bytecode[rs.get(j+1)];
                bytecode[rs.get(j+1)] = temp;
            }
        }
        return bytecode;
    }

    //tested
    public static void main(String[] args) {
        String[] example = {"60","46","80","38","3A","23","30","42","55","15","17"};
        String[] IndependentInstruction2 = {"38","3A","30","42","43","44","45","46"};
        System.out.println(Arrays.toString(OrderRearrange(example, IndependentInstruction2)));
    }
}
