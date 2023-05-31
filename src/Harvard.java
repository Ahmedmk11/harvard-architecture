import java.io.*;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

public class Harvard {
    static int[] instructionMemory = new int[1024];
    static int[] dataMemory = new int[2048];
    static int[] registers = new int[66]; // PC: registers[65] | SREG: registers[64]
    static int instructionCount;
    static int[] branchInfo = {0, 0};
    static Queue<Integer> fetchedArray = new LinkedList<>();
    static Queue<Object> decodedArray = new LinkedList<>();
    static Queue<String> printInstructions = new LinkedList<>();
    static Queue<String> printChanges = new LinkedList<>();
    public static void fetch() throws Exception {
        int fetched = instructionMemory[registers[65]];
        if (fetched == 0) {
            return;
        }
        printInstructions.add("Fetched Instruction: " + toBinary(fetched, 16));
        printInstructions.add("Fetch Inputs: " + toBinary(registers[65], 16));
        printChanges.add("Change in PC, Old Value: " + toBinary(registers[65]-1, 16) + ", New Value: " + toBinary(registers[65], 16));
        registers[65]++;
        fetchedArray.add(fetched);
    }
    public static void decode(int fetched) throws Exception {
        if (fetched == 0) {
            return;
        }
        String bin = toBinary(fetched, 16);
        String opcode = bin.substring(0, 4);
        String type;
        String rsAddress = bin.substring(4, 10);
        int rs = registers[Integer.parseInt(rsAddress, 2)];
        String rtiAddress = bin.substring(10, 16);
        int rti;
        switch (opcode) {
            case "0000", "0001", "0010", "0110", "0101", "0111" -> type = "r";
            case "0011", "0100", "1000", "1001", "1010", "1011" -> type = "i";
            default -> throw new Exception("Invalid opcode: " + opcode);
        }
        if (type.equals("r")) {
            rti = registers[Integer.parseInt(rtiAddress, 2)];
        } else {
            rti = toDecimal(rtiAddress);
        }
        if ((opcode.equals("1000") || opcode.equals("1001")) && rti < 0) {
            throw new Exception("Invalid Immediate Value (-ve)");
        }
        printInstructions.add("\nDecoded Instruction: " + toBinary(fetched, 16));
        printInstructions.add("Decode Inputs: " + bin);
        Object[] ret = new Object[6];
        ret[0] = opcode;
        ret[1] = rs;
        ret[2] = rti;
        ret[3] = rsAddress;
        ret[4] = rtiAddress;
        ret[5] = fetched;

        decodedArray.add(ret);
    }

