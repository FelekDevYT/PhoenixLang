package me.felek.ast.exprs

import me.felek.ast.Expr
import me.felek.ast.type.ValueType

/**
 * @className: ArrayGetExpr
 * @author: Felek
 * @date: 29/10/2026 19:55
 */

class ArrayGetExpr(line: Int, column: Int, val left: Expr, val indexes: List<Int>) : Expr(line, column) {
    lateinit var firstValue: ValueType
}