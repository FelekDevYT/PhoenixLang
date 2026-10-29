package me.felek.ast.exprs

import me.felek.ast.Expr

/**
 * @className: ForExpr
 * @author: Felek
 * @date: 29/10/2026 15:55
 */

class ForExpr(line: Int, column: Int, var initializer: VariableDeclaration, var condition: Expr, var increment: AssignmentExpression, var body: Expr) : Expr(line, column) {
}