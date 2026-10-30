package me.felek.fenix.utils

import me.felek.ast.type.ValueType
import me.felek.fenix.exceptions.UndefinedTypeException
import org.objectweb.asm.Type

/**
 * @className: TypeUtils
 * @author: Felek
 * @date: 27/10/2026 15:07
 */

object TypeUtils {
    @JvmStatic
    fun typeByName(name: String): ValueType {
        val arr = name.count { it == '[' }
        val base: ValueType = when (val nm = name.replace("[", "").replace("]", "")) {
            "Int" -> ValueType.INT
            "Float" -> ValueType.FLOAT
            "Bool" -> ValueType.BOOL
            "Str" -> ValueType.STRING
            "Void" -> ValueType.VOID
            else -> throw UndefinedTypeException(nm)
        }

        return ValueType("[".repeat(arr) + base.descriptor, base.name + "[]".repeat(arr), arr)
    }

    @JvmStatic
    fun typeByDescriptor(descriptor: String): ValueType {
        val dims = descriptor.count { it == '[' }
        val des = descriptor.replace("[", "")

        val base: ValueType = when (des) {
            "I" -> ValueType.INT
            "F"-> ValueType.FLOAT
            "Z" -> ValueType.BOOL
            "V" -> ValueType.VOID
            "Ljava/lang/String;" -> ValueType.STRING
            else -> throw RuntimeException("Unknown type: $descriptor")
        }

        return ValueType("[".repeat(dims) + base.descriptor, base.name + "[]".repeat(dims), dims)
    }

    @JvmStatic
    fun getAsmType(fullName: String): Type {
        return when (fullName) {
            "Int" -> Type.INT_TYPE
            "Float" -> Type.FLOAT_TYPE
            "Str" -> Type.getType("Ljava/lang/String;")
            "Bool" -> Type.BOOLEAN_TYPE
            else -> Type.getType("L$fullName;")
        }
    }

    @JvmStatic
    fun isBinaryComparable(type1: ValueType, type2: ValueType): Boolean {
        return type1 == type2
    }

    @JvmStatic
    fun canCast(typeA: ValueType, typeB: ValueType): Boolean {
        val convertable = listOf<ValueType>(ValueType.INT, ValueType.FLOAT)

        return convertable.contains(typeA) && convertable.contains(typeB)
    }

    @JvmStatic
    fun getCastReturn(typeA: ValueType, typeB: ValueType): ValueType {
        if (typeA == typeB) {
            return typeA
        }

        if (typeA == ValueType.INT && typeB == ValueType.FLOAT) {
            return ValueType.FLOAT
        } else if (typeA == ValueType.FLOAT && typeB == ValueType.INT) {
            return ValueType.FLOAT
        }

        return ValueType.VOID//unallowed cast
    }
}