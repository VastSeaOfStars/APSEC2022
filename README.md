# BOSC

- **BOSC**(**B**ytecode **O**bfuscation for **S**mart **C**ontract)

- **BOSC** is a bytecode obfuscation tool for Ethereum smart contract solidity language, which uses the following four bytecode obfuscation methods:
1. **incomplete instruction obfuscation**
2. **false branch obfuscation**
3. **instruction rearrange obfuscation** 
4. **flower instruction obfuscation**

## Repo structure
- `utils`：necessary tools for obfuscation are placed under this package
  1. `ArrayToCodeString.java`: convert a string array to a string
  2. `ByteCodeCleanAndRecovry.java`: the class of bytecode cleaning(extract the runtime section code) and recovery(combine the confused runtime bytecode with the original deployment code and aux code)
  3. `ByteCodeInput.java`: input class for data, used to read bytecode files
  4. `ByteCodeOutput.java`: the output class of data, which receives the obfuscated bytecode and outputs it in the specified format
  5. `CodeStingToArray.java`: receive bytecode of type String, group every two digits into a string array for easy subsequent processing
  6. `FindJumpAndChangeBValue.java`: find all jump or jumpi values in the string array, and change the b value before all jump and jumpi values
  7. `InsertIndex.java`: method of inserting elements into a string array
  8. `insertElement.java`: Used to find suitable places to insert obfuscated code
- `obfuscationmethods`: the core logical organization of four obfuscation methods
  1. `FalseBranchConfuse.java`
  2. `FlowerInstructionConfuse.java`
  3. `IncompleteInstructionsConfuse.java`
  4. `InstructionOrderRearrangeConfuse.java`

## How to use BOSC

System: Windows10; Jdk17; 

You can download the codes, and run in any IDE such as IntelliJ IDEA. Find the Main.class, configure the input file directory, output address and all is done. Then you can use the example.hex and run with it.

**Input**: bytecode files, which is suffixed with '.hex'. Support entire bytecode or runtime bytecode.

**Output**: entire bytecode or runtime bytecode(optional).

## License

This program is issued, reproduced or used under the permission of **MIT**. Please indicate the source when using.

