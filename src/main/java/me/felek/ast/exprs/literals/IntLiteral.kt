package me.felek.ast.exprs.literals

import me.felek.ast.Expr

/**
 * @className: IntExpr
 * @author: Felek
 * @date: 27/10/2026 14:08
 */

class IntLiteral(line: Int, column: Int, var value: Int) : Expr(line, column)