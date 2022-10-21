
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.HashSet;

public class CompilationEngine {

    public JackTokenizer tokenizer;
    public LinkedList<String> tags;



    public CompilationEngine(JackTokenizer jackTokenizer) {
        this.tokenizer = jackTokenizer;
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
        tags.add(CompilationEngine.printLexicalUnit(currentToken));

        //writing  '{' symbol and associated xml tags
        currentToken = tokenizer.advance();
        tags.add(CompilationEngine.printLexicalUnit(currentToken));

        this.compileclassVarDesc();
        String nextToken = tokenizer.tokens.peekFirst();
        
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

        //continues while there are further class variable to declare
        while (currentToken.equals("static") || currentToken.equals("field")) {
            
            //adds classVarDec tag
            tags.add("<classVarDec>");

            //adds <keyword> static / field </keyword>
            tags.add(CompilationEngine.printLexicalUnit(currentToken));

            //adds type - here type is an identifier
            currentToken = tokenizer.advance();
            tags.add(CompilationEngine.printLexicalUnit(currentToken));

            //adds varName - there could be many varNames of a certain type - loops here till all are added
            while (!currentToken.equals(";")) {
                currentToken = tokenizer.advance();
                tags.add(CompilationEngine.printLexicalUnit(currentToken));
                
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
        //first thing adds subroutine declarations tag
        tags.add("<subroutineDec>");

        //gets function type and adds it to tags
        String functionType = tokenizer.advance();
        tags.add(CompilationEngine.printLexicalUnit(functionType));

        //gets return type and addds it to tags  - can be int, boolean, char or className
        String functionReturnType = tokenizer.advance();
        tags.add(CompilationEngine.printLexicalUnit(functionReturnType));

        //gets function name and adds it to tags must be identifier so ez
        String functionName = tokenizer.advance();
        tags.add(CompilationEngine.printLexicalUnit(functionName));

        //gets ( symbol and adds it to tags
        String symbol = tokenizer.advance();
        tags.add(CompilationEngine.printLexicalUnit(symbol));

        this.compileParameterList();
        
        // gets ) symbol and adds it to tags
        symbol = tokenizer.advance();
        tags.add(CompilationEngine.printLexicalUnit(symbol));
        this.compileSubroutineBody();


        tags.add("</subroutineDec>");
    }

    public void compileParameterList() {

        tags.add("<parameterList>");
        String nextToken = tokenizer.tokens.peekFirst();
        String parameterType = "";
        String parameterName = "";

        //while parameters exist
        while (!nextToken.equals(")")) {

            // gets parameter type and name 
            parameterType = tokenizer.advance();
            
            parameterName = tokenizer.advance();
            

            // adds parameter name and type to tags
            tags.add(CompilationEngine.printLexicalUnit(parameterType));
            tags.add(CompilationEngine.printLexicalUnit(parameterName));

            //gets next token comma indicates keep going, also gotta add , to 
            nextToken = tokenizer.tokens.peekFirst();
            if (nextToken.equals(",")) {
                nextToken = tokenizer.advance();
                tags.add(CompilationEngine.printLexicalUnit(nextToken));
            }
            
        }
        
        tags.add("</parameterList>");
    }

    public void compileSubroutineBody() {

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

    public void compileVarDec() {
        String symbol = "";
        // adding vardec open tag
        tags.add("<varDec>");
        
        //adding  var tag
        String var = tokenizer.advance();
        tags.add(CompilationEngine.printLexicalUnit(var));

        //adding type tag
        String type = tokenizer.advance();
        tags.add(CompilationEngine.printLexicalUnit(type));

            while (!symbol.equals(";")) {

                // adding varname tag
                String varName = tokenizer.advance();
                tags.add(CompilationEngine.printLexicalUnit(varName));


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

    }

    public void compileLetStatement() {
        //adding letStatement tags
        tags.add("<letStatement>");


        //adding let keyword tag
        String keyword = tokenizer.advance();
        tags.add(CompilationEngine.printLexicalUnit(keyword));

        //adding varName identifier tag
        String varName = tokenizer.advance();
        tags.add(CompilationEngine.printLexicalUnit(varName));


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

        //adding semi colon to signify end of statement
        symbol = tokenizer.advance();
        tags.add(CompilationEngine.printLexicalUnit(symbol));

        //adding closing letStatement tags
        tags.add("</letStatement>");

        return;
    }

    public void compileExpression() {
        HashSet<String> operators = new HashSet<String>();
        operators.add("+");operators.add("-");operators.add("*");
        operators.add("/");operators.add("&");operators.add("|");
        operators.add("<");operators.add(">");operators.add("=");

        tags.add("<expression>");

        this.compileTerm();

        while (operators.contains(tokenizer.tokens.peekFirst())) {
            tags.add(CompilationEngine.printLexicalUnit(tokenizer.advance())); // should print the operator symbol
            this.compileTerm();
        }
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
        tags.add("<integerConstant> " + tokenizer.advance() + " </integerConstant>");
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
        tags.add(CompilationEngine.printLexicalUnit(tokenizer.advance())); // should print varname with identifier tag
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

    public void writeFile(String name, String path) {
        String currentTag = "";
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

    public static void main(String[] args) {
        //basically used to debug and test my created methods
        return;
    }
  
}
