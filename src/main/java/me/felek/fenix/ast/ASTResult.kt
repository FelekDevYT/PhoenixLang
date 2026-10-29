package me.felek.fenix.ast

import me.felek.ast.decl.FunctionDeclaration

/**
 * @className: Result
 * @author: Felek
 * @date: 27/10/2026 15:04
 */

data class ASTResult(var functions: List<FunctionDeclaration>, var moduleName: String, var importMap: Map<String, String>)
