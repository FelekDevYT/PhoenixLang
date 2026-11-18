package me.felek.ast.exprs

import me.felek.ast.Expr
import me.felek.fenix.scope.VariableSymbol
import java.lang.reflect.Field

/**
 * @className: SetField
 * @author: Felek
 * @date: 11/10/26 6:53 PM
 */

class SetField(line: Int, column: Int, var left: Expr, var id: String, var right: Expr) : Expr(line, column) {
    var clazz: Class<*>? = null
    var field: Field? = null
    var isStatic: Boolean = false
    var phoenixField: VariableSymbol? = null
    var ownerClass: String? = null
}