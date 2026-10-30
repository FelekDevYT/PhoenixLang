package me.felek.fenix.scope

import me.felek.ast.decl.Arg
import me.felek.ast.type.ValueType

/**
 * @className: FunctionSymbol
 * @author: Felek
 * @date: 28/10/2026 14:04
 */

class FunctionSymbol(var name: String, var args: List<Arg>, var returnType: ValueType)