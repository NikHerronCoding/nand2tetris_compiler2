
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.HashSet;
import java.util.HashMap;

public class CompilationEngine {

    public JackTokenizer tokenizer;
    public SymbolTable classTable;
    public VMWriter vmWriter;
    public int numIfs;
    public int numWhiles;


    public CompilationEngine(JackTokenizer jackTokenizer) {
        this.tokenizer = jackTokenizer;
        vmWriter = new VMWriter();
        numIfs = 0;
        numWhiles = 0;
    }

    public static String symbolTag(String token) {
        if (token.equals("<")) {
            return "&lt;";
        } else if (token.equals(">")) {
            return "&gt;";
        } else if (token.equals("&")) {
            return "&amp;";
        } else {
            return token;
        }
    }

    //class desc consists of class tag followed by className followed by "{" followed by classVarDesc* followed by subroutineDec* followed by "}" followed class close tag
    public void compileClass() {
        String currentToken;

        //keyword class tag
        currentToken = tokenizer.advance();

        //writing className which is an identifier
        currentToken = tokenizer.advance();

        //creating symbol table with type class
        this.classTable = new SymbolTable(currentToken, "CLASS");

        //writing  '{' symbol and associated xml tags
        currentToken = tokenizer.advance();

        this.compileclassVarDesc();
        String nextToken = tokenizer.tokens.peekFirst();
        while (!nextToken.equals("}")) {
            this.compileSubroutine();
            nextToken = tokenizer.tokens.peekFirst();
        }

        //getting  '}' symbol and associated xml tags
        currentToken = tokenizer.advance();
    
        //adding class closing tag, eof after this
        return;
    }

    public void compileclassVarDesc() {
        //gets next token
        String currentToken = tokenizer.advance();
        String kind, type, name, varIndex;

        //continues while there are further class variable to declare
        while (currentToken.equals("static") || currentToken.equals("field")) {

            //determines variable kind for adding var to symbol table
            kind = currentToken;


            //adds type - here type is an identifier
            currentToken = tokenizer.advance();

            //gets variable type for symbol table
            type = currentToken;

            //adds varName - there could be many varNames of a certain type - loops here till all are added
            while (!currentToken.equals(";")) {
                currentToken = tokenizer.advance();
                name = currentToken;
                classTable.classDefine(name, type, kind);
                varIndex = classTable.classIndex.get(name).toString();
                
                
                //if token is comma, dump and get another otherwise keep going
                currentToken = tokenizer.advance();
                if (currentToken.equals(",")) {
                }
            }

            currentToken = tokenizer.advance();
        }
        tokenizer.tokens.addFirst(currentToken);
        return;
    }

    public void compileSubroutine() {

        String className = classTable.name;
        String functionName = "";
        String vmFunctionName;

        //clears symbol table at subroutine level
        classTable.startSubroutine();
        numIfs = 0;
        numWhiles = 0;

        //gets function type and adds it to tags (can be staic or method)
        String subroutineType = tokenizer.advance();
        String classKind = classTable.name;

        //add THIS to arg 0 if method or constructor
        if (subroutineType.equals("method")){
            classTable.subroutineDefine("this", classKind,"argument");
        }

        //gets return type and addds it to tags  - can be int, boolean, char or className
        String functionReturnType = tokenizer.advance();

        //gets function name and adds it to tags must be identifier so ez
        functionName = tokenizer.advance();
        vmFunctionName = className + "." + functionName;

        //gets ( symbol and adds it to tags
        String symbol = tokenizer.advance();

        //involves compilation of the arguments in function
        this.compileParameterList();
        
        // gets ) symbol and adds it to tags
        symbol = tokenizer.advance();
 
        this.compileSubroutineBody(vmFunctionName, subroutineType);

    }

