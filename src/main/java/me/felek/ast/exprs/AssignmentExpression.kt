package me.felek.ast.exprs

import me.felek.ast.Expr
import me.felek.bright.parser.FenixParser

/**
 * @className: AssignmentExpression
 * @author: Felek
 * @date: 29/10/2026 15:36
 */

class AssignmentExpression(line: Int, column: Int, var name: String, var value: Expr) : Expr(line, column) {
}