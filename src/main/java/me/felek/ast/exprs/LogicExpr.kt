package me.felek.ast.exprs

import me.felek.ast.Expr
import me.felek.ast.exprs.util.Logic

/**
 * @className: LogicExpr
 * @author: Felek
 * @date: 30/10/2026 16:54
 */

class LogicExpr(line: Int, column: Int, var op: Logic, var left: Expr, var right: Expr) : Expr(line, column) {
}