package me.felek.fenix.scope

import me.felek.ast.type.ValueType

/**
 * @className: VariableSymbol
 * @author: Felek
 * @date: 27/10/2026 18:06
 */

class VariableSymbol(var name: String, var type: ValueType, var isConst: Boolean = false) {
    public var id: Int = -1;
    public var isArgVar: Boolean = false;
}