    public static void execute(Object[] obj) throws Exception {
        if ((int) obj[5] == 0) {
            return;
        }
        String opcode = (String) obj[0];
        int rs = (int) obj[1];
        int rti = (int) obj[2];
        String rsAddress = (String) obj[3];
        String rtiAddress = (String) obj[4];
        String oldSREG = toBinary(registers[64], 8);
        switch (opcode) {
            case "0000" -> {
                // carry flag
                int un1 = rs & 0x000000FF;
                int un2 = rti & 0x000000FF;
                if (((un1 + un2) & 0x00000100) == 0x00000100) {
                    sreg('c', 1);
                } else {
                    sreg('c', 0);
                }
                // overflow flag
                if ((rs > 0 && rti > 0 && (rs + rti < 0)) || (rs < 0 && rti < 0 && (rs + rti > 0))) {
                    sreg('v', 1);
                } else {
                    sreg('v', 0);
                }
                String oldr = toBinary(rs, 8);
                rs = rs + rti;
                int r = Integer.parseInt(rsAddress, 2);
                String newr = toBinary(rs, 8);
                registers[r] = rs;
                printChanges.add("Change in R" + r + ", Old Value: " + oldr + ", New Value: " + newr);
                // negative flag
                if (rs < 0) {
                    sreg('n', 1);
                } else {
                    sreg('n', 0);
                }
                // zero flag
                if (rs == 0) {
                    sreg('z', 1);
                } else {
                    sreg('z', 0);
                }
                // sign flag
                sreg('s', ((rs < 0)^((rs > 0 && rti > 0 && (rs + rti < 0)) || (rs < 0 && rti < 0 && (rs + rti > 0)))) ? 1 : 0);
            }
            case "0001" -> {
                // overflow flag
                if ((rs > 0 && rti < 0 && (rs - rti < 0)) || (rs < 0 && rti > 0 && (rs - rti > 0))) {
                    sreg('v', 1);
                } else {
                    sreg('v', 0);
                }
                int r = Integer.parseInt(rsAddress, 2);
                String oldr = toBinary(rs, 8);
                rs = rs - rti;
                registers[Integer.parseInt(rsAddress, 2)] = rs;
                String newr = toBinary(rs, 8);
                printChanges.add("Change in R" + r + ", Old Value: " + oldr + ", New Value: " + newr);
                // negative flag
                if (rs < 0) {
                    sreg('n', 1);
                } else {
                    sreg('n', 0);
                }
                // zero flag
                if (rs == 0) {
                    sreg('z', 1);
                } else {
                    sreg('z', 0);
                }
                // sign flag
                sreg('s', ((rs > 0 && rti < 0 && (rs - rti < 0)) || (rs < 0 && rti > 0 && (rs - rti > 0))) ? 1 : 0);
            }
            case "0010" -> {
                int r = Integer.parseInt(rsAddress, 2);
                String oldr = toBinary(rs, 8);
                rs = rs * rti;
                registers[Integer.parseInt(rsAddress, 2)] = rs;
                String newr = toBinary(rs, 8);
                printChanges.add("Change in R" + r + ", Old Value: " + oldr + ", New Value: " + newr);
                // negative flag
                if (rs < 0) {
                    sreg('n', 1);
                } else {
                    sreg('n', 0);
                }
                // zero flag
                if (rs == 0) {
                    sreg('z', 1);
                } else {
                    sreg('z', 0);
                }
            }
            case "0011" -> {
                int r = Integer.parseInt(rsAddress, 2);
                String oldr = toBinary(rs, 8);
                registers[r] = toDecimal(rtiAddress);
                printChanges.add("Change in R" + r + ", Old Value: " + oldr + ", New Value: " + toBinary(toDecimal(rtiAddress), 8));
            }
            case "0100" -> {
                if (rs == 0) {
                    if (toDecimal(rtiAddress) != 0) {
                        branchInfo[0] = 1;
                    }
                    String oldr = toBinary(registers[65], 16);
                    registers[65] = registers[65] + toDecimal(rtiAddress); // + 1 in fetch()
                    String newr = toBinary(registers[65], 16);
                    printChanges.add("Change in PC, Old Value: " + oldr + ", New Value: " + newr);
                }
            }
            case "0101" -> {
                int r = Integer.parseInt(rsAddress, 2);
                String oldr = toBinary(rs, 8);
                rs = rs & rti;
                registers[Integer.parseInt(rsAddress, 2)] = rs;
                String newr = toBinary(rs, 8);
                printChanges.add("Change in R" + r + ", Old Value: " + oldr + ", New Value: " + newr);
                // negative flag
                if (rs < 0) {
                    sreg('n', 1);
                } else {
                    sreg('n', 0);
                }
                // zero flag
                if (rs == 0) {
                    sreg('z', 1);
                } else {
                    sreg('z', 0);
                }
            }
            case "0110" -> {
                int r = Integer.parseInt(rsAddress, 2);
                String oldr = toBinary(rs, 8);
                rs = rs | rti;
                registers[Integer.parseInt(rsAddress, 2)] = rs;
                String newr = toBinary(rs, 8);
                printChanges.add("Change in R" + r + ", Old Value: " + oldr + ", New Value: " + newr);
                // negative flag
                if (rs < 0) {
                    sreg('n', 1);
                } else {
                    sreg('n', 0);
                }
                // zero flag
                if (rs == 0) {
                    sreg('z', 1);
                } else {
                    sreg('z', 0);
                }
            }
            case "0111" -> {
                String tmp = toBinary(rs, 8) + toBinary(rti, 8);
                branchInfo[1] = 1;
                if (toDecimal(tmp) > 1023) {
                    throw new Exception("Instruction Memory Length Exceeded");
                }
                String oldr = toBinary(registers[65], 16);
                registers[65] = toDecimal(tmp);
                String newr = toBinary(registers[65], 16);
                printChanges.add("Change in PC, Old Value: " + oldr + ", New Value: " + newr);
            }
            case "1000" -> {
                int r = Integer.parseInt(rsAddress, 2);
                String oldr = toBinary(rs, 8);
                rs = (rs <<  toDecimal(rtiAddress)) | (rs >>> (8 -  toDecimal(rtiAddress)));
                registers[r] = rs;
                String newr = toBinary(rs, 8);
                printChanges.add("Change in R" + r + ", Old Value: " + oldr + ", New Value: " + newr);
                // negative flag
                if (rs < 0) {
                    sreg('n', 1);
                } else {
                    sreg('n', 0);
                }
                // zero flag
                if (rs == 0) {
                    sreg('z', 1);
                } else {
                    sreg('z', 0);
                }
            }
            case "1001" -> {
                int r = Integer.parseInt(rsAddress, 2);
                String oldr = toBinary(rs, 8);
                rs = (rs >>>  toDecimal(rtiAddress)) | (rs << (8 -  toDecimal(rtiAddress)));
                registers[Integer.parseInt(rsAddress, 2)] = rs;
                String newr = toBinary(rs, 8);
                printChanges.add("Change in R" + r + ", Old Value: " + oldr + ", New Value: " + newr);
                // negative flag
                if (rs < 0) {
                    sreg('n', 1);
                } else {
                    sreg('n', 0);
                }
                // zero flag
                if (rs == 0) {
                    sreg('z', 1);
                } else {
                    sreg('z', 0);
                }
            }
            case "1010" -> {
                int r = Integer.parseInt(rsAddress, 2);
                String oldr = toBinary(rs, 8);
                rs = dataMemory[rti];
                registers[Integer.parseInt(rsAddress, 2)] = rs;
                String newr = toBinary(rs, 8);
                printChanges.add("Change in R" + r + ", Old Value: " + oldr + ", New Value: " + newr);
            }
            case "1011" -> {
                String oldm = toBinary(dataMemory[rti], 8);
                dataMemory[rti] = rs;
                String newm = toBinary(dataMemory[rti], 8);
                printChanges.add("Change in Data Memory at Address " + toBinary(rti, 11) + ", Old Value: " + oldm + ", New Value: " + newm);
            }
            default -> throw new Exception("Invalid Instruction");
        }
        String[] stringArray = new String[obj.length];
        for (int i = 0; i < obj.length; i++) {
            stringArray[i] = String.valueOf(obj[i]);
        }
        printInstructions.add("\nExecuted Instruction: " + toBinary((int) obj[5], 16));
        printInstructions.add("Execute Inputs:");
        printInstructions.add("Opcode (Base 2): " + stringArray[0]);
        printInstructions.add("Register 1 Address (Base 10): " + stringArray[1]);
        printInstructions.add("Register 2 Address/Value (Base 10): " + stringArray[2]);
        printInstructions.add("Register 1 Address (Base 2): " + stringArray[3]);
        printInstructions.add("Register 2 Address/Value (Base 2): " + stringArray[4]);
        printInstructions.add("Instruction Executed (Base 2): " + toBinary(Integer.parseInt(stringArray[5]), 16));
        String newSREG = toBinary(registers[64], 8);
        if (toDecimal(newSREG) != toDecimal(oldSREG)){
            printChanges.add("Change in SREG, Old Value: " + oldSREG + ", New Value: " + newSREG);

        }
    }
    public static void replaceCharAtIndex(int index, int newChar) {
        char[] charArray = toBinary(registers[64], 8).toCharArray();
        if (newChar != 1) {
            newChar = 0;
        }
        charArray[index] = (char) (newChar + '0');
        registers[64] = toDecimal(new String(charArray));
    }

