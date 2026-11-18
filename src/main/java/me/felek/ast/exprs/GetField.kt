package me.felek.ast.exprs

import me.felek.ast.Expr
import me.felek.fenix.scope.VariableSymbol
import java.lang.reflect.Field

/**
 * @className: GetField
 * @author: Felek
 * @date: 11/10/26 6:46 PM
 */

class GetField(line: Int, column: Int, var left: Expr, val name: String) : Expr(line, column) {
    var clazz: Class<*>? = null
    var field: Field? = null
    var isStatic: Boolean = false
    var phoenixField: VariableSymbol? = null
    var ownerClass: String? = null
}