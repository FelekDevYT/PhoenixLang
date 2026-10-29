package me.felek.ast.invoke

import me.felek.ast.Expr

/**
 * @className: InvokeLocalExpr
 * @author: Felek
 * @date: 28/10/2026 14:00
 */

class InvokeLocalExpr(line: Int, column: Int, var method: String, var args: List<Expr>) : Expr(line, column) {
}