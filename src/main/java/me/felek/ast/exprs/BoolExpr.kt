package me.felek.ast.exprs

import me.felek.ast.Expr

/**
 * @className: BoolExpr
 * @author: Felek
 * @date: 27/10/2026 14:10
 */

class BoolExpr(line: Int, column: Int, var value: Boolean) : Expr(line, column)