package me.felek.ast.type;

import java.util.Objects;

/**
 * @className: ValueType
 * @author: Felek
 * @date: 27/10/2026 17:22
 */

public class ValueType {
    private String descriptor;
    private String name;
    private int dimensions = 0;//0 - not an array

    public ValueType(String descriptor, String name, int dimensions) {
        this.name = name;
        this.descriptor = descriptor;
        this.dimensions = dimensions;
    }

    public ValueType(String name, String descriptor) {
        this.name = name;
        this.descriptor = descriptor;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public void setDescriptor(String descriptor) {
        this.descriptor = descriptor;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getDimensions() {
        return dimensions;
    }

    public void setDimensions(int dimensions) {
        this.dimensions = dimensions;
    }

    public static final ValueType INT = new ValueType("Int", "I");
    public static final ValueType FLOAT = new ValueType("Float", "F");
    public static final ValueType BOOL = new ValueType("Bool", "Z");
    public static final ValueType VOID = new ValueType("Void", "V");
    public static final ValueType STRING = new ValueType("Str", "Ljava/lang/String;");

    @Override
    public String toString() {
        return "ValueType{" +
                "descriptor='" + descriptor + '\'' +
                ", name='" + name + '\'' +
                ", dimensions=" + dimensions +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ValueType valueType)) return false;
        return dimensions == valueType.dimensions && Objects.equals(descriptor, valueType.descriptor) && Objects.equals(name, valueType.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(descriptor, name, dimensions);
    }
}
