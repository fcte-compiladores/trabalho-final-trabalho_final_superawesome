package lox;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import javax.management.RuntimeErrorException;

// to clarify, the interpreter is doing a post-order traversal
// each node evaluetes its children before doing its own work
class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void>{
    private final Map<Expr, Integer> locals = new HashMap<>();
    final Environment globals = new Environment();
    private Environment environment = globals;
    // private Environment environment = new Environment();

    Interpreter(){
        // Native funcions that returns the number of seconds that passed since some point in time
        globals.define("clock", new LoxCallable() {
            @Override
            public int arity() {return 0;}

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments){
                return (double)System.currentTimeMillis()/1000.0;
            }
            @Override
            public String toString(){return "<native fn>";}
        });
    }

    void interpret(List <Stmt> stmts){
        try {
            for ( Stmt stmt : stmts) {
                execute(stmt);
            }
        } catch (RuntimeError error) {
            // TODO: handle exception
            jLox.runtimeError(error);
        }
    }


    @Override
    public Object visitLiteralExpr(Expr.Literal expr){
        return expr.value;
    }

    // Here, evaluate the right operand first and if short circuit, if not, we evaluate the right operand.  
    @Override
    public Object visitLogicalExpr(Expr.Logical expr){
        Object left = evaluate(expr);

        if (expr.operator.type == tokenType.OR) {
            if (isTruthy(left)) {
                return left;
            } else{
                if (!isTruthy(left)) {
                    return left;
                }
            }
        }
        return evaluate(expr.right);
    }

    @Override
    public Object visitSetExpr(Expr.Set expr){
        Object obj = evaluate(expr.object);

        if (!(obj instanceof LoxInstance)) {
            throw new RuntimeError(expr.name, "Only instances have fields.");
        }
        Object value = evaluate(expr.value);
        ((LoxInstance)obj).set(expr.name, value);
        return value;
    }

    @Override
    public Object visitSuperExpr(Expr.Super expr){
        int distance = locals.get(expr);
        LoxClass superclass = (LoxClass)environment.getAt(distance, "super");
        LoxInstance object = (LoxInstance)environment.getAt(distance - 1, "this");

        LoxFunction method = superclass.findMethod(expr.method.lexemme);

        if (method == null) {
            throw new RuntimeError(expr.method, "Undefined property '"+expr.method.lexemme+"'.");
        }
        return method.bind(object);
    }

    @Override
    public Object visitThisExpr(Expr.This expr){
        return lookupVariable(expr.keyword, expr);
    }

    // Evaluating parenthesis
    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    private void execute(Stmt stmt){
        stmt.accept(this);
    }

    void resolve(Expr expr, int depth){
        locals.put(expr,depth);
    }


    void executeBlock(List<Stmt> statements, Environment environment){
        Environment previous = this.environment;
        try {
            this.environment = environment;
            for (Stmt stmt : statements) {
                execute(stmt);
            }
            
        } finally {
            this.environment = previous;
        }
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt){
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt){

        Object superclass = null;
        if (stmt.superclass != null) {
            superclass = evaluate(stmt.superclass);
            if (stmt.superclass != null) {
                superclass = evaluate(stmt.superclass);
                if (!(superclass instanceof LoxClass)) {
                    throw new RuntimeError(stmt.superclass.name, "Superclass must be a class.");
                }
            }
        }

        environment.define(stmt.name.lexemme, null);

        if (stmt.superclass != null) {
            environment = new Environment(environment);
            environment.define("super", superclass);
        }

        Map<String, LoxFunction> methods = new HashMap<>();
        for (Stmt.Function method : stmt.methods) {
            LoxFunction function = new LoxFunction(method, environment, method.name.lexemme.equals("init"));
            methods.put(method.name.lexemme, function);
        }
        
        LoxClass klass = new LoxClass(stmt.name.lexemme,(LoxClass)superclass, methods);
        
        if (superclass != null) {
            environment = environment.enclosing;
        }

        environment.assign(stmt.name, klass);
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt){
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt){
        LoxFunction function = new LoxFunction(stmt, environment, false);
        environment.define(stmt.name.lexemme, function);
        return null;
    }

