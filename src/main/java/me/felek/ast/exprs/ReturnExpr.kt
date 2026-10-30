package me.felek.ast.exprs

import me.felek.ast.Expr

/**
 * @className: ReturnExpr
 * @author: Felek
 * @date: 27/10/2026 21:59
 */

class ReturnExpr(line: Int, column: Int, var expr: Expr?) : Expr(line, column)