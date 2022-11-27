import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;



public class VMWriter {


    LinkedList<String> vmLines;

    public VMWriter() {
        vmLines = new LinkedList<String>();
    }

    public void addVMLine(String line){
        vmLines.add(line);
    }
    public void writePush(String segment, String index){
        String line = "push " + segment + " " + index;
        this.addVMLine(line); 
    }

    public void writePop(String segment, String index){
        String line = "pop " + segment + " " + index;
        this.addVMLine(line);
    }

    public void writeArithmetic(String command){
        String line = command;
        this.addVMLine(line);
    }

    public void writeLabel(String labelName){
        String line = "label " + labelName;
        this.addVMLine(line);
    }

    public void writeGoTo(String labelname){
        String line = "goto " + labelname;
        this.addVMLine(line);
    }

    public void writeIf(String label){
        String line = "If-goto " + label; 
    }

    public void writeCall(String functionName, int nArgs){
        String line = "call " + functionName + " " + String.valueOf(nArgs);
        this.addVMLine(line);
    }

    public void writeFunction(String functionName, int nLocals){
        String line = "function " + functionName + " " + String.valueOf(nLocals);
        this.addVMLine(line);

    }

    public void writeReturn(){
        String line = "return";
        this.addVMLine(line);
    }

    public void writeLines(String name, String path) {
        String currentLine = "";
        try {
            FileWriter file = new FileWriter(path + "\\" + name + ".vm");
            while (vmLines.peekFirst() != null) {
                currentLine = vmLines.pop();
                file.write(currentLine);
                file.write("\n");
            }

            file.close();

        } catch (IOException e) {
            System.out.println("FILE IO EXCEPTION IN FILE VMWRITE FUNCTION");
            e.printStackTrace();
        }
    }
    

}