    public void compileParameterList() {


        //declaring variable(s) to store var in symbol table
        String kind = "argument";

        String nextToken = tokenizer.tokens.peekFirst();
        String parameterType = "";
        String parameterName = "";
        String varIndex;

        //while parameters exist
        while (!nextToken.equals(")")) {

            // gets parameter type and name 
            parameterType = tokenizer.advance();
 
            parameterName = tokenizer.advance();


            //adds parameter to subroutine define as argument list
            classTable.subroutineDefine(parameterName, parameterType, kind);
            varIndex = classTable.subroutineIndex.get(parameterName).toString();

            //gets next token comma indicates keep going, also gotta add , to 
            nextToken = tokenizer.tokens.peekFirst();
            if (nextToken != null && nextToken.equals(",")) {
                nextToken = tokenizer.advance();
            }
            
        }
    }

    public void compileSubroutineBody(String vmFunctionName, String subroutineType) {
        int numLocalVars = 0;

        // adding open curly bracket to tags
        String symbol = tokenizer.advance();

        String currentToken = tokenizer.tokens.peekFirst();
        
        while (!currentToken.equals("}")) {
            if (currentToken.equals("var")) {
                this.compileVarDec();    
            } else {
                //writing vm function declaration code here becuase we finally know how many local variables exist!
                numLocalVars = classTable.subroutineVarCount("local"); //need to know how many local vars there are
                vmWriter.writeFunction(vmFunctionName, numLocalVars);
                    //compiling subroutine body. If subroutine is a method, then the first thing done need to be pushing arg 0 to pointer 0

                    if (subroutineType.equals("method")){
                        vmWriter.writePush("argument", "0");
                        vmWriter.writePop("pointer", "0");
                    } 

                //adding vm memory allocation step for object if constructor
                int numFields;

                if (subroutineType.equals("constructor")){

                    //each field has takes up single address space unit this find out how much memory to allocate
                    numFields = classTable.classVarCount("field");

                    //writing the vm code to allocate the memory
                    vmWriter.writePush("constant", Integer.toString(numFields));
                    vmWriter.writeCall("Memory.alloc", 1);

                    //adds allocated memory address to this pointer (pointer 0)
                    vmWriter.writePop("pointer", "0");

                }


                //compiling rest of statements within the subroutine
                this.compileStatements();
                break;
            }
            currentToken = tokenizer.tokens.peekFirst();
        }

        // gets } symbol 
        symbol = tokenizer.advance();
    }
    //returns the num of local
    public void compileVarDec() {
        String kind = "local";
        String symbol = "";
        String varIndex;
        
        //adding  var tag
        String var = tokenizer.advance();

        //adding type tag
        String type = tokenizer.advance();

            while (!symbol.equals(";")) {

                //getting varname 
                String varName = tokenizer.advance();

                //adding variable to symbol table with subroutine scope
                classTable.subroutineDefine(varName, type, kind);
                varIndex = classTable.subroutineIndex.get(varName).toString();

                
                //adding variable to symbol table with subroutine scope
                //classTable.subroutineDefine(varName, type, kind);

                //getting next token, either will be comma or semicolon
                symbol = tokenizer.tokens.peekFirst();
                if (symbol.equals(",")) {
                    symbol = tokenizer.advance();
                }
        }

        //adding closing ;
        symbol = tokenizer.advance();
        
        return;
    }


    public void compileStatements() {
        //output opening statemnts tag

        //determining statement type and determining what to compile
        while (true) {
            String type = tokenizer.tokens.peekFirst();
            if (type.equals("let")) {
                this.compileLetStatement();
            } else if (type.equals("if")) {
                this.compileIfStatement();
            } else if (type.equals("while")) {
                this.compileWhileStatement();
            } else if (type.equals("do")) {
                this.compileDoStatement();
            } else if (type.equals("return")) {
                this.compileReturnStatement();
            } else {
                break;
            }
        }
    }

