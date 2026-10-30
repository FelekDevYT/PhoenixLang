package me.felek.ast.decl

import me.felek.ast.Expr
import me.felek.ast.Node
import me.felek.ast.type.ValueType

/**
 * @className: FunctionDeclaration
 * @author: Felek
 * @date: 27/10/2026 15:01
 */

class FunctionDeclaration(line: Int, start: Int, var name: String, var args: List<Arg>, var returnType: ValueType, var body: Expr) : Node(line, start)