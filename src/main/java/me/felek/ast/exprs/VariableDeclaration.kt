package me.felek.ast.exprs

import me.felek.ast.Expr
import me.felek.ast.type.ValueType

/**
 * @className: VariableDeclaration
 * @author: Felek
 * @date: 27/10/2026 17:32
 */

class VariableDeclaration(line: Int, column: Int, var name: String, var isConst: Boolean = false, var type: ValueType?, var value: Expr?) : Expr(line, column) {
}