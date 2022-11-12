import java.util.HashMap;
import java.util.Set;

class SymbolTable {

    String name;
    String type;
    HashMap<String, String> classType;
    HashMap<String, String> classKind;
    HashMap<String, Integer> classIndex;
    HashMap<String, String> subroutineType;
    HashMap<String, String> subroutineKind;
    HashMap<String, Integer> subroutineIndex;

    private static final Set<String> KINDS = Set.of("static", "field", "arg", "var");
    

    public static void main(String[] args) {
        System.out.println("Test.");
        return;
    }

    public SymbolTable(String name, String type) {
      classType = new HashMap<String, String>();
      classKind = new HashMap<String, String>();
      classIndex = new HashMap<String, Integer>();
      subroutineType = new HashMap<String, String>();
      subroutineKind = new HashMap<String, String>();
      subroutineIndex = new HashMap<String, Integer>();
      this.name = name;
      this.type = type;
    }

    public void startSubroutine(){
      subroutineType = new HashMap<String, String>();
      subroutineKind = new HashMap<String, String>();
      subroutineIndex = new HashMap<String, Integer>();

    }
    
    public void classDefine(String name, String type, String kind) {
      //getting index for variable
      int index = this.classVarCount(kind);
      
      classType.put(name, type);
      classKind.put(name, kind);
      classIndex.put(name, index);
      
      return;
    
    }

    public void subroutineDefine(String name, String type, String kind) {
      //getting index for variable
      int index = this.subroutineVarCount(kind);
      
      subroutineType.put(name, type);
      subroutineKind.put(name, kind);
      subroutineIndex.put(name, index);
      
      return;
    
    }
    
    //counts the number of times a kind of variable exists in the kind table
    public int classVarCount(String kind){
    
       int kindCount = 0;
   
       for (String i : this.classKind.values()){
         if (i.equals(kind)) {
            kindCount++;
         }
       }
       
       return kindCount;
     }
     public int subroutineVarCount(String kind){
    
      int kindCount = 0;
  
      for (String i : this.subroutineKind.values()){
        if (i.equals(kind)) {
           kindCount++;
        }
      }
      
      return kindCount;
    }

     public void printClassTable() {
        System.out.println("Symbol Table " + this.type + " | " + this.name);

        for (String key : this.classType.keySet()) {
          System.out.print(" |  Name:   ");
          System.out.print(key);
          System.out.print("  | Type:   ");
          System.out.print(this.classType.get(key));
          System.out.print(" |  Kind:   ");
          System.out.print(this.classKind.get(key));
          System.out.print("  |  Index:   ");
          System.out.println(this.classIndex.get(key)); 
        }
        return;
     }

     public void printSubroutineTable(String subroutineName) {
      System.out.println("Symbol Table | subroutine  " + subroutineName);

      for (String key : this.subroutineType.keySet()) {
        System.out.print(" |  Name:   ");
        System.out.print(key);
        System.out.print("  | Type:   ");
        System.out.print(this.subroutineType.get(key));
        System.out.print(" |  Kind:   ");
        System.out.print(this.subroutineKind.get(key));
        System.out.print("  |  Index:   ");
        System.out.println(this.subroutineIndex.get(key)); 
      }
      return;
   }
        
     
      
      
}

