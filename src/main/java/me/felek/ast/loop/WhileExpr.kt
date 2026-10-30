package me.felek.ast.loop

import me.felek.ast.Expr

/**
 * @className: WhileExpr
 * @author: Felek
 * @date: 29/10/2026 15:07
 */

class WhileExpr(line: Int, column: Int, var condition: Expr, var statement: Expr) : Expr(line, column)