    public void compileIfStatement() {
        
        int currentIfNumber = numIfs;


        //incrementing number of if statments for tracking must increment here in case there are nested if statements
        numIfs++;

        String truePhrase, falsePhrase, endPhrase;
        String currentToken = tokenizer.advance();

        //gets the open parenthese 
        String symbol = tokenizer.advance();

        //compiles the expression to be evaluated
        this.compileExpression();
        

        //first line of vmcode goes here - 
        truePhrase = "IF_TRUE" + Integer.toString(currentIfNumber);
        vmWriter.writeIf(truePhrase);

        //creating "if_false" jump
        falsePhrase = "IF_FALSE" + Integer.toString(currentIfNumber);
        vmWriter.writeGoTo(falsePhrase);


        //gets the close parenthese
        symbol = tokenizer.advance();


        //gets the open curly bracket
        symbol = tokenizer.advance();

        //adding tag for vm code if true
        vmWriter.writeLabel(truePhrase);

        //this compiles the statements within the if statement -recursive
        this.compileStatements();

        //gets the close curly bracket
        symbol = tokenizer.advance();

        //determines if the there is an "else" statement in addition to the 'if'
        currentToken = tokenizer.tokens.peekFirst();

        if (currentToken.equals("else")) {

            endPhrase = "IF_END" + Integer.toString(currentIfNumber);


            //if else exists then need to go to if end if condition is true
            vmWriter.writeGoTo(endPhrase);

            //writing vmcode for "if_false" 
            vmWriter.writeLabel(falsePhrase);

            //getting keyword else 
            currentToken = tokenizer.advance();
            
            //getting opening bracket
            symbol = tokenizer.advance();

            //adding statements to else block
            this.compileStatements();

            //getting closing bracket 
            symbol = tokenizer.advance();

        //adding vm code for endif - only needed if there is an else statement
        vmWriter.writeLabel(endPhrase);
        
        } else{ 

            //writing vmcode for "if_false" 
            vmWriter.writeLabel(falsePhrase);

        }
    }

    public void compileWhileStatement() {

        //getting # of while statement
        int currentWhile = numWhiles;
        numWhiles++;

        String labelPhrase = "WHILE_EXP" + Integer.toString(currentWhile);
        String endPhrase = "WHILE_END" + Integer.toString(currentWhile);

        //adding while label to signify start if while loop
        vmWriter.writeLabel(labelPhrase);

        //adding while token as a keyword
        String currentToken = tokenizer.advance();

        //adding open parenthese 
        String symbol = tokenizer.advance();

        //compiling expression
        this.compileExpression();

        /*writing VM code with the following ruls for compilation:
        1. Once the while condition is evaluated the boolean expression is inverted. This is becuase
        the code required forces a branch if the condition is not met rather than is met.*/
        vmWriter.writeArithmetic("not");

        /* 2. "if-goto" branch is  made here to exit while loop if condition is not met*/
        vmWriter.writeIf(endPhrase);

        //adding close parenthese 
        symbol = tokenizer.advance();

        //adding open curly brace
        symbol = tokenizer.advance();

        //compiling statements
        this.compileStatements();

        //adding closed curly brace
        symbol = tokenizer.advance();

        //adding vm code to retest condition for while loop
        vmWriter.writeGoTo(labelPhrase);
        vmWriter.writeLabel(endPhrase);
    }

    public void compileDoStatement() {

        //adding 'do' keyword to tags
        String nextToken = tokenizer.advance();      

        //compiles subroutineCall
        this.compileSubroutineCall();

        //add semicolon to end of statment
        String symbol = tokenizer.advance();   

        //adding line to discard any value that is returned form the "DO statement"
        vmWriter.writePop("temp", "0");
    }

    public void compileReturnStatement() {

        //adding return keyword to tags
        String nextToken = tokenizer.advance(); 
        
        //seeing if we need to return a value or just return
        String currentToken = tokenizer.tokens.peekFirst();

        if (!currentToken.equals(";")) {
            this.compileExpression();
        } else{
            vmWriter.writePush("constant", "0");
        }
        //this should add the end statement semi colon to the tags
        currentToken = tokenizer.advance();

        //vm code here
        vmWriter.writeReturn();
    }

