package me.felek.fenix.runner

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import me.felek.bright.parser.FenixLexer
import me.felek.bright.parser.FenixParser
import me.felek.fenix.analyzer.Error
import me.felek.fenix.analyzer.SemanticAnalyzer
import me.felek.fenix.ast.ASTVisitor
import me.felek.fenix.compiler.Compiler
import me.felek.fenix.listener.ErrorStrategy
import me.felek.fenix.utils.OtherUtils.formatError
import org.antlr.v4.runtime.*
import java.nio.file.Files
import java.nio.file.Paths
import java.util.function.Consumer

/**
 * @className: Runner
 * @author: Felek
 * @date: 29/10/2026 13:46
 */

class Phx : CliktCommand(
    name = "Phoenix root command"
) {
    override fun run() = Unit
}

class Compile : CliktCommand(
    name =  "compile"
) {
    val inputFile: String by argument(name = "inputFile", help = "File to compile")
    val outputFile: String? by argument(name = "outputFile", help = ".class output file name").optional()

    override fun help(context: Context): String {
        return "Compile a .phx file to .class"
    }

    override fun run() {
        val errors: MutableList<String?> = ArrayList<String?>()

        val lexer = FenixLexer(CharStreams.fromString(Files.readString(Paths.get(inputFile))))
        lexer.removeErrorListeners()
        lexer.addErrorListener(object : BaseErrorListener() {
            override fun syntaxError(
                recognizer: Recognizer<*, *>?,
                offendingSymbol: Any?,
                line: Int,
                charPositionInLine: Int,
                msg: String,
                e: RecognitionException?
            ) {
                errors.add(formatError(0x000, line, charPositionInLine, msg))
            }
        })
        val tokens = CommonTokenStream(lexer)
        val parser = FenixParser(tokens)
        parser.removeErrorListeners()
        parser.addErrorListener(object : BaseErrorListener() {
            override fun syntaxError(
                recognizer: Recognizer<*, *>?,
                offendingSymbol: Any?,
                line: Int,
                charPositionInLine: Int,
                msg: String?,
                e: RecognitionException?
            ) {
                errors.add(msg)
            }
        })
        parser.setErrorHandler(ErrorStrategy())
        val parsedCode = parser.program()
        if (!errors.isEmpty()) {
            errors.forEach(Consumer { x: String? -> System.err.println(x) })
            return
        }

        val ast = ASTVisitor(inputFile.replace(".phx", ""))
        ast.visit(parsedCode)
        val sema = SemanticAnalyzer()
        sema.visit(ast.getResult())

        if (!sema.getErrors().isEmpty()) {
            sema.getErrors()
                .forEach(Consumer { e: Error? -> System.err.println("[ " + e!!.code + " ] " + e.message) })
            return
        }

        val compiler = Compiler()
        compiler.visit(sema.getResult())
        Files.write(Paths.get(outputFile ?: Paths.get(inputFile).fileName.toString().removeSuffix(".phx") + ".class"), compiler.bytecode())
    }
}

fun main(args: Array<String>) = Phx().subcommands(Compile()).main(args)