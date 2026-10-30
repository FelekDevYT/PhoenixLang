package me.felek.ast.exprs

import me.felek.ast.Expr

/**
 * @className: IdExpr
 * @author: Felek
 * @date: 27/10/2026 21:06
 */

class IdExpr(line: Int, column: Int, var id: String) : Expr(line, column)