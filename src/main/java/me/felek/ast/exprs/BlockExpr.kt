package me.felek.ast.exprs

import me.felek.ast.Expr

/**
 * @className: BlockExpr
 * @author: Felek
 * @date: 27/10/2026 15:03
 */

class BlockExpr(line: Int, column: Int, var body: List<Expr>) : Expr(line, column)