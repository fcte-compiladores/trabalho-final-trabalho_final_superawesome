package lox;
import java.util.Map;
import java.util.HashMap;

class LoxInstance {
    private final Map<String, Object> fields = new HashMap<>();
    private LoxClass klass;

    LoxInstance(LoxClass klass){
        this.klass = klass;
    }

    Object get(token name){
        if (fields.containsKey(name.lexemme)) {
            return fields.get(name.lexemme);
        }

        LoxFunction method = klass.findMethod(name.lexemme);
        if (method != null) {
            return method.bind(this);
        }

        throw new RuntimeError(name, "Undefined property '"+name.lexemme+"'.");
    }

    void set(token name, Object value){
        fields.put(name.lexemme, value);
    }

    @Override
    public String toString(){
        return klass.name + " instance";
    }
}
