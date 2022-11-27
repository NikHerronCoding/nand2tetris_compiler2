
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.HashSet;
import java.util.HashMap;

public class CompilationEngine {

    public JackTokenizer tokenizer;
    public LinkedList<String> tags;
    public SymbolTable classTable;
    public VMWriter vmWriter;


    public CompilationEngine(JackTokenizer jackTokenizer) {
        this.tokenizer = jackTokenizer;
        vmWriter = new VMWriter();
        this.tags = new LinkedList<String>();
    }

    //in the case that you need to get the xml output for a lexical element, they can be output by this
    public static String printLexicalUnit(String token) {
        String openTag = "";
        String closeTag = "";
        switch (JackTokenizer.tokenType(token)) {
            case "SYMBOL":
                token = CompilationEngine.symbolTag(token);
                openTag = "<symbol>";
                closeTag = "</symbol>";
                break;
            case "INT_CONST":
                openTag = "<integerConstant>";
                closeTag = "</integerConstant>";
                break;
            case "STRING_CONST":
                openTag = "<stringConstant>";
                closeTag = "</stringConstant>";
                break;
            case "KEYWORD":
                openTag = "<keyword>";
                closeTag = "</keyword>";
                break;
            case "IDENTIFIER":
                openTag = "<identifier>";
                closeTag = "</identifier>";
                break;
            default:
                openTag = "<unidentified>";
                closeTag = "</unidentified>";
                break;
        }
        return openTag + " " + token + " " + closeTag;
    }

    public static String printIdentifier(String name, String category, String kind, String index) {
        String openTag = "<identifier>";
        String closeTag = "</identifier>";

        //putting the info all into one string to add to tags
        String phrase = openTag + "name: " + name + " category: " + category + " kind:" + kind + " index: " + index + closeTag;

        return phrase;
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

        //prints class open tag
        tags.add("<class>");
        
        //keyword class tag
        currentToken = tokenizer.advance();
        tags.add(CompilationEngine.printLexicalUnit(currentToken));

        //writing className which is an identifier
        currentToken = tokenizer.advance();

        //creating symbol table with type class
        this.classTable = new SymbolTable(currentToken, "CLASS");
        tags.add(CompilationEngine.printIdentifier(currentToken, "CLASS NAME", "CLASS", "NO INDEX"));

        //writing  '{' symbol and associated xml tags
        currentToken = tokenizer.advance();
        tags.add(CompilationEngine.printLexicalUnit(currentToken));

        this.compileclassVarDesc();
        String nextToken = tokenizer.tokens.peekFirst();
        this.classTable.printClassTable();
        while (!nextToken.equals("}")) {
            this.compileSubroutine();
            nextToken = tokenizer.tokens.peekFirst();
        }

        //writing  '}' symbol and associated xml tags
        currentToken = tokenizer.advance();
        tags.add(CompilationEngine.printLexicalUnit(currentToken));
    
        //adding class closing tag, eof after this
        tags.add("</class>");
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

            //adds classVarDec tag
            tags.add("<classVarDec>");

            //adds <keyword> static / field </keyword>
            tags.add(CompilationEngine.printLexicalUnit(currentToken));

            //adds type - here type is an identifier
            currentToken = tokenizer.advance();
            tags.add(CompilationEngine.printLexicalUnit(currentToken));

            //gets variable type for symbol table
            type = currentToken;


            //adds varName - there could be many varNames of a certain type - loops here till all are added
            while (!currentToken.equals(";")) {
                currentToken = tokenizer.advance();
                name = currentToken;
                classTable.classDefine(name, type, kind);
                varIndex = classTable.classIndex.get(name).toString();
                tags.add(CompilationEngine.printIdentifier(name, "CLASS VARIABLE", kind, varIndex));
                
                
                //if token is comma, dump and get another otherwise keep going
                currentToken = tokenizer.advance();
                if (currentToken.equals(",")) {
                    tags.add(CompilationEngine.printLexicalUnit(currentToken));
                }
            }
            //shoudl print symbol tags with semicolon
            tags.add(CompilationEngine.printLexicalUnit(currentToken));

            tags.add("</classVarDec>");
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

        //first tag adds subroutine declarations tag
        tags.add("<subroutineDec>");

        //gets function type and adds it to tags (can be staic or method)
        String staticOrMethod = tokenizer.advance();
        String classKind = classTable.name;
        tags.add(CompilationEngine.printLexicalUnit(staticOrMethod));
        

        //add THIS to arg 0 if method or constructor
        System.out.println("Should be static, method or constructor:  " + staticOrMethod );
        if (staticOrMethod.equals("method") || staticOrMethod.equals("constructor")){
            classTable.subroutineDefine("this", classKind,"argument");
        }

        //gets return type and addds it to tags  - can be int, boolean, char or className
        String functionReturnType = tokenizer.advance();
        tags.add(CompilationEngine.printLexicalUnit(functionReturnType));

        //gets function name and adds it to tags must be identifier so ez
        functionName = tokenizer.advance();
        vmFunctionName = className + "." + functionName;
        tags.add(CompilationEngine.printIdentifier(functionName, "Function Name", "Subroutine", "N/A"));

        //gets ( symbol and adds it to tags
        String symbol = tokenizer.advance();
        tags.add(CompilationEngine.printLexicalUnit(symbol));

        //involves compilation of the arguments in function
        this.compileParameterList();
        
        // gets ) symbol and adds it to tags
        symbol = tokenizer.advance();
        tags.add(CompilationEngine.printLexicalUnit(symbol));
        this.compileSubroutineBody(vmFunctionName);


        tags.add("</subroutineDec>");
        classTable.printSubroutineTable(functionName);
    }

