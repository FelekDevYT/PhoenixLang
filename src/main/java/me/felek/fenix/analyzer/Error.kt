package me.felek.fenix.analyzer

/**
 * @className: Error
 * @author: Felek
 * @date: 27/10/2026 19:08
 */

data class Error(var line: Int, var column: Int, var code: Int, var message: String)
