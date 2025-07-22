package tool;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

// The main reason of this file is to generate the ast
// I really did not wanna write all 21 clases by myself =3

public class generateAST {
    public static void main(String[] args) throws IOException{
        if (args.length!=1) {
            System.err.println("Usage: generate_ast <output directory>");
            System.exit(64);
        }
        String outputDir = args[0];

        defineAST(outputDir, "Expr", Arrays.asList(
        "Assign   : token name,Expr value",
        "Binary   : Expr left,token operator,Expr right",
        "Call     : Expr callee,token paren,List<Expr> arguments",
        "Get      : Expr object,token name",
        "Grouping : Expr expression",
        "Literal  : Object value",
        "Logical  : Expr left,token operator,Expr right",
        "Set      : Expr object,token name,Expr value",
        "Super    : token keyword,token method",
        "This     : token keyword",
        "Unary    : token operator,Expr right",
        "Variable : token name"
        ));

        defineAST(outputDir, "Stmt", Arrays.asList(
            "Block      : List<Stmt> statements",
            "Class      : token name,Expr.Variable superclass,List<Stmt.Function> methods",
            "Expression : Expr expression",
            "Function   : token name,List<token> params,"+ "List<Stmt> body",
            "If         : Expr condition, Stmt thenBranch," + "Stmt elseBranch",
            "Print      : Expr expression",
            "Return     : token keyword,Expr value",
            "Var        : token name,Expr initializer",
            "While      : Expr condition,Stmt body"
        ));
    }
    // This needs to output the base Expr.java
    private static void defineAST(String outputDir, String basename, List<String> types) throws IOException{
        String path = outputDir + "/" + basename + ".java";
        PrintWriter writer = new PrintWriter(path, "UTF-8");
        writer.println("package lox;");
        writer.println("import java.util.List;");
        writer.println();
        writer.println("abstract class "+basename+"{");

        defineVisitor(writer, basename, types);

        // define the AST classes
        for (String type : types) {
            String className = type.split(":")[0].trim();
            String fields = type.split(":")[1].trim();
            defineType(writer, basename, className, fields);
        }
        // the base accept method
        writer.println();
        writer.println("    abstract <R> R accept(Visitor<R> visitor);");

        writer.println("}");
        writer.close();
    }

    private static void defineVisitor(PrintWriter writer, String baseName, List<String> types){
        // Creating vistor classes
        writer.println("    interface Visitor<R> {");
        
        for(String type : types){ // itirating through the subclasses and declaring a visit method for each one.
            String typename = type.split(":")[0].trim();
            writer.println("    R visit" + typename + baseName + "(" +typename + " " + baseName.toLowerCase() + ");");

        }
        writer.println(" }");
    }

    private static void defineType( PrintWriter writer, String baseName, String className, String fieldList){
        // Creating class for Expr:
        writer.println(" static class "+ className + " extends " + baseName +"{");

        // Constructor
        writer.println("    " + className + "(" + fieldList + ") {");

        // Store parameters in fields
        String[] fields = fieldList.split(",");
        for (String field : fields) {
                String name = field.split(" ")[1];
                writer.println("    this."+ name +"= " + name+ ";");
        }
        writer.println("    }"); // close constructor
        // Visitor pattern
        writer.println();
        writer.println("    @Override");
        writer.println("    <R> R accept(Visitor<R> visitor) {");
        writer.println("        return visitor.visit"+className+baseName+"(this);");
        writer.println("        }");

        // Fields
        writer.println();
        for (String field : fields) {
            writer.println("    final "+ field + ";");
        }
        writer.println("}"); // closing class
    }
}
