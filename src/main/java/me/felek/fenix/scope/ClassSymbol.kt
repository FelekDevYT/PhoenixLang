package me.felek.fenix.scope

/**
 * @className: ClassSymbol
 * @author: Felek
 * @date: 11/13/26 8:33 PM
 */

class ClassSymbol(val name: String, val modifier: Modifier, var classFields: Map<String, VariableSymbol>)