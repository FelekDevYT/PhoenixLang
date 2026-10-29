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
    public fun typeByName(name: String): ValueType {
        val arr = name.count({ it == '[' })
        var nm = name
        nm = name.replace("[", "")
        nm = nm.replace("]", "")

        var base = ValueType.VOID;

        when (nm) {
            "Int" -> base = ValueType.INT
            "Float" -> base = ValueType.FLOAT
            "Bool" -> base = ValueType.BOOL
            "Str" -> base = ValueType.STRING
            "Void" -> base = ValueType.VOID
            else -> throw UndefinedTypeException(nm)
        }
        base.descriptor = "[".repeat(arr) + base.descriptor

        base.dimensions = arr

        return base
    }

    @JvmStatic
    fun typeByDescriptor(descriptor: String): ValueType {
        var type: ValueType = ValueType.VOID
        var des = descriptor
        des.replace("[", "")

        when (des) {
            Int.javaClass.descriptorString() -> type = ValueType.INT
            Float.javaClass.descriptorString() -> type = ValueType.FLOAT
            Boolean.javaClass.descriptorString() -> type = ValueType.BOOL
            "V" -> type = ValueType.VOID
            "Ljava/lang/String;" -> type = ValueType.STRING
            else -> throw RuntimeException("Unknown type: " + descriptor)
        }

        type.dimensions = descriptor.count( { it == '[' })

        return type

//        return when(descriptor) {
//            Int.javaClass.descriptorString() -> ValueType.INT
//            Float.javaClass.descriptorString() -> ValueType.FLOAT
//            Boolean.javaClass.descriptorString() -> ValueType.BOOL
//            "V" -> ValueType.VOID
//            "Ljava/lang/String;" -> ValueType.STRING
//            else -> throw RuntimeException("Unknown type: " + descriptor)
//        }
    }

    @JvmStatic
    public fun getAsmType(fullName: String): Type {
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
        if (type1 == type2) {
            return true
        }

        return false
    }
}