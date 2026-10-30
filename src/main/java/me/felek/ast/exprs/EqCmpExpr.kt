package me.felek.ast.exprs

import me.felek.ast.Expr
import me.felek.ast.exprs.util.ECType
import org.objectweb.asm.Type

/**
 * @className: EqCmpExpr
 * @author: Felek
 * @date: 28/10/2026 12:26
 */

class EqCmpExpr(line: Int, column: Int, var left: Expr, var right: Expr, var ec: ECType) : Expr(line, column) {
    lateinit var type: Type
}