package me.felek.ast.exprs

import me.felek.ast.Expr

/**
 * @className: IfElseExpr
 * @author: Felek
 * @date: 28/10/2026 12:02
 */

class IfElseExpr(line: Int, column: Int, var ifExpr: Expr, var ifStmt: Expr, var elseStmt: Expr?) : Expr(line, column)