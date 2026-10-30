package me.felek.ast.exprs

import me.felek.ast.Expr
import me.felek.ast.type.ValueType

/**
 * @className: IncDecExpr
 * @author: Felek
 * @date: 30/10/2026 12:29
 */

class IncDecExpr(line: Int, column: Int, val varName: String, var isIncrement: Boolean) : Expr(line, column) {
}