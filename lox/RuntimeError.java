package lox;

class RuntimeError extends RuntimeException {
    final token Token;

    RuntimeError(token Token, String message){
        super(message);
        this.Token = Token;
    }
}
