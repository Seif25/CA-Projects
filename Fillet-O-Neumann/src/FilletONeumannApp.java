import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Hashtable;

public class FilletONeumannApp implements FilletONeumannInterface {

    Hashtable<String,Integer> Registers;
    int [] Memory ;
    int PC = 0 ;
    int instructioncounter;
    boolean fetchflag;
    int decode;
    boolean decodeflag;
    int[] decodeflags;
    int execute;
    int[] executedata;
    int[] executeflags;
    boolean executeflag;
    int InsMemory;
    boolean Memoryflag;
    int[] Memorydata;
    int[] Memoryflags;
    int write;
    int[] writedata;
    int[] writeflags;
    boolean writeflag;

    public FilletONeumannApp () throws Exception {
        Memory = new int [2048];
        Registers = new Hashtable<String,Integer>();
        //zero
        fetchflag = true;
        decodeflags = new int[9];
        executeflags = new int[9];
        Memoryflags = new int[9];
        writeflags = new int[9];
        executedata = new int[7];
        Memorydata = new int[7];
        writedata = new int[7];
        for(int i = 0  ; i <=31 ; i++)
        {
            Registers.put("R"+i, 0);
        }
        parsefile();
        clockmanager();
    }

    void parsefile() throws Exception {
        String line = "";
        String splitBy = " ";
        try {
            BufferedReader br = new BufferedReader(new FileReader("src\\code\\code.txt"));
            while ((line = br.readLine()) != null) // returns a Boolean value
            {
                String[] data = line.split(splitBy);
                int Instruction;
                int oPCode;
                int R1;
                int R2;
                int R3;
                int SHAMT;
                int immediate;
                String type = "";
                switch(data[0].toUpperCase()) {
                    case("ADD") : oPCode = 0;type = "R";break;
                    case("SUB") : oPCode = 1;type = "R";break;
                    case("MULI"): oPCode = 2;type = "I";break;
                    case("ADDI"): oPCode = 3;type = "I";break;
                    case("BNE") : oPCode = 4;type = "I";break;
                    case("ANDI"): oPCode = 5;type = "I";break;
                    case("ORI") : oPCode = 6;type = "I";break;
                    case("J")   : oPCode = 7;type = "J";break;
                    case("SLL") : oPCode = 8;type = "R";break;
                    case("SRL") : oPCode = 9;type = "R";break;
                    case("LW")  : oPCode = 10;type = "I";break;
                    case("SW")  : oPCode = 11;type = "I";break;
                    default: throw new Exception("This operation ("+data[0]+") is not supported");
                }

                if(type.equals("J")) {
                    if(data.length != 2) {
                        throw new Exception("Missing or Extra Data");
                    }
                    Instruction = oPCode << 28 | Integer.parseInt(data[1]);
                }
                else if(type.equals("R")) {
                    if(data.length != 4) {
                        throw new Exception("Missing or Extra Data");
                    }
                    R1 = Integer.parseInt(data[1].substring(1));
                    if((data[1].charAt(0) != ('R')) && (data[1].charAt(0) != ('r'))) throw new Exception("Wrong input");
                    if(R1 < 0 || R1 > 31) throw new Exception("Wrong input");
                    R2 = Integer.parseInt(data[2].substring(1));
                    if((data[2].charAt(0) != ('R')) && (data[2].charAt(0) != ('r'))) throw new Exception("Wrong input");
                    if(R2 < 0 || R2 > 31) throw new Exception("Wrong input");
                    if(R1 ==0) throw new Exception("R0 cannot be destination");
                    if(oPCode != 8 && oPCode!= 9) {
                        R3 = Integer.parseInt(data[3].substring(1));
                        if((data[3].charAt(0) != ('R')) && (data[3].charAt(0) != ('r'))) throw new Exception("Wrong input");
                        if(R3 < 0 || R3 > 31) throw new Exception("Wrong input");
                        Instruction = oPCode << 28 | R1 << 23 | R2 << 18 | R3 << 13;
                    }
                    else {
                        SHAMT = Integer.parseInt(data[3]);
                        Instruction = oPCode << 28 | R1 << 23 | R2 << 18 | SHAMT;
                    }
                }
                else {
                    if(data.length != 4) {
                        throw new Exception("Missing or Extra Data");
                    }
                    R1 = Integer.parseInt(data[1].substring(1));
                    if((data[1].charAt(0) != ('R')) && (data[1].charAt(0) != ('r'))) throw new Exception("Wrong input");
                    if(R1 < 0 || R1 > 31) throw new Exception("Wrong input");
                    R2 = Integer.parseInt(data[2].substring(1));
                    if((data[2].charAt(0) != ('R')) && (data[2].charAt(0) != ('r'))) throw new Exception("Wrong input");
                    if(R2 < 0 || R2 > 31) throw new Exception("Wrong input");
                    immediate = 0b00000000000000111111111111111111  & Integer.parseInt(data[3]);
                    if(R1 ==0) throw new Exception("R0 cannot be destination");
                    Instruction = oPCode << 28 | R1 << 23 | R2 << 18 | immediate;
                }
                Memory[instructioncounter] = Instruction;
                instructioncounter++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void clockmanager() throws Exception {
        int cycle =1;
        while(true) {
            if(PC>=instructioncounter && !decodeflag && !executeflag && !Memoryflag && !writeflag) {
                System.out.print("Registers : PC = "+PC);
                for(int i=0;i<31;i++) {
                    System.out.print("  R"+i+" = "+Registers.get("R"+i));
                }
                System.out.println("  R31 = "+Registers.get("R31"));
                System.out.print("Memory : ");
                for(int i=0;i<2048;i++) {
                    System.out.print("  Memory["+i+"]="+Memory[i]);
                }
                break;
            }
            System.out.print("Cycle "+ cycle+":");
            if(writeflag) {
                System.out.print("  W");
                //System.out.print("(rd:"+writedata[0]+" rs:"+writedata[1]+" rt:"+writedata[2]+" SHAMT:"+writedata[3]+" IMM:"+writedata[4]+" address:"+writedata[5]+" ALUValue:"+writedata[6]+")" );
                WriteBack();
            }
            if(Memoryflag) {
                System.out.print("  M");
                //System.out.print("(rd:"+Memorydata[0]+" rs:"+Memorydata[1]+" rt:"+Memorydata[2]+"
                // SHAMT:"+Memorydata[3]+" IMM:"+Memorydata[4]+" address:"+Memorydata[5]+"
                // ALUValue:"+Memorydata[6]+")" );
                MemoryReadWrite();
            }
            if(executeflag) {
                System.out.print("  E");
                //System.out.print("(rd:"+executedata[0]+" rs:"+executedata[1]+" rt:"+executedata[2]+" SHAMT:"+executedata[3]+" IMM:"+executedata[4]+" address:"+executedata[5]+" ALUValue:"+executedata[6] +")");
                InstructionExecute(cycle);
            }
            if(decodeflag) {
                System.out.print("  D");
                InstructionDecode(cycle);
            }
            if(fetchflag && cycle%2 ==1)
                InstructionFetch();
            cycle++;
            System.out.println();

        }

    }

    public  void InstructionFetch() {
        if(PC<instructioncounter) {
            int x =  Memory[PC];
            System.out.print("  F");
            decode = x;
            decodeflag = true;
            fetchflag = false;
        }
        PC++;
    }

    //[RegDst , Jump , Branch , MemRead , MemtoReg , ALUOp , MemWrite , ALUSrc , RegWrite ]

    public void InstructionDecode(int clock) {
        if(clock%2 == 0) {
            int[] flag = new int [9];
            int oPCode = (decode & 0b11110000000000000000000000000000) >>28 ;
            switch(oPCode)
            {case 0: flag[0] =1 ; flag[1]=0 ;flag[2]= 0 ; flag[3] = 0 ; flag [4] = 0 ;flag[5]= 0 ; flag[6]= 0 ;flag[7]= 0 ; flag[8] =1 ;break ;
                case 1: flag[0] =1 ; flag[1]=0 ;flag[2]= 0 ; flag[3] = 0 ; flag [4] = 0 ;flag[5]= 1 ; flag[6]= 0 ;flag[7]= 0 ; flag[8] =1	 ;break ;

                case 2: flag[0] =1 ; flag[1]=0 ;flag[2]= 0 ; flag[3] = 0 ; flag [4] = 0 ;flag[5]= 2 ; flag[6]= 0 ;flag[7]= 1 ; flag[8] =1	 ;break ;
                case 3: flag[0] =1 ; flag[1]=0 ;flag[2]= 0 ; flag[3] = 0 ; flag [4] = 0 ;flag[5]= 3 ; flag[6]= 0 ;flag[7]= 1 ; flag[8] =1	 ;break ;

                case 4:	 flag[0] =-1 ; flag[1]=0 ;flag[2]= 1 ; flag[3] = 0 ; flag [4] = -1 ;flag[5]= 4 ; flag[6]= 0 ;flag[7]= 0 ; flag[8] =0	 ;break ;

                case 5: flag[0] =1 ; flag[1]=0 ;flag[2]= 0 ; flag[3] = 0 ; flag [4] = 0 ;flag[5]= 5 ; flag[6]= 0 ;flag[7]= 1 ; flag[8] =1	 ;		;break ;
                case 6: flag[0] =1 ; flag[1]=0 ;flag[2]= 0 ; flag[3] = 0 ; flag [4] = 0 ;flag[5]= 6 ; flag[6]= 0 ;flag[7]= 1 ; flag[8] =1	 ;		;break ;

                case 7:	flag[0] =-1 ; flag[1]=1 ;flag[2]= 0 ; flag[3] = 0 ; flag [4] = -1 ;flag[5]= 7 ; flag[6]= 0 ;flag[7]= -1 ; flag[8] =0 		;break ;

                case -8: flag[0] =1 ; flag[1]=0 ;flag[2]= 0 ; flag[3] = 0 ; flag [4] = 0 ;flag[5]= 8; flag[6]= 0 ;flag[7]= 0 ; flag[8] =1		;break ;
                case -7: flag[0] =1 ; flag[1]=0 ;flag[2]= 0 ; flag[3] = 0 ; flag [4] = 0 ;flag[5]= 9 ; flag[6]= 0 ;flag[7]= 0 ; flag[8] =1	;break ;
                case -6: flag[0] =0 ; flag[1]=0 ;flag[2]= 0 ; flag[3] = 1 ; flag [4] = 1 ;flag[5]= 10 ; flag[6]= 0 ;flag[7]= 1 ; flag[8] =1	;break ;
                case -5: flag[0] =-1 ; flag[1]=0 ;flag[2]= 0 ; flag[3] = 0 ; flag [4] = -1 ;flag[5]= 11 ; flag[6]= 1 ;flag[7]= 1 ; flag[8] =0		;break ;


                default : System.out.println("Instruction Cannot be executed");

            }
//		if (oPCode ==7) {//Handling jump Case
//			PC= (PC& 0b11110000000000000000000000000000) |(decode & 0b00001111111111111111111111111111) ;
//		}
//
            decodeflags = flag;
            printop(flag);
        }
        else {
            int R1 = (decode & 0b00001111100000000000000000000000)>>23;
            int R2 = (decode & 0b00000000011111000000000000000000)>>18;
            int R3 = (decode & 0b00000000000000111110000000000000)>>13;
            int SHAMT = decode & 0b00000000000000000001111111111111;
            int immediate = decode & 0b00000000000000111111111111111111;
            int address = decode & 0b00001111111111111111111111111111;
            int lfb = (immediate & 0b100000000000000000) >> 17;
            if(lfb ==1)
                immediate = immediate | 0b11111111111111000000000000000000;
            int[] data = new int[7];
            data[0] = R1;
            data[1] = R2;
            data[2] = R3;
            data[3] = immediate;
            data[4] = SHAMT;
            data[5] = address;
            executedata = data;
            printstatement();
            decodeflag = false;
            execute = decode;
            executeflag = true;
            fetchflag = true;
            executeflags = decodeflags;
        }
    }

    private void printstatement() {
        switch(decodeflags[5]) {
            case(0) : System.out.print("(add,rd= R"+executedata[0]+" ,rs= R"+executedata[1]+" ,rt= R"+executedata[2]+")");break;
            case(1) : System.out.print("(sub,rd= R"+executedata[0]+" ,rs= R"+executedata[1]+" ,rt= R"+executedata[2]+")");break;
            case(2) : System.out.print("(muli,rd= R"+executedata[0]+" ,rs= R"+executedata[1]+" ,IMM ="+executedata[3]+")");break;
            case(3) : System.out.print("(addi,rd= R"+executedata[0]+" ,rs= R"+executedata[1]+" ,IMM ="+executedata[3]+")");break;
            case(4) : System.out.print("(bne,rd= R"+executedata[0]+" ,rs= R"+executedata[1]+" ,IMM ="+executedata[3]+")");break;
            case(5) : System.out.print("(andi,rd= R"+executedata[0]+" ,rs= R"+executedata[1]+" ,IMM ="+executedata[3]+")");break;
            case(6) : System.out.print("(ori,rd= R"+executedata[0]+" ,rs= R"+executedata[1]+" ,IMM ="+executedata[3]+")");break;
            case(7) : System.out.print("(j,address ="+executedata[5]+")");break;
            case(8) : System.out.print("(sll,rd= R"+executedata[0]+" ,rs= R"+executedata[1]+" ,SHAMT ="+executedata[4]+")");break;
            case(9) : System.out.print("(srl,rd= R"+executedata[0]+" ,rs= R"+executedata[1]+" ,SHAMT ="+executedata[4]+")");break;
            case(10): System.out.print("(lw,rd= R"+executedata[0]+" ,rs= R"+executedata[1]+" ,IMM ="+executedata[3]+")");break;
            case(11): System.out.print("(sw,rd= R"+executedata[0]+" ,rs= R"+executedata[1]+" ,IMM ="+executedata[3]+")");break;
            default:break;
        }

    }

    public void InstructionExecute(int clock ) {
        if(clock%2 == 1){
            String R1Name = "R"+executedata[0];
            String R2Name = "R"+executedata[1];
            String R3Name = "R"+executedata[2];
            int immediate = executedata[3];
            int SHAMT = executedata[4];
            int address = executedata[5];
            switch(executeflags[5]){
                case(0): executedata[6] = ADD(R2Name,R3Name);break;
                case(1): executedata[6] = SUB(R2Name,R3Name);break;
                case(2): executedata[6] = MULi(R2Name,immediate);break;
                case(3): executedata[6] = ADDi(R2Name,immediate);break;
                case(4): BNE(R1Name,R2Name,immediate);break;
                case(5): executedata[6] = ANDi(R2Name,immediate);break;
                case(6): executedata[6] = ORi(R2Name,immediate);break;
                case(7): J(address);break;
                case(8): executedata[6] = SLL(R2Name,SHAMT);break;
                case(9): executedata[6] = SRL(R2Name,SHAMT);break;
                case(10): executedata[6] = ADDi(R2Name,immediate);break;
                case(11): executedata[6] = ADDi(R2Name,immediate);break;
                default:break;
            }
            printstatement2();
            Memoryflag = true;
            InsMemory = execute;
            Memoryflags = executeflags;
            Memorydata = executedata;
            executeflag = false;
        }
        else
            printop(executeflags);
    }

    private void printop(int[] flag) {
        switch(flag[5]) {
            case(0) : System.out.print("(add)");break;
            case(1) : System.out.print("(sub)");break;
            case(2) : System.out.print("(muli)");break;
            case(3) : System.out.print("(addi)");break;
            case(4) : System.out.print("(bne)");break;
            case(5) : System.out.print("(andi)");break;
            case(6) : System.out.print("(ori)");break;
            case(7) : System.out.print("(j)");break;
            case(8) : System.out.print("(sll)");break;
            case(9) : System.out.print("(srl)");break;
            case(10): System.out.print("(lw)");break;
            case(11): System.out.print("(sw)");break;
            default:break;
        }

    }

    private void printstatement2() {
        printop(executeflags);
        if(executeflags[5] != 4 && executeflags[5] != 7) {
            System.out.print("(ALUVAlue= "+executedata[6]+")");
        }

    }

    public void MemoryReadWrite() throws Exception {
        String R1Name = "R"+Memorydata[0];
        printop(Memoryflags);
        if(Memoryflags[3]==1) {
            int ALUValue = Memorydata[6];
            Memorydata[6] = Memory[ALUValue];
        }

        if(Memoryflags[6]==1) {
            if(Memorydata[6] < 1024 || Memorydata[6] >2047) throw new Exception("Address out of bound");
            Memory[Memorydata[6]] = Registers.get(R1Name);
            System.out.print(" (Memory Address "+Memorydata[6]+" updated to "+Registers.get(R1Name)+")");
        }
        Memoryflag = false;
        write = InsMemory;
        writeflag =true;
        writedata = Memorydata;
        writeflags = Memoryflags;
    }

    public void WriteBack() {
        String R1Name = "R"+writedata[0];
        printop(writeflags);
        if(writeflags[8]==1) {
            Registers.replace(R1Name,writedata[6]);
            System.out.print(" (Register "+R1Name+" updated to "+writedata[6]+")");
        }
        writeflag = false;
    }

    public int ADD(String R2, String R3) {
        int srcReg1 = Registers.get(R2);
        int srcReg2 = Registers.get(R3);
        return srcReg1+srcReg2;
    }

    public int SUB(String R2, String R3) {
        int srcReg1 = Registers.get(R2);
        int srcReg2 = Registers.get(R3);
        return srcReg1-srcReg2;
    }

    public int MULi(String R2, int IMM) {
        int srcReg = Registers.get(R2);
        return srcReg*IMM;
    }

    public int ADDi(String R2, int IMM) {
        int srcReg = Registers.get(R2);
        return srcReg+IMM;
    }

    public void BNE(String R1, String R2, int IMM) {
        int srcReg1 = Registers.get(R1);
        int srcReg2 = Registers.get(R2);
        if(srcReg1-srcReg2 !=0) {
            PC = PC+IMM-1;
            fetchflag = true;
            decodeflag = false;
        }
    }

    public int ANDi(String R2, int IMM) {
        int srcReg = Registers.get(R2);
        return srcReg&IMM;
    }

    public int ORi(String R2, int IMM) {
        int srcReg = Registers.get(R2);
        return srcReg|IMM;
    }

    public void J(int Address) {
        int PC3128 = (PC-1) & 0b11110000000000000000000000000000;
        PC = concat(PC3128,Address);
        fetchflag = true;
        decodeflag = false;
    }

    public int SLL(String R2, int SHAMT) {
        int srcReg = Registers.get(R2);
        return srcReg<<SHAMT;
    }

    public int SRL(String R2, int SHAMT) {
        int srcReg = Registers.get(R2);
        return srcReg>>>SHAMT;
    }

    public int LW(String R2, int IMM) {
        int srcReg = Registers.get(R2);
        return Memory[srcReg+IMM];
    }

    public void SW(String R1,String R2,int IMM) {
        int srcReg1 = Registers.get(R1);
        int srcReg2 = Registers.get(R2);
        Memory[srcReg2+IMM] = srcReg1;
    }

    public static int getBinaryLength(int n) {
        int length = 0;
        while (n > 0)
        {
            length += 1;
            n /= 2;
        }
        return length;
    }

    public static int concat(int m, int n) {

        // Find binary length of n
        int length = getBinaryLength(n);

        // left binary shift m and then add n
        return (m << length) + n;
    }
    public static void main(String[] args) throws Exception {
        new FilletONeumannApp();

    }
}
