package me.felek.ast.loop

import me.felek.ast.Expr
import me.felek.ast.exprs.VariableDeclaration

/**
 * @className: ForExpr
 * @author: Felek
 * @date: 29/10/2026 15:55
 */

class ForExpr(line: Int, column: Int, var initializer: VariableDeclaration?, var condition: Expr?, var increment: Expr?, var body: Expr) : Expr(line, column)