package me.felek.fenix.scope

import me.felek.ast.decl.clazz.ClassMethod

/**
 * @className: ClassSymbol
 * @author: Felek
 * @date: 11/13/26 8:33 PM
 */

class ClassSymbol(val name: String, val modifier: Modifier, var classFields: Map<String, VariableSymbol>, var classMethods: Map<String, ClassMethod>)