    public void compileLetStatement() {
        boolean isArray = false;
        String varName, varType, varKind, varIndex, phrase;


        //adding let keyword tag
        String keyword = tokenizer.advance();

        //adding varName identifier tag
        varName = tokenizer.advance();


        //getting var type kind and index, searches subroutine scope first then class level

        if (classTable.subroutineType.containsKey(varName)) {
            varType = classTable.subroutineType.get(varName);
            varKind = classTable.subroutineKind.get(varName);
            varIndex = classTable.subroutineIndex.get(varName).toString();
        } else {
            varType = classTable.classType.get(varName);
            varKind = classTable.classKind.get(varName);
            //fields aka kinds are referenced in the  pointer memory segment with vm
            if (varKind.equals("field")){
                varKind = "this";
            }
            varIndex = classTable.classIndex.get(varName).toString();
        }
        
        // //adding possible symbol [
        String symbol = tokenizer.tokens.peekFirst();
        if (symbol.equals("[")) {
            isArray = true;
            symbol = tokenizer.advance();
            // if [  exists then an expression within it exists
            this.compileExpression();

            //adding closing ]
            symbol = tokenizer.advance();

            //vm code essentially finding base address of element - first push occurs when above expression is compiled
            vmWriter.writePush(varKind, varIndex);
            vmWriter.writeArithmetic("add");
        }

        //adding equals sign
        symbol = tokenizer.advance();

        //compiling expression 
        this.compileExpression();

        if (isArray){
            vmWriter.writePop("temp", "0");
            vmWriter.writePop("pointer", "1");
            vmWriter.writePush("temp", "0");
            vmWriter.writePop("that", "0");
        } else {
        //VM CODE HERE - IF NO ARRAY POPS VALUE TO DESIRED MEMORY SEGMENT LOCATION
        vmWriter.writePop(varKind, varIndex);

        }



        //adding semi colon to signify end of statement
        symbol = tokenizer.advance();

        return;
    }

    public void compileExpression() {
        HashSet<String> operators = new HashSet<String>();
        HashMap<String, String> operatorMap = new HashMap<String,String>();
        String operator, vmTerm;
        operators.add("+");operators.add("-");operators.add("*");
        operators.add("/");operators.add("&");operators.add("|");
        operators.add("<");operators.add(">");operators.add("=");

        operatorMap.put("+", "add");
        operatorMap.put("-", "sub");
        operatorMap.put("*", "call Math.multiply 2");
        operatorMap.put("/", "call Math.divide 2");
        operatorMap.put(">", "gt");
        operatorMap.put("<", "lt");
        operatorMap.put("=", "eq");
        operatorMap.put("&", "and");
        operatorMap.put("|", "or");


        this.compileTerm();

        while (operators.contains(tokenizer.tokens.peekFirst())) {
            operator = tokenizer.advance();


            //continue compilation of next term (if operator exists another term must exist)
            this.compileTerm();

            //adding vm code for operator here - converstion from infix to postfix so vm arithmetic is after term compilation
            vmTerm = operatorMap.get(operator);
            vmWriter.writeArithmetic(vmTerm);
        }

    }

    public void compileTerm() {
        String type = this.determineType();

        switch (type) {
            case "integerConstant": 
                this.compileIntegerConstant();
                break;
            case "stringConstant":
                this.compileStringConstant();
                break;
            case "keywordConstant":
                this.compileKeywordConstant();
                break;
            case "subroutineCall":
                this.compileSubroutineCall();
                break;
            case "varName":
                this.compileVarName();
                break;
            case "parenthetical":
                this.compileParenthetical();
                break;
            case "urnaryOP":
                this.compileUrnaryOP();
                break;
            default:
                break;
        }
    }

    public void compileIntegerConstant() {
        String integerConstant = tokenizer.advance();

        //vm code added here
        vmWriter.writePush("constant", integerConstant);
    }

    public void compileStringConstant() {
        String currentToken = tokenizer.advance();
        String phrase = currentToken.substring(1,currentToken.length()-1);
        int length = phrase.length();
        char value;
        int intValue;

        vmWriter.writePush("constant", Integer.toString(length));
        vmWriter.writeCall("String.new", 1);

        //vm code here, iterate through string and turn char to int value then push to stack
        for (int i = 0; i < phrase.length(); i++){
            value = phrase.charAt(i); 
            intValue = (int) value;
            vmWriter.writePush("constant" , Integer.toString(intValue));
            vmWriter.writeCall("String.appendChar", 2);
        }
    }


