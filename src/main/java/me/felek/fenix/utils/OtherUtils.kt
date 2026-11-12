package me.felek.fenix.utils

import me.felek.ast.exprs.util.AssignmentType
import me.felek.ast.exprs.util.AssignmentType.ADD
import me.felek.ast.exprs.util.AssignmentType.DIV
import me.felek.ast.exprs.util.AssignmentType.EQ
import me.felek.ast.exprs.util.AssignmentType.MUL
import me.felek.ast.exprs.util.AssignmentType.REM
import me.felek.ast.exprs.util.AssignmentType.SUB

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

    @JvmStatic
    public fun formatOutError(code: Int, message: String, line: Int, column: Int, codeline: String, filepath: String): String {
        return """
            error[${code.toString(16)}]: $message
              --> $filepath:$line:$column
                |
               $line |      $codeline
                |
        """.trimIndent()
    }

    @JvmStatic
    fun getAssignment(op: String): AssignmentType {
        return when (op) {
            "-=" -> SUB
            "+=" -> ADD
            "*=" -> MUL
            "/=" -> DIV
            "%=" -> REM
            else -> EQ
        }
    }
}