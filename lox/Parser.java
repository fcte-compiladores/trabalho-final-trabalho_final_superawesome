package lox;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import static lox.tokenType.*;


// Given a valid sequence of tokens, produce the corresponding syntax treee
// Given an invalid sequence, detect errors and tell the user about their mistakes.

public class Parser {
    private static class ParseError extends RuntimeException{}
    private final List<token> tokens;
    private int current = 0;

    Parser(List<token> tokens){
        this.tokens= tokens;
    }
    // visit later when add statements
    // for now its just a single expression
    //----------------------------------
    // the later is now!
    // Creates a series of stmt 

    List<Stmt> parse(){
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }

    private Expr expression(){
        return assignment();
    }

    private Stmt declaration(){
        try {
            if (match(CLASS)) {
                return classDeclaration();
            }
            if (match(FUN)) {
                return function("function");
            }
            if (match(VAR)) {
                return varDeclaration();
            }
            return statement();
        } catch (ParseError error) {
            // TODO: handle exception
            syncronize();
            return null;
        }
    }

    private Stmt classDeclaration(){
        token name = consume(IDENTIFIER, "Expect class name");
        Expr.Variable superclass = null;
        if (match(LESS)) {
            consume(IDENTIFIER, "Expect superclass name.");
            superclass = new Expr.Variable(previous());
        }
        consume(LEFT_BRACE, "Expect '(' before class body");

        List<Stmt.Function> methods = new ArrayList<>();
        while (!check(RIGHT_BRACE)&& !isAtEnd()) {
            methods.add(function("methods"));
        }

        consume(RIGHT_BRACE, "Expected '}' after clas body");

        return new Stmt.Class(name,superclass,methods);
    }

    private Stmt statement(){
        if (match(PRINT)) {
            return printStmt(); // Each gets its own method =3
        }
        if (match(LEFT_BRACE)) {
            return new Stmt.Block(block());
        }
        if (match(IF)) {
            return ifStatement();
        }
        if (match(WHILE)) {
            return whileStmt();
        }
        if (match(RETURN)) {
            return returnStmt();
        }
        if (match(FOR)) {
            return forStatement();
        }
        return expressionStmt();

        
    }

    private Stmt forStatement(){
        consume(LEFT_PAREN, "Expect '()' after 'for'. ");
        // Var initializer -> First field of for
        Stmt initializer;
        if (match(SEMICOLON)) {
            initializer = null;
        } else if (match(VAR)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStmt();
        }
        // COndition -> second field of for
        Expr condition = null;
        if (!check(SEMICOLON)) {
            condition = expression();
        }
        consume(SEMICOLON, "Expect ';' after loop condition");
        // Third field
        Expr increment = null;
        if (!check(RIGHT_PAREN)) {
            increment = expression();
        }
        consume(RIGHT_PAREN, "Expect ')' after for clauses.");
        Stmt body = statement();

        if (increment != null) {
            body = new Stmt.Block(Arrays.asList(body, new Stmt.Expression(increment)));
        }
        if (condition == null ) {
            condition = new Expr.Literal(true);
        }
        body = new Stmt.While(condition, body);

        if (initializer != null ) {
            body = new Stmt.Block(Arrays.asList(initializer,body));
        }

        return body;
    }

    private Stmt ifStatement(){
        consume(LEFT_PAREN, "Expect '(' after 'if'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after 'if' condition.");

        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(ELSE)) {
            elseBranch = statement();
        }
        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    // Print
    private Stmt printStmt(){
        Expr value = expression();
        consume(SEMICOLON, "Expected ';' after value.");
        return new Stmt.Print(value);
    }

    private Stmt returnStmt(){
        token keyword = previous();
        Expr value = null;
        if (!check(SEMICOLON)) {
            value = expression();
        }
        consume(SEMICOLON, "Expect ';' after return value"); // i cant write corretly anymore cant this all end????
        return new Stmt.Return(keyword, value);
    }

    private Stmt varDeclaration(){
        token name= consume(IDENTIFIER, "Expect variable name.");
        
        Expr initialiaze = null;
        if (match(EQUAL)) {
            initialiaze = expression();
        }

        consume(SEMICOLON, "Expect ';' after variable declaration.");
        return new Stmt.Var(name, initialiaze);
    }

    private Stmt whileStmt(){
        consume(LEFT_PAREN, "Expect '(' after while.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect')' after condtion.");
        Stmt body = statement();

        return new Stmt.While(condition, body);
    }

    private Stmt expressionStmt(){
        Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(expr);
    }