    public void compileKeywordConstant() {
        String keyWord = tokenizer.advance();

        //vm code generated:

        if (keyWord.equals("true")){
            vmWriter.writePush("constant", "0");
            vmWriter.writeArithmetic("not");
        } else if (keyWord.equals("false")){
            vmWriter.writePush("constant", "0");
        } else if (keyWord.equals("this")) {
            vmWriter.writePush("pointer", "0");
        } else if (keyWord.equals("null")){
            vmWriter.writePush("constant", "0");
        }
    }

    public void compileSubroutineCall() {

        int numberOfArgs = 0;
        String className = "";
        String varSegment = "";
        String varIndex = "";
        String functionName = "";
        String classAndFunctionName = "";
        String objectName = "";
        boolean isMethod = false;
        
        // should print identifier term + identifier tags (either class name or subroutine name)
        functionName = tokenizer.advance();
        
        if (tokenizer.tokens.peekFirst().equals("(")) {
            //if there is no dot after first identidier then the method is of the current class "this" needs to be added to stack as first var
            className = classTable.name;
            tokenizer.advance();
             //this tag should be open parenthese
            vmWriter.writePush("pointer", "0");
            numberOfArgs = this.compileExpressionList() + 1;
            tokenizer.advance();
            //should print closed parenthese
        }

        if (tokenizer.tokens.peekFirst().equals(".")) {

            /*If there is a function call involding a dot, then it can either be a static / method / constructor subroutine.
            if it is a method - class name is there, if it is a static, must determine type that the caller is 
             */

             if (classTable.subroutineType.containsKey(functionName)){
                objectName = functionName;
                className = classTable.subroutineType.get(functionName);
                isMethod = true;
                varSegment = classTable.subroutineKind.get(objectName);
                varIndex = Integer.toString(classTable.subroutineIndex.get(objectName));
             } else if (classTable.classType.containsKey(functionName)) {
                objectName = functionName;
                className = classTable.classType.get(functionName);
                isMethod = true;     
                varSegment = classTable.classKind.get(objectName);
                    if (varSegment.equals("field")){
                        varSegment = "this";
                    }
                varIndex = Integer.toString(classTable.classIndex.get(objectName));
             } else {
                className = functionName;
             }
             // should print period
            tokenizer.advance();
        

            //if method then first var to push to stack is the referenced object
            if (isMethod) {

                vmWriter.writePush(varSegment, varIndex);
            }

            //getting function name
            functionName = tokenizer.advance();

            tokenizer.advance();
            // should print open parenthese
            if (isMethod) {
                numberOfArgs = this.compileExpressionList() + 1;
            } else {
                numberOfArgs = this.compileExpressionList();
            }
            tokenizer.advance();
        }

        //vmwritercode for subroutine call here
        classAndFunctionName = className + "." + functionName;



        vmWriter.writeCall(classAndFunctionName, numberOfArgs);
    }

    public int compileExpressionList() {
        int numberOfArgs = 0;

        String nextToken = tokenizer.tokens.peekFirst();

        if (!nextToken.equals(")")) {
            numberOfArgs++;
            this.compileExpression();
            while (tokenizer.tokens.peekFirst().equals(",")) {
                numberOfArgs++;
                // should print a comma
                tokenizer.advance();
                this.compileExpression();
            }
        }

        return numberOfArgs;
    }

    public void compileUrnaryOP() {
        String operatorType = tokenizer.advance();

        //then compile term after urnary operator
        this.compileTerm();

        //writing vm code for urnary operator
        if (operatorType.equals("~"))
            vmWriter.writeArithmetic("not");
        else if (operatorType.equals("-")){
            vmWriter.writeArithmetic("neg");
    }
    }

