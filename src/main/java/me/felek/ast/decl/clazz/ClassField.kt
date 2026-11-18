package me.felek.ast.decl.clazz

import me.felek.ast.exprs.VariableDeclaration
import me.felek.fenix.scope.Modifier

/**
 * @className: ClassField
 * @author: Felek
 * @date: 11/13/26 6:48 PM
 */

class ClassField(line: Int, column: Int, var modifier: Modifier, var varDeclaration: VariableDeclaration) : ClassMember(line, column)