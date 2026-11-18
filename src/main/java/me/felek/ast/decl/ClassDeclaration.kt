package me.felek.ast.decl

import me.felek.ast.Node
import me.felek.ast.decl.clazz.ClassMember
import me.felek.fenix.scope.Modifier

/**
 * @className: ClassDeclaration
 * @author: Felek
 * @date: 11/13/26 6:47 PM
 */

class ClassDeclaration(line: Int, column: Int, var modifier: Modifier, var name: String, var members: List<ClassMember>) : Node(line, column)