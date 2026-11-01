package me.felek.ast.exprs

import me.felek.ast.Expr
import me.felek.ast.type.ValueType

/**
 * @className: IsExpr
 * @author: Felek
 * @date: 10/31/26 9:14 PM
 */

class IsExpr(line: Int, column: Int, var left: Expr, var isType: ValueType) : Expr(line, column) {
}