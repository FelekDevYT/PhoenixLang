package me.felek.ast.exprs.literals

import me.felek.ast.Expr

/**
 * @className: FloatExpr
 * @author: Felek
 * @date: 27/10/2026 14:09
 */

class FloatLiteral(line: Int, column: Int, var value: Float) : Expr(line, column)