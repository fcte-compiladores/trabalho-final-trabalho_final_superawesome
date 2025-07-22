# jLox
## Integrant
Renan Camara - 222006409 - Turma 03 - 16hrs
## Introduction
This repository implements from Lox to Java.
- Control Flow
- Loops
- Classes
- Inheritance
## Install and Run
- Install JDK 11 +
- Simply clone repository and run `java lox.jLox`
- If no path is given, a prompt will open to code
## Examples
- Examples can be found inside `examples/`
- They can be tested running `java lox.jLox ./examples/EXAMPLE-HERE`
## Structure
- Main Class: jLox
- Token types are defined in lox/tokenType and lox/token
- Scanning happens diretcly in lox/Scanner.java (Expressions and Statements were created using tool/generateAST)
- Parser and Resolve are done in lox/Parser.java and lox/Resolver.java
- For OOP, Classes such as lox/LoxFunction , lox/LoxInstance and lox/LoxClass are made to define and auxiliate running them.
## Known Limitations for future implementation
- There is not a `break` statement implemented
- There is no support for multi-line comments
- There is no use to `new` when instancing
- There is no support for if statements using `?` and `:`
- No error handling when dividing by zero =(
## References
https://craftinginterpreters.com/
