package me.felek.ast.decl.clazz

import me.felek.ast.decl.FunctionDeclaration

/**
 * @className: ClassMethod
 * @author: Felek
 * @date: 11/19/26 7:27 PM
 */

class ClassMethod(line: Int, column: Int, var func: FunctionDeclaration) : ClassMember(line, column) {
}