package me.felek.ast.exprs

import me.felek.ast.Expr

/**
 * @className: FloatExpr
 * @author: Felek
 * @date: 27/10/2026 14:09
 */

class FloatExpr(line: Int, column: Int, var value: Float) : Expr(line, column)