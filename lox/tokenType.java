package lox;
enum tokenType{
    // single character token
    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE, COMMA, DOT, MINUS, PLUS, SEMICOLON, SLASH, STAR,

    // single or double character token
    BANG, BANG_EQUAL, EQUAL, EQUAL_EQUAL, GREATER, GREATER_EQUAL, LESS, LESS_EQUAL,

    // Literal
    IDENTIFIER, STRING, NUMBER,

    // Keyword
    AND,CLASS, ELSE, IF, WHILE, VAR, OR, STRUCT, PRINT, RETURN, TRUE, FALSE, NIL, SUPER, FUN, THIS, FOR,

    EOF
}