    public void compileParameterList() {

        //clearing the subroutine symbol table for this new subroutine
        //classTable.startSubroutine();

        //declaring variable(s) to store var in symbol table
        String kind = "argument";

        tags.add("<parameterList>");
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
         
            

            // adds parameter name and type to tags
            tags.add(CompilationEngine.printLexicalUnit(parameterType));
            tags.add(CompilationEngine.printLexicalUnit(parameterName));
            tags.add(CompilationEngine.printIdentifier(parameterName, parameterType, "arg", varIndex));

            //gets next token comma indicates keep going, also gotta add , to 
            nextToken = tokenizer.tokens.peekFirst();
            if (nextToken.equals(",")) {
                nextToken = tokenizer.advance();
                tags.add(CompilationEngine.printLexicalUnit(nextToken));
            }
            
        }
        
        tags.add("</parameterList>");
    }

    public void compileSubroutineBody(String vmFunctionName) {
        int numLocalVars = 0;

        //adding subroutineBody opening tag
        tags.add("<subroutineBody>");

        // adding open curly bracket to tags
        String symbol = tokenizer.advance();
        tags.add(CompilationEngine.printLexicalUnit(symbol));

        String currentToken = tokenizer.tokens.peekFirst();
        
        while (!currentToken.equals("}")) {
            if (currentToken.equals("var")) {
                this.compileVarDec();    
            } else {
                //writing vm function declaration code here becuase we finally know how many local variables exist!
                numLocalVars = classTable.subroutineVarCount("local"); //need to know how many local vars there are
                vmWriter.writeFunction(vmFunctionName, numLocalVars);


                //compiling rest of statements within the subroutine
                this.compileStatements();
                break;
            }
            currentToken = tokenizer.tokens.peekFirst();
        }

        // gets } symbol and adds it to tags
        symbol = tokenizer.advance();
        tags.add(CompilationEngine.printLexicalUnit(symbol));


        //adding subroutineBody opening tag
        tags.add("</subroutineBody>");
    }
    //returns the num of local
    public void compileVarDec() {
        String kind = "local";
        String symbol = "";
        String varIndex;
        // adding vardec open tag
        tags.add("<varDec>");
        
        //adding  var tag
        String var = tokenizer.advance();
        tags.add(CompilationEngine.printLexicalUnit(var));

        //adding type tag
        String type = tokenizer.advance();
        tags.add(CompilationEngine.printLexicalUnit(type));

            while (!symbol.equals(";")) {

                //getting varname 
                String varName = tokenizer.advance();

                //adding variable to symbol table with subroutine scope
                classTable.subroutineDefine(varName, type, kind);
                varIndex = classTable.subroutineIndex.get(varName).toString();

                //adding tag
                tags.add(CompilationEngine.printIdentifier(varName, "local", kind, varIndex ));
                
                //adding variable to symbol table with subroutine scope
                //classTable.subroutineDefine(varName, type, kind);

                //getting next token, either will be comma or semicolon
                symbol = tokenizer.tokens.peekFirst();
                if (symbol.equals(",")) {
                    symbol = tokenizer.advance();
                    tags.add(CompilationEngine.printLexicalUnit(symbol));
                }
        }

        //adding closing ;
        symbol = tokenizer.advance();
        tags.add(CompilationEngine.printLexicalUnit(symbol));
        
        // adding vardec close tag
        tags.add("</varDec>");
        return;
    }


    public void compileStatements() {
        //output opening statemnts tag
        tags.add("<statements>");

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
        //output closing statemnts tag
        tags.add("</statements>");
    }

    public void compileIfStatement() {
        //adds if statement tag
        tags.add("<ifStatement>");

        String currentToken = tokenizer.advance();
        // adds the 'if' statement to the tags
        tags.add(CompilationEngine.printLexicalUnit(currentToken));

        //adds the open parenthese to the tags
        String symbol = tokenizer.advance();
        tags.add(CompilationEngine.printLexicalUnit(symbol));

        //compiles the expression to be evaluated
        this.compileExpression();

        //adds the close parenthese to the tags
        symbol = tokenizer.advance();
        tags.add(CompilationEngine.printLexicalUnit(symbol));

        //adds the open curly bracket to the tags
        symbol = tokenizer.advance();
        tags.add(CompilationEngine.printLexicalUnit(symbol));

        //this compiles the statements within the if statement -recursive
        this.compileStatements();

        //adds the close curly bracket to the tags
        symbol = tokenizer.advance();
        tags.add(CompilationEngine.printLexicalUnit(symbol));

        //determines if the there is an "else" statement in addition to the 'if'
        currentToken = tokenizer.tokens.peekFirst();

        if (currentToken.equals("else")) {

            //adding keywordelse to tags
            currentToken = tokenizer.advance();
            tags.add(CompilationEngine.printLexicalUnit(currentToken));
            
            //adding opening bracket to tags
            symbol = tokenizer.advance();
            tags.add(CompilationEngine.printLexicalUnit(symbol));

            //adding statements to else block
            this.compileStatements();

            //adding closing bracket to tags
            symbol = tokenizer.advance();
            tags.add(CompilationEngine.printLexicalUnit(symbol));
        }

        //adds closing if statement tag
        tags.add("</ifStatement>");
    }

    public void compileWhileStatement() {

        //adds opening while statement tags
        tags.add("<whileStatement>");

        //adding while token as a keyword
        String currentToken = tokenizer.advance();
        tags.add(CompilationEngine.printLexicalUnit(currentToken));

        //adding open parenthese 
        String symbol = tokenizer.advance();
        tags.add(CompilationEngine.printLexicalUnit(symbol));

        //compiling expression
        this.compileExpression();

        //adding close parenthese 
        symbol = tokenizer.advance();
        tags.add(CompilationEngine.printLexicalUnit(symbol));

        //adding open curly brace
        symbol = tokenizer.advance();
        tags.add(CompilationEngine.printLexicalUnit(symbol));

        //compiling statements
        this.compileStatements();

        //adding closed curly brace
        symbol = tokenizer.advance();
        tags.add(CompilationEngine.printLexicalUnit(symbol));


        //adds closing while statement tags
        tags.add("</whileStatement>");
    }

    public void compileDoStatement() {
        //adds opening do statment tags
        tags.add("<doStatement>");

        //adding 'do' keyword to tags
        String nextToken = tokenizer.advance();
        tags.add(CompilationEngine.printLexicalUnit(nextToken));        

        //compiles subroutineCall
        this.compileSubroutineCall();

        //add semicolon to end of statment
        String symbol = tokenizer.advance();
        tags.add(CompilationEngine.printLexicalUnit(symbol));     

        //adds closing do statment tags
        tags.add("</doStatement>");
    }

    public void compileReturnStatement() {
        //adding opening returnStatment tags
        tags.add("<returnStatement>");

        //adding return keyword to tags
        String nextToken = tokenizer.advance();
        tags.add(CompilationEngine.printLexicalUnit(nextToken)); 
        
        //seeing if we need to return a value or just return
        String currentToken = tokenizer.tokens.peekFirst();

        if (!currentToken.equals(";")) {
            this.compileExpression();
        }
        //this should add the end statement semi colon to the tags
        currentToken = tokenizer.advance();
        tags.add(CompilationEngine.printLexicalUnit(currentToken)); 

        //adding closing returnStatment tags
        tags.add("</returnStatement>");

        //vm code here
        vmWriter.writeReturn();

    }

    public void compileLetStatement() {

        String varName, varType, varKind, varIndex, phrase;

        //adding letStatement tags
        tags.add("<letStatement>");


        //adding let keyword tag
        String keyword = tokenizer.advance();
        tags.add(CompilationEngine.printLexicalUnit(keyword));

        //adding varName identifier tag
        varName = tokenizer.advance();
        System.out.println("Compiling let, first var is: " + varName);

        //getting var type kind and index, searches subroutine scope first then class level

        if (classTable.subroutineType.containsKey(varName)) {
            varType = classTable.subroutineType.get(varName);
            varKind = classTable.subroutineKind.get(varName);
            varIndex = classTable.subroutineIndex.get(varName).toString();
        } else {
            varType = classTable.classType.get(varName);
            varKind = classTable.classKind.get(varName);
            varIndex = classTable.classIndex.get(varName).toString();
        }
        
        //
        phrase = "<identifier> VARIABLE NAME: " + varName + "  Variable Type: " +varType + "  Variable Kind: " + varKind + "  Variable Index: "+ varIndex + "</identifier>";
        tags.add(phrase);


        //adding possible symbol [
        String symbol = tokenizer.tokens.peekFirst();
        if (symbol.equals("[")) {
            symbol = tokenizer.advance();
            tags.add(CompilationEngine.printLexicalUnit(symbol));
            // if [  exists then an expression within it exists
            this.compileExpression();

            //adding closing ]
            symbol = tokenizer.advance();
            tags.add(CompilationEngine.printLexicalUnit(symbol));
        }

        //adding equals sign
        symbol = tokenizer.advance();
        tags.add(CompilationEngine.printLexicalUnit(symbol));

        //compiling expression 
        this.compileExpression();

        //VM CODE HERE - POPS VALUE TO DESIRED MEMORY SEGMENT LOCATION
        vmWriter.writePop(varKind, varIndex);

        //adding semi colon to signify end of statement
        symbol = tokenizer.advance();
        tags.add(CompilationEngine.printLexicalUnit(symbol));

        //adding closing letStatement tags
        tags.add("</letStatement>");

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

        tags.add("<expression>");

        this.compileTerm();

        while (operators.contains(tokenizer.tokens.peekFirst())) {
            operator = tokenizer.advance();
            tags.add(CompilationEngine.printLexicalUnit(operator)); // should print the operator symbol


            //continue compilation of next term (if operator exists another term must exist)
            this.compileTerm();

            //adding vm code for operator here - converstion from infix to postfix so vm arithmetic is after term compilation
            vmTerm = operatorMap.get(operator);
            vmWriter.writeArithmetic(vmTerm);
        }

        


        //adding final tag
        tags.add("</expression>");
    }

    public void compileTerm() {
        tags.add("<term>");
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

        tags.add("</term>");
    }

    public void compileIntegerConstant() {
        String integerConstant = tokenizer.advance();
        //tags for xml file
        tags.add("<integerConstant> " + integerConstant + " </integerConstant>");

        //vm code added here
        vmWriter.writePush("constant", integerConstant);
    }

    public void compileStringConstant() {
        String currentToken = tokenizer.advance();
        tags.add("<stringConstant> " + currentToken.substring(1,currentToken.length()-1) + " </stringConstant>");
    }

    public void compileKeywordConstant() {
        tags.add("<keyword> " + tokenizer.advance() + " </keyword>");
    }

    public void compileSubroutineCall() {
        tags.add(CompilationEngine.printLexicalUnit(tokenizer.advance())); // should print identifier term + identifier tags (either class name or subroutine name)
        if (tokenizer.tokens.peekFirst().equals("(")) {
            tags.add(CompilationEngine.printLexicalUnit(tokenizer.advance())); //this tag should be open parenthese
            this.compileExpressionList();
            tags.add(CompilationEngine.printLexicalUnit(tokenizer.advance())); //should print closed parenthese
        }

        if (tokenizer.tokens.peekFirst().equals(".")) {
            tags.add(CompilationEngine.printLexicalUnit(tokenizer.advance())); // should print period
            tags.add(CompilationEngine.printLexicalUnit(tokenizer.advance())); // should print identifier indicating subroutine name;
            tags.add(CompilationEngine.printLexicalUnit(tokenizer.advance())); // should print open parenthese
            this.compileExpressionList();
            tags.add(CompilationEngine.printLexicalUnit(tokenizer.advance())); //should print closed parenthese
        }
    }

    public void compileExpressionList() {
        tags.add("<expressionList>");
        String nextToken = tokenizer.tokens.peekFirst();

        if (!nextToken.equals(")")) {
            this.compileExpression();
            while (tokenizer.tokens.peekFirst().equals(",")) {
                tags.add(CompilationEngine.printLexicalUnit(tokenizer.advance())); // should print a comma
                this.compileExpression();
            }
        }
        tags.add("</expressionList>");
    }

    public void compileUrnaryOP() {
        tags.add(CompilationEngine.printLexicalUnit(tokenizer.advance())); // should print symbol tag for either urnary negation or urnary not
        this.compileTerm();
    }

    public void compileVarName() {

        String varName = tokenizer.advance();
        String varKind, varIndex;

        if (classTable.subroutineType.containsKey(varName)) {
            varKind = classTable.subroutineKind.get(varName);
            varIndex = classTable.subroutineIndex.get(varName).toString();
        } else {
            varKind = classTable.classKind.get(varName);
            varIndex = classTable.classIndex.get(varName).toString();
        }



        //xml tags here
        System.out.println();
        System.out.println("compiling: "+ varName);
        tags.add(CompilationEngine.printLexicalUnit(varName)); // should print varname with identifier tag

        //vmcode here
        vmWriter.writePush(varKind, varIndex);
        System.out.println("varname: " + varName + " kind: " + varKind + " index: " + varIndex);
        
        // if var name has indexing for an array must also compile here - will add SOON
        if (tokenizer.tokens.peekFirst().equals("[")) {
            tags.add(CompilationEngine.printLexicalUnit(tokenizer.advance())); // should print symbol [ with symbol tag
            this.compileExpression(); //should compile the expression within the brackets
            tags.add(CompilationEngine.printLexicalUnit(tokenizer.advance())); // should print symbol ] with symbol tag
        }
    }

    public void compileParenthetical() {
        tags.add(CompilationEngine.printLexicalUnit(tokenizer.advance())); // should print open parenthese
        this.compileExpression();
        tags.add(CompilationEngine.printLexicalUnit(tokenizer.advance())); // should print closed parenthese
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

    public void writeXMLFile(String name, String path) {
        String currentTag = "";
        System.out.println(path + "\\" + name + ".xml");
        try {
            FileWriter file = new FileWriter(path + "\\" + name + ".xml");
            while (tags.peekFirst() != null) {
                currentTag = tags.pop();
                file.write(currentTag);
                file.write("\n");
            }

            file.close();

        } catch (IOException e) {
            System.out.println("FILE IO EXCEPTION IN FILE WRITE FUNCTION");
            e.printStackTrace();
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
