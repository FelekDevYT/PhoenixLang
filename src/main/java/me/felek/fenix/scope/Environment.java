package me.felek.fenix.scope;

import me.felek.ast.type.ValueType;

import java.util.HashMap;
import java.util.Map;

/**
 * @className: Environment
 * @author: Felek
 * @date: 27/10/2026 18:05
 */

public class Environment {
    public Environment parent = null;
    public Map<String, VariableSymbol> variables = new HashMap<>();

    public Environment(Environment parent) {
        this.parent = parent;
    }

    public void defineVariable(String name, ValueType type, boolean isConst) {
        variables.put(name, new VariableSymbol(name, type, isConst));
    }

    public void defineVariable(String name, VariableSymbol s) {
        variables.put(name, s);
    }

    public VariableSymbol lookupVariable(String name) {
        if (variables.containsKey(name)) {
            return variables.get(name);
        }
        if (parent != null) {
            return parent.lookupVariable(name);
        }

        return null;
    }

    @Override
    public String toString() {
        return "Environment{" +
                "parent=" + parent +
                ", variables=" + variables +
                '}';
    }
}
