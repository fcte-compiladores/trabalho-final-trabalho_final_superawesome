package lox;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Scanner { // SCAN!!!!
    private final String source;
    private final List<token> tokens= new ArrayList<>();
    private int start = 0;
    private int current = 0;
    private int line = 1;

    // Keyword Map or smth  
    private static final Map<String, tokenType> keywords;
    static{
        keywords = new HashMap<>();
        keywords.put("and", tokenType.AND);
        keywords.put("class", tokenType.CLASS);
        keywords.put("else", tokenType.ELSE);
        keywords.put("false", tokenType.FALSE);
        keywords.put("for", tokenType.FOR);
        keywords.put("fun", tokenType.FUN);
        keywords.put("if", tokenType.IF);
        keywords.put("nil", tokenType.NIL);
        keywords.put("or", tokenType.OR);
        keywords.put("print", tokenType.PRINT);
        keywords.put("return", tokenType.RETURN);
        keywords.put("super", tokenType.SUPER);
        keywords.put("this", tokenType.THIS);
        keywords.put("true", tokenType.TRUE);
        keywords.put("var", tokenType.VAR);
        keywords.put("while", tokenType.WHILE);
    }
    Scanner(String source){
        this.source = source;
    }

    List<token> scanTokens(){
        while (!isAtEnd()) {
            // We are  at the begging of the next lexemme !
            start = current;
            scanToken();
        }
        tokens.add(new token(tokenType.EOF,"", null, line));
        return tokens;
    }

    // If consumed all characters
    private boolean isAtEnd(){
        return current >= source.length();
    }

    // Recognize Lexemes
    private void scanToken() {
        char c = advance();
        switch (c) {
            // Single character
        case '(': addToken(tokenType.LEFT_PAREN); break;
        case ')': addToken(tokenType.RIGHT_PAREN); break;
        case '{': addToken(tokenType.LEFT_BRACE); break;
        case '}': addToken(tokenType.RIGHT_BRACE); break;
        case ',': addToken(tokenType.COMMA); break;
        case '.': addToken(tokenType.DOT); break;
        case '-': addToken(tokenType.MINUS); break;
        case '+': addToken(tokenType.PLUS); break;
        case ';': addToken(tokenType.SEMICOLON); break;
        case '*': addToken(tokenType.STAR); break; 
            
            // Operators
        case '!':
            addToken(match('=') ? tokenType.BANG_EQUAL : tokenType.BANG);
            break;
        case '=':
            addToken(match('=') ? tokenType.EQUAL_EQUAL : tokenType.EQUAL);
            break;
        case '<':
            addToken(match('=') ? tokenType.LESS_EQUAL : tokenType.LESS);
            break;
        case '>':
            addToken(match('=') ? tokenType.GREATER_EQUAL : tokenType.GREATER);
            break;
        case '/':
            if (match('/')){
                // if it IS a comment than read til the end of line
                while (peek()!= '\n' && !isAtEnd()) {
                    advance();
                }
            } else {
                addToken(tokenType.SLASH);
            }
            break;

            // Ignoring...
        case ' ':
        case '\r':
        case '\t':
            break;
        case '\n':
            line++;
            break;

        case '"':
            string();
            break;
        default:
            if (isDigit(c)) {
                number();
            } 
            else if (isAlpha(c)) {
                identifier();
            }
            else jLox.error(line,"Unexpected character");
        }
      }

      // Check if alpha numeric
      private void identifier(){
        while (isAlphaNumeric(peek())) {
            advance();
        }
        String text = source.substring(start,current);
        tokenType type = keywords.get(text);
        if (type == null) {
            type = tokenType.IDENTIFIER;
        }
            addToken(type);
      }
    
      // Check if next char is =
      private boolean match(char expected){
        if(isAtEnd()) return false;
        if (source.charAt(current) != expected) {
            return false;            
        }
        current ++;
        return true;
      }

      // Get comments -> Lookahead
      private char peek(){
        if (isAtEnd()) {
            return '\0';
        }
        return source.charAt(current);
      }
      // Find if its a number
      private boolean isDigit(char c){
        return c >= '0' && c<='9';
      }

      // If we indeed are in a number, find if has floating point
      private void number(){
        while (isDigit(peek())) {
            advance();
        }
        if (peek() == '.' && isDigit(peekNext())) {
            // consume the .
            advance();

            while (isDigit(peek())) {
                advance();
            }
        }
        addToken(tokenType.NUMBER, Double.parseDouble(source.substring(start,current)));
      }

      // Check to see if there is a number after the .
      private char peekNext(){
        if (current+1 >= source.length()) {
            return '\0';
        }
        return source.charAt(current +1 );
      }

      private boolean isAlpha(char check){
        return( check >= 'a' && check <= 'z' || check >= 'A' && check <= 'Z' || check == '_' );
      }

      private boolean isAlphaNumeric(char check){
        return isAlpha(check) || isDigit(check);
      }

      // Increment char to be read
      private char advance(){
        return source.charAt(current++); 
      }

      // Read String like in comment
      private void string(){
        while (peek() != '"' && !isAtEnd()) {
            if (peek() =='\n') {
                line++;
            }
            advance();
        }
        // if it doesnt close
        if (isAtEnd()) {
            jLox.error(line, "Unterminated String");
        }
        // if close
        advance();
        // Trim the surrounding quotes " "
        String value = source.substring(start+1, current -1);
        addToken(tokenType.STRING, value);
      }

      // Output, grabs the lexemme and creates the token
      private void addToken(tokenType type){
        addToken(type,null);
      }
      private void addToken (tokenType type, Object literal){ // String n Comments
            String text = source.substring(start, current);
            tokens.add(new token(type, text, literal, line));
      }
}
