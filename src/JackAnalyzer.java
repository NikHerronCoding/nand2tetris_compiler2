
import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;



public class JackAnalyzer {

    public static void main(String[] args) {
        //location of file to tokenize
        String path = args[0];
        File file = new File(path);
        String name = "";


        if (file.isDirectory()) {
            File[] fileList = file.listFiles();


            for (int i = 0; i < fileList.length; i++) {
                //figuring out what to call the compiled file
                name = fileList[i].getName();

                //if file is not a jack file then skip
                if (!name.substring(name.length()-5).equals(".jack")) {
                    continue;
                }

                name = name.substring(0, name.length() - 5); 
                //calling for compilation and writing of each file
                JackAnalyzer.writeCompileFile(fileList[i], name, path);
            }

        } else {
            //finding name
            name = file.getName();
            name = name.substring(0, name.length() - 5);
            path = file.getParentFile().getName();
            //compiling and writing file
            JackAnalyzer.writeCompileFile(file, name, path);
        }
                                    
        return;
    }

    public static void writeCompileFile(File file,String name, String path) {
       
        String text = new String("");

        try {
            Scanner lines = new Scanner(file);
            while (lines.hasNextLine()) {
                text += (lines.nextLine() + "\n");
            }
            lines.close();
        } catch (FileNotFoundException ff) {
            System.out.println("Exception " + ff.toString());
        }

        //removing line comments
        text = formatAndRemoveComments(text);

        //tokenizing
        JackTokenizer tokenizer = new JackTokenizer(text);

        //compiling
        CompilationEngine compiled = new CompilationEngine(tokenizer);
        compiled.compileClass();

        //writing to file
        compiled.writeFile(name, path);

        return;

    }

        // removes block comments, in lines comments and extra whitespace
        public static String formatAndRemoveComments(String text) {
            String outputText = "";
            outputText = removeBlockComments(text);
            outputText = removeLineComments(outputText);
            outputText = removeWhiteSpace(outputText);
            return outputText;
        }
    
        public static String removeLineComments(String text) {
            String[] splitText = text.split("\n");
            String cleanedText = new String("");
    
            for (int i = 0; i < splitText.length; i++) {
                String line = splitText[i];
    
                if (line.contains("//")) {
                    int index = line.indexOf("//");
                    cleanedText += line.substring(0,index);
                } else {
                    cleanedText += line + "\n";
                }
            }
            return cleanedText;
        }
    
        public static String removeBlockComments(String text) {
            String cleanedText = text;
            while (cleanedText.contains("/*")) {
                int openIndex = cleanedText.indexOf("/**");
                int closedIndex = cleanedText.indexOf("*/");
                cleanedText = cleanedText.substring(0, openIndex) + cleanedText.substring(closedIndex + 2, cleanedText.length());
            }
            return cleanedText;
        }
        public static String removeWhiteSpace(String text) {
            String modifiedText = text.replace("\n","");
            modifiedText = modifiedText.replace("\t","");
            modifiedText = modifiedText.replaceAll(" +"," ");
            return modifiedText;
        }

}
