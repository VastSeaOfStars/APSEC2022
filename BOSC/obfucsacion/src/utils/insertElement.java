package utils;

import java.util.Arrays;

/**
 * Insert element method into string array
 * Insert element, the following elements need to be back
 */
public class insertElement {
    /**
     * Insert element method
     * @param original
     * @param element
     * @param index
     * @return
     */
    public static String[] insertElement(String original[], String element, int index) {
        //bug: If index is -1, an array out-of-bounds exception will be reported
        int length = original.length;
        String destination[] = new String[length + 1];
        System.out.println("index:"+index);
        System.arraycopy(original, 0, destination, 0, index);
        destination[index] = element;
        System.out.println("length"+ (length-index));
        System.arraycopy(original, index, destination, index+ 1, length - index);
        return destination;
    }

    public static void main(String[] args) {
        String[] emample = {"11","60","32"};
        String[] rs = insertElement(emample,"666",1);
        System.out.println(Arrays.toString(rs));
    }
}
