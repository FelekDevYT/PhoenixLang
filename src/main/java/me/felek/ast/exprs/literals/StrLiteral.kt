package me.felek.ast.exprs.literals

import me.felek.ast.Expr

/**
 * @className: StrExpr
 * @author: Felek
 * @date: 27/10/2026 14:09
 */

class StrLiteral(line: Int, column: Int, var value: String) : Expr(line, column)