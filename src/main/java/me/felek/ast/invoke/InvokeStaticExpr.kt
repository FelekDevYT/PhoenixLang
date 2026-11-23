package me.felek.ast.invoke

import me.felek.ast.Expr
import me.felek.ast.decl.FunctionDeclaration
import org.objectweb.asm.commons.Method

/**
 * @className: InvokeStatic
 * @author: Felek
 * @date: 27/10/2026 14:11
 */

class InvokeStaticExpr(line: Int, column: Int, var owner: String, var method: String, var args: List<Expr>) : Expr(line, column) {
    var methodImpl: java.lang.reflect.Method? = null
    var phoenixMethod: FunctionDeclaration? = null
}