    private Stmt.Function function(String kind){
        token name = consume(IDENTIFIER, "Expect "+kind+ " name.");
        consume(LEFT_PAREN, "Expect '(' after "+kind+" name.");
        List<token> parameters = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (parameters.size() >= 255) {
                    error(peek(), "Cant have more than 255 parameters");
                }
                parameters.add(consume(IDENTIFIER, "Expect parameter name."));
            } while (match(COMMA));
        }
        consume(RIGHT_PAREN, "Expect ')' after parameters.");

        // now for the bodyW
        consume(LEFT_BRACE, "Expect '{' before "+kind+ " body.");
        List<Stmt> body = block();
        return new Stmt.Function(name, parameters, body);
    }
    
    private List<Stmt> block(){
        List<Stmt> statements = new ArrayList<>();

        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            Stmt stmt = declaration();
            if (stmt != null) {
                statements.add(stmt);
            }
        }
        consume(RIGHT_BRACE, "Expect '}' after block.");
        return statements;
    }

    private Expr assignment(){
        Expr expr = or();

        if (match(EQUAL)) {
            token equals = previous();
            Expr value = assignment();

            if (expr instanceof Expr.Variable) {
                token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name,value);
            } else if (expr instanceof Expr.Get) {
                Expr.Get get = (Expr.Get)expr;
                return new Expr.Set(get.object, get.name, value);
            }

            error(equals, "Invalid assignment target.");
        }
        return expr;
    }

    private Expr or(){
        Expr expr = and();

        while (match(OR)) {
            token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    private Expr and(){
        Expr expr = equality();

        while (match(AND)) {
            token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }



    private Expr equality(){
        Expr expr = comparison();

        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    // Rule: comparison  → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;

    private Expr comparison(){
        Expr expr = term();

        while (match(GREATER, GREATER_EQUAL,LESS, LESS_EQUAL)) {
            token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    // addition and substration
    private Expr term(){
        Expr expr = factor();

        while (match(MINUS, PLUS)) {
            token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    // and multiplication n division
    private Expr factor(){
        Expr expr = unary();

        while (match(SLASH, STAR)) {
            token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    // now for the unary
    // rule: unary          → ( "!" | "-" ) unary
    //                     | primary ;

    private Expr unary(){
        if (match(BANG, MINUS)) {
            token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return call();
    }

    private Expr call(){
        Expr expr = primary();

        while (true) {
            if (match(LEFT_PAREN)) {
                expr = finishCall(expr);
            } else if(match(DOT)){
                token name = consume(IDENTIFIER, "Expect property name after '.'");
                expr = new Expr.Get(expr, name);
            } 
            else{
                break;
            }
        }
        return expr;
    }

    private Expr finishCall(Expr callee){
        List<Expr> arguments = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (arguments.size() >= 255) {
                    error(peek(), "Cant have more than 255 arguments.");
                    // DONT throw anything cause parser'll enter panic mode
                    // Here, it works fine, there is just too many arguments
                }
                arguments.add(expression());
            } while (match(COMMA));
        }
        token paren = consume(RIGHT_PAREN, "Expect ')' after arguments");
        return new Expr.Call(callee, paren, arguments);
    }

    // primary
    private Expr primary(){
        if (match(FALSE)) {
            return new Expr.Literal(false);
        }
        if (match(TRUE)) {
            return new Expr.Literal(true);
        }
        if (match(NIL)) {
            return new Expr.Literal(null);
        }

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(THIS)) {
            return new Expr.This(previous());
        }

        if (match(SUPER)) {
            token keyword = previous();
            consume(DOT, "Expect . after 'super'.");
            token method = consume(IDENTIFIER, "Expect superclass method name.");
            return new Expr.Super(keyword, method);
        }

        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
        }
        
        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }
        throw error(peek(), "Expected expression.");
        
    }

    
    

    // This checks to see if the current token has any of the given types.
    // If so, it consumes the token and returns true.
    // Otherwise, it returns false and leaves the current token alone. 
    private boolean match(tokenType... types){
        for (tokenType type : types){
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private token consume(tokenType type, String message){
        if (check(type)) {
            return advance();
        }
        throw error(peek(),message);
    }

    // The check() method returns true if the current token is of the given type. 
    private boolean check (tokenType type){
        if (isAtEnd()) {
            return false;
        }
        return peek().type == type;
    }

    // consumes and returns the token
    private token advance(){
        if (!isAtEnd()) {
            current ++;
        }
        return previous();
    }

    // Enter panic mode to report error from consumes()
    private ParseError error(token Token, String message){
        jLox.error(Token, message);
        return new ParseError();
    }

    // discards tokens til its found a statement boundary
    // the main objective is to find an error and sync back 
    private void syncronize(){
        advance();

        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) {
                return;
            }
            switch (peek().type) {
                case CLASS:
                case FUN:    
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
            }
            advance();
        }
    }

    // Other primitive funcions
    private boolean isAtEnd() {
    return peek().type == EOF;
  }

  private token peek() {
    return tokens.get(current);
  }

  private token previous() {
    return tokens.get(current - 1);
  }
}
