package me.felek.ast.exprs

import me.felek.ast.Expr
import me.felek.ast.type.ValueType

/**
 * @className: ArrayCreateExpr
 * @author: Felek
 * @date: 29/10/2026 20:46
 */

class ArrayCreateExpr(line: Int, column: Int, var indexes: List<Int>, var type: ValueType) : Expr(line, column)