    public void compileVarName() {
        String nextToken;
        String varName = tokenizer.advance();
        String nextSymbol = tokenizer.tokens.peekFirst();
        String varKind, varIndex;
        boolean isArray = (nextSymbol.equals("["));

        if (classTable.subroutineType.containsKey(varName)) {
            varKind = classTable.subroutineKind.get(varName);
            varIndex = classTable.subroutineIndex.get(varName).toString();

        } else {
            varKind = classTable.classKind.get(varName);

            //if a variable is a field variable then it is referred to in the pointer memory segment where pointer base address = "this"
            if (varKind.equals("field")){
                varKind = "this";
            }


            varIndex = classTable.classIndex.get(varName).toString();
        }

        //vmcode here //proper code depends on if varName is array or simply a var.

        if (isArray) {
            //should remove the [ symbol
            nextSymbol = tokenizer.advance();
            this.compileExpression();
            vmWriter.writePush(varKind, varIndex);
            vmWriter.writeArithmetic("add");
            vmWriter.writePop("pointer", "1");
            vmWriter.writePush("that", "0");
            //should remove ] symbol
            nextSymbol = tokenizer.advance();
        } else {
            vmWriter.writePush(varKind, varIndex);
        }

        nextToken = tokenizer.tokens.peekFirst();

    }

    public void compileParenthetical() {

        tokenizer.advance(); // should print open parenthese
        this.compileExpression();
        tokenizer.advance();  // should print closed parenthese
    }

    //returns the typr of token that will be delivered after the "advance" method
    public String determineType() {
        String token = tokenizer.tokens.peekFirst();
        if (CompilationEngine.isIntegerConstant(token)) {
            return "integerConstant";
        } else if (CompilationEngine.isStringConstant(token)) {
            return "stringConstant";
        } else if (CompilationEngine.isKeywordConstant(token)) {
            return "keywordConstant";
        } else if (this.isSubroutineCall()) {
            return "subroutineCall";
        } else if (this.isVarName()){
            return "varName";
            //if it is a parenthetical
        } else if (token.equals("(")) {
            return "parenthetical";
            //if it is an urnaryop 
        } else if (token.equals("-") || token.equals("~")) {
            return "urnaryOP";
        } else {
            return "unknown";
        }

    }

    public boolean isVarName() {
        String token = tokenizer.tokens.peekFirst();
        return (JackTokenizer.tokenType(token).equals("IDENTIFIER"));
       
    }

    public boolean isSubroutineCall() {
        String token = tokenizer.advance();
        String nextToken = tokenizer.tokens.peekFirst();
        boolean output = true;
        // if first token = identifier and 2nd token = open parentheses then true
        if (JackTokenizer.tokenType(token).equals("IDENTIFIER") && nextToken.equals("(")) {
            output = true;
        } else if (JackTokenizer.tokenType(token).equals("IDENTIFIER") && nextToken.equals(".")) {
            output = true;
        } else {
            output = false;
        }
        tokenizer.tokens.addFirst(token);
        return output;
    }   

    public static boolean isIntegerConstant( String token) {

        HashSet<Character> numbers = new HashSet<Character>();
         numbers.add('1');numbers.add('2');numbers.add('3');numbers.add('4');
         numbers.add('5');numbers.add('6');numbers.add('7');numbers.add('8');
         numbers.add('9');numbers.add('0');

        
        
        if (numbers.contains(token.charAt(0))) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isStringConstant(String token) {

        if (token.charAt(0) == '"') {
            return true;
        } else {
            return false;
        }
    }
    //will return true if a token is a keyword NOT if its a keyword + [ expression ]
    public static boolean isVarName(String token) {
        return true;
    }

    public static boolean isKeywordConstant(String token) {
        HashSet<String> keywords = new HashSet<String>();
        keywords.add("true");
        keywords.add("false");
        keywords.add("null");
        keywords.add("this");

        if (keywords.contains(token)) {
            return true;
        } else {
            return false;
        }
    }

    public void writeVMFile(String name, String path){
        vmWriter.writeLines(name, path);
    }

    public static void main(String[] args) {
        //basically used to debug and test my created methods
        return;
    }
  
}