    public static void sreg(char flag, int value) throws Exception {
        switch (flag) {
            case 'c' -> replaceCharAtIndex(3, value);
            case 'v' -> replaceCharAtIndex(4, value);
            case 'n' -> replaceCharAtIndex(5, value);
            case 's' -> replaceCharAtIndex(6, value);
            case 'z' -> replaceCharAtIndex(7, value);
            default -> throw new Exception("Invalid parameter");
        }
    }
    public static String[] readLines(String filename) throws IOException {
        FileReader fileReader = new FileReader(filename);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        ArrayList<String> lines = new ArrayList<>();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            int index = line.indexOf(";");
            if (index >= 0) {
                line = line.substring(0, index);
            }
            line = line.trim();
            if (!line.isEmpty()) {
                lines.add(line);
            }
        }
        bufferedReader.close();
        return lines.toArray(new String[0]);
    }
    public static void parseInstruction(String s, int index) throws Exception {
        String[] arr = s.split(" "); // [ADD, R1, R2]
        String instruction = arr[0];
        String opcodeStr;
        boolean flag = false;
        boolean branch = false;
        switch (instruction) {
            case "ADD" -> opcodeStr = "0000";
            case "SUB" -> opcodeStr = "0001";
            case "MUL" -> opcodeStr = "0010";
            case "LDI" -> {
                opcodeStr = "0011";
                flag = true;
            }
            case "BEQZ" -> {
                opcodeStr = "0100";
                flag = true;
                branch = true;
            }
            case "AND" -> opcodeStr = "0101";
            case "OR" -> opcodeStr = "0110";
            case "JR" -> opcodeStr = "0111";
            case "SLC" -> {
                opcodeStr = "1000";
                flag = true;
            }
            case "SRC" -> {
                opcodeStr = "1001";
                flag = true;
            }
            case "LB" -> {
                opcodeStr = "1010";
                flag = true;
            }
            case "SB" -> {
                opcodeStr = "1011";
                flag = true;
            }
            default -> throw new Exception("Invalid Instruction");
        }
        if (!arr[1].matches("^R([1-6]\\d|[0-9])$")) {
            if (arr[1].equals("R64")) {
                throw new Exception("Cannot Modify SREG");
            } else if (arr[1].equals("R65")) {
                throw new Exception("Cannot Modify PC");
            } else {
                throw new Exception("Invalid Register for Instruction " + instruction + " at line " + (index + 1));
            }
        }
        if (flag) {
            if (!arr[2].matches("^-?[0-9]+$")) {
                throw new Exception("Invalid input for I-Type Instruction " + instruction + " at line " + (index + 1));
            }
            if (Integer.parseInt(arr[2]) > 31 || Integer.parseInt(arr[2]) < -32) {
                System.out.println("hi"+arr[2]);
//                throw new Exception("Only use Immediate between -32 to 31");
            }
        }
        if (branch && Integer.parseInt(arr[2]) < 0){
            throw new Exception("Incorrect Branch Offset " + "at line " + (index + 1));
        }
        String r1 = toBinary(Integer.parseInt(arr[1].replaceAll("R", "")), 6);
        String r2 = toBinary(Integer.parseInt(arr[2].replaceAll("R", "")), 6);
        String s1 = opcodeStr + r1 + r2;
        instructionMemory[index] = Integer.parseInt(s1, 2);
    }
    public static void populateInstructionMemory() throws Exception {
        String[] lines = readLines("/Volumes/BASMA/CA/Project/Test Files/Package 2/Program.txt");
        if (lines.length > 1024) {
            throw new Exception("Instruction Memory Exceeded");
        }
        for (int i = 0; i < lines.length; i++) {
            parseInstruction(lines[i], i);
        }
        instructionCount = lines.length;
    }
    public static int toDecimal(String binaryString) {
        int numBits = binaryString.length();
        boolean isNegative = (binaryString.charAt(0) == '1');
        if (isNegative) {
            StringBuilder invertedString = new StringBuilder(binaryString);
            for (int i = 0; i < numBits; i++) {
                char bit = binaryString.charAt(i);
                invertedString.setCharAt(i, (bit == '0') ? '1' : '0');
            }
            for (int i = numBits - 1; i >= 0; i--) {
                if (invertedString.charAt(i) == '0') {
                    invertedString.setCharAt(i, '1');
                    break;
                } else {
                    invertedString.setCharAt(i, '0');
                }
            }
            binaryString = invertedString.toString();
        }
        int magnitude = 0;
        for (int i = 1; i < numBits; i++) {
            char bit = binaryString.charAt(i);
            if (bit == '1') {
                magnitude += Math.pow(2, numBits - i - 1);
            }
        }
        return isNegative ? -magnitude : magnitude;
    }
    public static String toBinary(int num, int numBits) {
        StringBuilder binaryString = new StringBuilder(Integer.toBinaryString(Math.abs(num)));
        int numMagnitudeBits = binaryString.length();
        if (num < 0) {
            for (int i = 0; i < numMagnitudeBits; i++) {
                char bit = binaryString.charAt(i);
                binaryString.setCharAt(i, (bit == '0') ? '1' : '0');
            }
            for (int i = numMagnitudeBits - 1; i >= 0; i--) {
                if (binaryString.charAt(i) == '0') {
                    binaryString.setCharAt(i, '1');
                    break;
                } else {
                    binaryString.setCharAt(i, '0');
                }
            }
        }
        char paddingChar = (num < 0) ? '1' : '0';
        while (binaryString.length() < numBits) {
            binaryString.insert(0, paddingChar);
        }

        while (binaryString.length() > numBits) {
            binaryString.deleteCharAt(0);
        }
        return binaryString.toString();
    }
    public static void printInfo(int clockCycle) {
        if (clockCycle == 1) {
            System.out.println("\n==============================================================================");
            System.out.println("Start of Program");
            System.out.println("==============================================================================");
        }
        System.out.println("\n\nClock Cycle " + clockCycle);
        System.out.println("\n\n==========================");
        System.out.println("Pipelining Stages");
        System.out.println("==========================\n\n");

        while(!printInstructions.isEmpty()) {
            System.out.println(printInstructions.remove());
        }

        System.out.println("\n\n==========================");
        System.out.println("Changes to Registers");
        System.out.println("==========================\n\n");

        if (printChanges.isEmpty()) {
            System.out.println("No Changes");
        } else {
            while(!printChanges.isEmpty()) {
                System.out.println(printChanges.remove());
            }
        }
    }
    public static void pipeline() throws Exception {
        int clk = 0;
        boolean firstLoop = true;
        for (int clockCycle = 1; true; clockCycle++) {
            clk++;
            if ((branchInfo[0] == 1 || branchInfo[1] == 1) && firstLoop) {
                firstLoop = false;
                clk = 1;
                if (branchInfo[0] == 1) {
                    registers[65] -= (fetchedArray.size() + decodedArray.size());
                    branchInfo[0] = 0;
                    firstLoop = true;
                } else if(branchInfo[1] == 1) {
                    branchInfo[1] = 0;
                    firstLoop = true;
                }
                fetchedArray.clear();
                decodedArray.clear();
            }
            if (clk == 1) {
                fetch();
            } else if (clk == 2) {
                fetch();
                if (!fetchedArray.isEmpty()) {
                    decode(fetchedArray.remove());
                }
            } else if (clk == instructionCount +1) {
                if (!fetchedArray.isEmpty()) {
                    decode(fetchedArray.remove());
                }
                if (!decodedArray.isEmpty()) {
                    execute((Object[]) decodedArray.remove());

                }
            } else if (clk == instructionCount + 2){

                if (!decodedArray.isEmpty()) {
                    execute((Object[]) decodedArray.remove());
                }
            } else {
                fetch();
                if (!fetchedArray.isEmpty()) {
                    decode(fetchedArray.remove());
                }
                if (!decodedArray.isEmpty()) {
                    execute((Object[]) decodedArray.remove());
                }
            }
            printInfo(clockCycle);
            if (fetchedArray.isEmpty() && decodedArray.isEmpty() ) {
                System.out.println("\n\n==============================================================================");
                System.out.println("End of Program");
                System.out.println("==============================================================================");
                System.out.println("\n\n==========================");
                System.out.println("Registers");
                System.out.println("==========================\n\n");
                for (int i = 0; i < 66; i++) {
                    String pre;
                    if (i < 64) {
                        pre = "R" + i;
                    } else if (i == 64) {
                        pre = "SREG";
                    } else {
                        pre = "PC";
                    }
                    System.out.println(pre + ": " + registers[i]);
                }
                System.out.println("\n\n==========================");
                System.out.println("Data Memory");
                System.out.println("==========================\n\n");
                for (int i = 0; i < 2048; i++) {
                    System.out.println(i + ": " + dataMemory[i]);
                }
                System.out.println("\n\n==========================");
                System.out.println("Instruction Memory");
                System.out.println("==========================\n\n");
                for (int i = 0; i < 1024; i++) {
                    System.out.println(i + ": " + instructionMemory[i]);
                }
                break;
            }
            //printInfo(clockCycle);
        }
    }
    public static void main(String[] args) throws Exception {
        File file = new File("/Users/ahmedmahmoud/Desktop/Uni/Semester 6/Computer Sys. Architecture/harvard-architecture/harvard-architecture/src/output.txt");
        PrintStream stream = new PrintStream(file);
        System.setOut(stream);

        populateInstructionMemory();
        pipeline();
    }
}
