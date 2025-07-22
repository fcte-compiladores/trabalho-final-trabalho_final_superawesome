package lox;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.IOError;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class jLox {
    
    // Running interpreter
    private static final Interpreter interpreter = new Interpreter();

    static boolean hadError = false; // Restart  had error
    static boolean hadRuntimeError = false; // Helps to tell what line were
    public static void main(String[] args) throws IOException {
        if (args.length >1) {
            System.out.println("Usage: Lox [script]");
            System.out.println(64);
        } else if (args.length == 1) {
            runFile(args[0]);
        }else {
            runPrompt();
        }
    }

    // Run jLox giving a path to file so that it reads and executes it
    private static void runFile(String path) throws IOException{
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes,Charset.defaultCharset()));
        if (hadError) {
            System.exit(65); // Indicate and error in the exit code
        }
        if (hadRuntimeError) {
            System.exit(70);
        }
    }

    // Run jLox without any arguments to open a prompt to run code 
    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);
        for (;;){
            System.out.print("> ");
            String line = reader.readLine(); // Reads an input normally typin ctrl+D return EOF
            if (line == null) { // Checking to stop loop 
                break;
            }
            run(line);
            hadError = false; // Restart without killing the session
        }
    }

    private static void run(String source){
        Scanner scanner = new Scanner(source);
        List<token> tokens = scanner.scanTokens(); // Will create a scanner class later on

        Parser parser = new Parser(tokens);
        List<Stmt> stmts = parser.parse();

        // Stop if there was a syntax error.
        // done with parse =3
        if (hadError) {
            System.out.println("Parser error occurred.");
            return;
        }
        Resolver resolver = new Resolver(interpreter);
        resolver.resolve(stmts);

        // Gotta check again cause need to skip interpret.
        if (hadError) {
            System.out.println("Resolver error occured.");
            return;
        }

        interpreter.interpret(stmts);
    }

    // Error Handling
    static void error (int line, String message){
        report(line, "", message);
    }

    private static void report (int line, String where, String message){
        System.err.println("line["+line+"] Error"+where+": "+ message);
        hadError = true;
    }
    static void error (token Token, String message){
        if (Token.type == tokenType.EOF) {
            report(Token.line, " at end", message);
        } else {
            report(Token.line, " at '"+ Token.lexemme+ "'", message);
        }
    }
    static void runtimeError(RuntimeError error){
        System.err.println(error.getMessage()+ "\n[line " + error.Token.line + "]");
        hadRuntimeError = true;
    }

}
