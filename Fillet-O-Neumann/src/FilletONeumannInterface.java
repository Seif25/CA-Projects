public interface FilletONeumannInterface {

     void InstructionFetch();
     void InstructionDecode(int clock);
     void InstructionExecute(int clock);
     void MemoryReadWrite() throws Exception;
     void WriteBack();



}
