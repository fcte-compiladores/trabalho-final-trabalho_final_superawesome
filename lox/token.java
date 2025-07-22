package lox;
class token {
    final tokenType type;
    final String lexemme;
    final Object literal;
    final int line;

    token(tokenType type, String lexemme, Object literal, int line){
        this.type = type;
        this.lexemme = lexemme;
        this.literal = literal;
        this.line = line;
    }

    public String toString(){
        return type + " " + lexemme+ " " + literal; 
    }
}
