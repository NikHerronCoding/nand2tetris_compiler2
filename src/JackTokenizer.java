//This is the Jack Tokenizer 


import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class JackTokenizer {

   static final String TERMINATING_CHARACTERS = "'{}()[].,;+-*/&|<>=~ ";
   static final String[] SYMBOLS_ARRAY = {"{", "}", "(", ")", "[", "]", ".", ",",";", "+", "-", "*", "/", "&", "|", "<", ">", "=", "~" };
   static final String[] KEYWORDS_ARRAY = {"class", "constructor", "function", "method", "field", "static", "var", "int", "char", "boolean", "true", "false", "null", "this", "let", "do", "if", "else", "while", "return", "void"};
   static final Set<String> SYMBOLS = new HashSet<>(Arrays.asList(SYMBOLS_ARRAY));
   static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(KEYWORDS_ARRAY));
   static final String NUMBERS = "1234567890";

   LinkedList<String> tokens;

   public JackTokenizer(String text) {
    tokens = tokenization(text);
    return;
   }
   //method to determine if there are any more tokens
   public boolean hasMoreTokens(){
    return (tokens.peekFirst() != null) ? true : false;
   }

   public String advance() {
    String token = tokens.pop();
    return token;
   }

   public static String tokenType(String token){
    String tokenChar = "" + token.charAt(0);
    if (SYMBOLS.contains(tokenChar)) {
        //if token is symbol return symbol
        return "SYMBOL";
    } else if (NUMBERS.indexOf(tokenChar) != -1) {
        //if token is an int return int constant
        return "INT_CONST";
    } else if (tokenChar.equals("\"")) {
        //if token starts with double quote it is a string const
        return "STRING_CONST";
    } else if (KEYWORDS.contains(token)) {
        return "KEYWORD";
    } else {
        return "IDENTIFIER";
    }
   }


    public static LinkedList<String> tokenization(String text) {
        int i = 0, j = 0;
        int textLength = text.length();
        char currentChar = ' ';
        String currentWord = "";
        String terminatingPhrase = "";
        LinkedList<String> tokens = new LinkedList<String>();
        
        //loops through text, finds symbols that indicate token serparation
        while (j < textLength) {
            currentChar = text.charAt(j);
            currentWord = text.substring(i,j);

            if (isTerminatingCharacter(currentChar, currentWord)) {
                
                terminatingPhrase = "" + currentChar;
                i = j + 1;
                if (currentWord != "" && currentWord != " ") {
                    if (currentWord.charAt(0) == '"') {
                        tokens.add(currentWord + '"');
                    } else {
                        tokens.add(currentWord);
                    }

                }
                if (!(terminatingPhrase.equals( " ")||(terminatingPhrase.equals("\"")))) {
        
                    tokens.add(terminatingPhrase);
                }
            }
           
            j++;
        }

        return tokens;
    }

    public static boolean isTerminatingCharacter(char currentChar, String currentWord) {
            if (currentWord.length() > 0 && currentWord.charAt(0) == '"') {
                if (currentChar == '"') {
                    return true;
                } else {
                    return false;
                }

            } else if (TERMINATING_CHARACTERS.indexOf(currentChar) != -1) {
                return true;
            } else {
                return false;
            }
    }

    public void writeTokenFile() {
        String openTag = "";
        String closeTag = "";
        String tokenContent;
        try {
            FileWriter tokenFile = new FileWriter("tokens.xml");
            tokenFile.write("<tokens>\n");
            
            while (tokens.peekFirst() != null) {
                String token = tokens.pop();
                
                //switch to determine xml markup
                switch (JackTokenizer.tokenType(token)) {
                    case "SYMBOL":
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


                //if to determine "token content" to make compatible with XML
                    if (token.charAt(0) == '<'){
                        tokenContent = "&lt;";
                    } else if (token.charAt(0) == '>') {
                        tokenContent = "&gt;";
                    } else if (token.charAt(0) == '&') {
                        tokenContent = "&amp;";
                    } else if (token.charAt(0) == '"') {
                        tokenContent = token.substring(1, (token.length() - 1));
                    } else {
                        tokenContent = token;
                    }

                tokenFile.write(openTag + " ");
                tokenFile.write(tokenContent);
                tokenFile.write(" " + closeTag);
                tokenFile.write("\n");
            }
            tokenFile.write("</tokens>\n");
            tokenFile.close();

        } catch (IOException e) {
            System.out.println("An error occured");
            e.printStackTrace();
        }

    }

}