package lox;

import java.util.HashMap;
import java.util.Map;

// Here the objective is to store variables with its values.
class Environment {
    Environment(){
        enclosing = null;
    }

    Environment(Environment enclosing){
        this.enclosing = enclosing;
    }

    final Environment enclosing;
    private final Map<String, Object> values = new HashMap<>();    
    

    Object get(token name){
        if (values.containsKey(name.lexemme)) {
            return values.get(name.lexemme);
        }

        if (enclosing != null) {
            return enclosing.get(name);
        }

        throw new RuntimeError(name, "Undefined variable '"+ name.lexemme + "'.");

    }

    void assign (token name, Object value){
        if (values.containsKey(name.lexemme)) {
            values.put(name.lexemme, value);
            return;
        }
        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }
        
        throw new RuntimeError(name, "Undefined variable: '" + name.lexemme +"'.");
    }

    void define(String name, Object value){
        values.put(name, value);
    }

    Environment ancestor(int distance){
        Environment environment = this;
        for (int i = 0; i < distance; i++) {
            environment = environment.enclosing;
        }
        return environment;
    }

    Object getAt(int distance, String name){
        return ancestor(distance).values.get(name);
    }

    void assignAt(int distance, token name, Object value){
        ancestor(distance).values.put(name.lexemme, value);
    }

}