@Override
public Void visitIfStmt(Stmt.If stmt) {
    Object condition = evaluate(stmt.condition);
    boolean truthy = isTruthy(condition);
    // System.out.println("If condition: " + condition + ", truthy: " + truthy); // Debug
    if (truthy) {
        execute(stmt.thenBranch);
    } else if (stmt.elseBranch != null) {
        execute(stmt.elseBranch);
    }
    return null;
}

    @Override
    public Void visitPrintStmt(Stmt.Print stmt){
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt){
        Object value = null;
        if (stmt.value != null) {
            value= evaluate(stmt.value);
        }
        throw new Return(value);
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt){
        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }

        environment.define(stmt.name.lexemme, value);
        return null;
    }

    // Interpret while loop
    @Override
    public Void visitWhileStmt(Stmt.While stmt){
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body);
        }
        return null;
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr){
        Object value = evaluate(expr.value);
        Integer distance = locals.get(expr);
        if (distance != null) {
            environment.assignAt(distance,expr.name, value);
        } else{
            globals.assign(expr.name, value);
        }
        
        return value;
    }

    // Evaluating unary
    @Override
    public Object visitUnaryExpr(Expr.Unary expr){
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case BANG:
                return !isTruthy(right);
            case MINUS:
                // Trying to get the runtime error:
                checkNumberOperand(expr.operator, right);
                return -(double)right;


        }
        // UNreachable
        return null;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr){
        // Object value = environment.get(expr.name); // Debug
        // System.out.println("Variable " + expr.name.lexemme + " = " + value); // Debug
        return lookupVariable(expr.name, expr);
    }

    private Object lookupVariable(token name, Expr expr){
        Integer distance = locals.get(expr);
        if (distance != null) {
            return environment.getAt(distance, name.lexemme);
        } else{
            return globals.get(name);
        }
    }

    private void checkNumberOperand(token operator, Object operand){
        if (operand instanceof Double) {
            return;
        }
        throw new RuntimeError(operator, "Operand must be a number!");
    }

    private void checkNumberOperands(token operator, Object left, Object right){
        if (left instanceof Double && right instanceof Double) {
            return;
        }
        throw new RuntimeError(operator, "Operands must be numbers!");
    }


    // True or false?
    // Using ruby's rule where if not false or nil everything is true
    private boolean isTruthy(Object obj){
        if (obj == null) {  
            return false;
        }
        if (obj instanceof Boolean) {
            return (Boolean)obj;
        }
        if (obj instanceof Double) {
            return (Double)obj != 0.0;
        }
        return true;
    }

    private boolean isEqual(Object obj1, Object obj2) {
        if (obj1 == null && obj2 == null) {
            return true;
        }
        if (obj1 == null || obj2 == null) {
            return false;
        }
        if (obj1 instanceof String && obj2 instanceof String) {
            return ((String) obj1).equals((String) obj2);
        }
        return obj1.equals(obj2);
}

    private String stringify(Object object){
        if (object == null) {
            return "nil";
        }
        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }
        return object.toString();
    }

    // binary
@Override
public Object visitBinaryExpr(Expr.Binary expr) {
    Object left = evaluate(expr.left);
    Object right = evaluate(expr.right);
    // System.out.println("Binary op: " + expr.operator.type + ", left = " + left + ", right = " + right); // DEbug
    switch (expr.operator.type) {
        case GREATER:
            checkNumberOperands(expr.operator, left, right);
            boolean greater = (Double) left > (Double) right;
            //System.out.println("GREATER result: " + greater); // Debug
            return greater;
        case GREATER_EQUAL:
            checkNumberOperands(expr.operator, left, right);
            boolean greaterEqual = (Double) left >= (Double) right;
            //System.out.println("GREATER_EQUAL result: " + greaterEqual); // Debug
            return greaterEqual;
        case LESS:
            checkNumberOperands(expr.operator, left, right);
            boolean less = (Double) left < (Double) right;
            // System.out.println("LESS result: " + less); // Debug
            return less;
        case LESS_EQUAL:
            checkNumberOperands(expr.operator, left, right);
            boolean lessEqual = (Double) left <= (Double) right;
            //System.out.println("LESS_EQUAL result: " + lessEqual); // Debug
            return lessEqual;
        case BANG_EQUAL:
            boolean notEqual = !isEqual(left, right);
            // System.out.println("BANG_EQUAL result: " + notEqual); // Debug
            return notEqual;
        case EQUAL_EQUAL:
            boolean equal = isEqual(left, right);
            // System.out.println("EQUAL_EQUAL result: " + equal); // Debug
            return equal;
        case MINUS:
            checkNumberOperands(expr.operator, left, right);
            return (double) left - (double) right;
        case PLUS:
            if (left instanceof Double && right instanceof Double) {
                return (double)left + (double)right;
            }
            if (left instanceof String || right instanceof String) {
                String leftStr = stringify(left);
                String rightStr = stringify(right);
                return leftStr + rightStr;
            }
            throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings!");
        case SLASH:
            checkNumberOperands(expr.operator, left, right);
            return (double) left / (double) right;
        case STAR:
            checkNumberOperands(expr.operator, left, right);
            return (double) left * (double) right;
    }
    return null;
}

    @Override
    public Object visitCallExpr(Expr.Call expr){
        Object callee = evaluate(expr.callee);

        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments) {
            arguments.add(evaluate(argument));
        }
        // Check if trying to call using strings
        if (!(callee instanceof LoxCallable)) {
            throw new RuntimeError(expr.paren, "Can only call funcions and classes.");
        }

        LoxCallable function = (LoxCallable)callee;

        // need to check if the number of parameters is equal to the nuber of arguments declared
        if (arguments.size() != function.arity()) {
            throw new RuntimeError(expr.paren, "Expected "+function.arity()+" arguments but got "+arguments.size()+".");
        }

        return function.call(this, arguments);
    }
    @Override
    public Object visitGetExpr(Expr.Get expr){
        Object obj = evaluate(expr.object);
        if (obj instanceof LoxInstance) {
            return ((LoxInstance)obj).get(expr.name);
        }
        throw new RuntimeError(expr.name, "Only instances have properties.");
    }
}
