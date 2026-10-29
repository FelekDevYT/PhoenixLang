package me.felek.fenix.listener

import me.felek.bright.parser.FenixParser
import me.felek.fenix.utils.OtherUtils
import me.felek.fenix.utils.OtherUtils.formatError
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.parse.GrammarTreeVisitor
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.tree.TerminalNode

/**
 * @className: ErrorStrategy
 * @author: Felek
 * @date: 28/10/2026 17:00
 */
class ErrorStrategy : DefaultErrorStrategy() {
    private val errors: MutableList<String?> = mutableListOf()

    override fun reportError(recognizer: Parser?, e: RecognitionException) {
        val token = e.getOffendingToken()
        val line = token.getLine()
        val column = token.getCharPositionInLine()

        val code: Int = determinate(e, token, recognizer!!.ruleContext)
        val err: String = OtherUtils.formatError(code, line, column, format(code, token))

        errors.add(err)
        recognizer.notifyErrorListeners(token, err, e)
    }

    fun determinate(e: RecognitionException, token: Token?, ctx: ParserRuleContext?): Int {
        if (e is NoViableAltException) {
            return 0x040
        }

        return when (ctx) {
            is FenixParser.IfElseContext -> 0x043
            is FenixParser.VariableDeclarationContext -> {
                if (token!!.text == "=") {
                    return 0x052
                }
                return 0x050
            }
            is FenixParser.ArgContext -> 0x302
            is FenixParser.UsingDeclarationContext -> if (token!!.text == ";") 0x011 else 0x010
            is FenixParser.FunctionDeclarationContext -> {
                val last: ParseTree = ctx.children.get(ctx.children.size - 1)
                if (last is TerminalNode) {
                    when (last.text) {
                        "func" -> 0x020
                        "(" -> 0x021
                        "," -> 0x023
                        "->" -> 0x025
                        "=" -> 0x026
                        ")" -> 0x024
                        else -> 0x024
                    }
                } else {
                    0x024
                }
            }
            is FenixParser.BlockContext -> 0x031
            is FenixParser.InvokeLocalContext -> if (token!!.text == ",") 0x042 else 0x041
            is FenixParser.InvokeStaticContext -> if (token!!.text == ",") 0x042 else 0x041
            is FenixParser.ProgramContext -> when (token!!.text) {
                "}" -> 0x032
                "else" -> 0x044
                else -> 0x014
            }
            is FenixParser.ExprContext -> 0x040
            else -> if (isOp(token!!.text)) 0x040 else 0x999
        }
    }

    private fun isOp(text: String): Boolean {
        return text == "+" || text == "-" || text == "*" || text == "/" || text == "%" || text == "+" || text == "-" ||
                text == "==" || text == "!=" || text == ">=" || text == "<=" || text == "<" || text == ">"
    }

    private fun format(code: Int, token: Token): String {
        return when (code) {
            0x010 -> "String expected after using"
            0x011 -> "';' expected after using"
            0x014 -> "unexpected text outside function"
            0x020 -> "function name expected"
            0x021 -> "'(' or ')' expected in parameter list"
            0x023 -> "',' expected between parameters"
            0x024 -> "function body expected"
            0x025 -> "type expected after '->'"
            0x026 -> "expression expected after '='"
            0x031 -> "';' expected in block"
            0x032 -> "extra '}'"
            0x040 -> "missing operand"
            0x041 -> "unclosed or extra ')' in call"
            0x042 -> "',' expected between arguments"
            0x043 -> "if without condition or body"
            0x044 -> "else without if"
            0x050 -> "name expected after var/const"
            0x052 -> "expression expected after '='"
            0x302 -> "':' expected in parameter"
            else -> "Syntax error near " + token.text + "!"
        }
    }
}