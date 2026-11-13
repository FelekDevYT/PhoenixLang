package me.felek.ast.loop

import me.felek.ast.Expr

/**
 * @className: DoWhileExpr
 * @author: Felek
 * @date: 11/12/26 9:09 PM
 */

class DoWhileExpr(line: Int, column: Int, var condition: Expr, var statement: Expr) : Expr(line, column) {
}