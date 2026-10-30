package me.felek.ast.exprs

import me.felek.ast.Expr

/**
 * @className: UnaryNotExpr
 * @author: Felek
 * @date: 30/10/2026 12:07
 */

class UnaryNotExpr(line: Int, column: Int, var expr: Expr) : Expr(line, column)  {
}