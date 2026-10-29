package me.felek.fenix.utils

/**
 * @className: OtherUtils
 * @author: Felek
 * @date: 27/10/2026 15:58
 */

object OtherUtils {
    @JvmStatic
    public fun resolveImport(import: String, imports: Map<String, String>): String {
        return imports.getOrDefault(import, import)
    }

    @JvmStatic
    public fun formatError(code: Int, line: Int, column: Int, message: String): String {
        return "[$code] [$line;$column] $message"
    }
}