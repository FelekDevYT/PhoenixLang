package me.felek.fenix.scope

import org.objectweb.asm.Opcodes

/**
 * @className: Modifier
 * @author: Felek
 * @date: 11/11/26 6:10 PM
 */

enum class Modifier(val opcode: Int) {
    PUB(Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC),
    LOC(Opcodes.ACC_PRIVATE)
}